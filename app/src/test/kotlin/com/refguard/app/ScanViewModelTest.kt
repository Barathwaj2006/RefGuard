package com.refguard.app

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.refguard.app.api.*
import com.refguard.app.queue.OfflineScanQueue
import com.refguard.app.viewmodel.ScanUiState
import com.refguard.app.viewmodel.ScanViewModel
import com.refguard.platform.models.ContentType
import com.refguard.platform.models.IngressError
import com.refguard.platform.models.IngressResult
import com.refguard.platform.models.ScanRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import retrofit2.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ScanViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeApiService: FakeRefGuardApiService
    private lateinit var offlineQueue: OfflineScanQueue
    private lateinit var viewModel: ScanViewModel

    private var networkAvailable = true

    class FakeRefGuardApiService : RefGuardApiService {
        var scanHandler: (suspend (ScanRequestDto) -> Response<ScanResponseDto>)? = null
        var scanCallCount = 0
        var lastScanRequest: ScanRequestDto? = null

        override suspend fun scan(request: ScanRequestDto): Response<ScanResponseDto> {
            scanCallCount++
            lastScanRequest = request
            val handler = scanHandler ?: throw IllegalStateException("No scanHandler configured")
            return handler(request)
        }

        override suspend fun report(report: ScamReportDto): Response<ReportResponseDto> {
            return Response.success(ReportResponseDto("rep_123", "RECEIVED"))
        }

        override suspend fun submitFeedback(feedback: FeedbackRequestDto): Response<FeedbackResponseDto> {
            return Response.success(FeedbackResponseDto("RECEIVED", "s_001", "CONFIRMED_FRAUD", "Thanks"))
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeApiService = FakeRefGuardApiService()
        offlineQueue = OfflineScanQueue(null)
        viewModel = ScanViewModel(fakeApiService, offlineQueue) { networkAvailable }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun makeScanResponse(severity: String = "LOW") = ScanResponseDto(
        scan_id = "s_001",
        timestamp = "2026-01-01T00:00:00Z",
        risk_assessment = RiskAssessmentDto(
            risk_score = 5, risk_severity = severity, confidence = 0.9,
            signals = listOf("ok"), evidence_references = null,
            human_explanation = "Fine", recommended_action = "Proceed"
        ),
        protection_decision = ProtectionDecisionDto(
            action = "ALLOW", detected_summary = "Clean",
            why_it_matters = "Safety", user_instruction = "Go ahead."
        ),
        payment_intent_mismatch = null,
        scam_chain = null,
        evidence_pack = null
    )

    private val validRequest = ScanRequest(ContentType.TEXT, "test", "ctx", "2026-01-01T00:00:00Z")

    @Test
    fun `initial state is Idle`() = runTest {
        assertEquals(ScanUiState.Idle, viewModel.scanState.first())
    }

    @Test
    fun `successful scan transitions to Success state`() = runTest {
        fakeApiService.scanHandler = { Response.success(makeScanResponse()) }
        viewModel.submitScan(validRequest)
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.scanState.first()
        assertTrue(state is ScanUiState.Success)
    }

    @Test
    fun `CRITICAL severity result is preserved in state`() = runTest {
        fakeApiService.scanHandler = { Response.success(makeScanResponse("CRITICAL")) }
        viewModel.submitScan(validRequest)
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.scanState.first() as ScanUiState.Success
        assertEquals(com.refguard.app.domain.RiskLevel.CRITICAL, state.result.riskLevel)
    }

    @Test
    fun `IOException transitions to network Error state`() = runTest {
        fakeApiService.scanHandler = { throw IOException("no network") }
        viewModel.submitScan(validRequest)
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.scanState.first()
        assertTrue(state is ScanUiState.Success)
        assertTrue((state as ScanUiState.Success).result.isLocalEdgeResult)
    }

    @Test
    fun `offline submission queues the request`() = runTest {
        networkAvailable = false
        viewModel.submitScan(validRequest)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, offlineQueue.size())
        val state = viewModel.scanState.first()
        assertTrue(state is ScanUiState.Success)
        assertTrue((state as ScanUiState.Success).result.isLocalEdgeResult)
    }

    @Test
    fun `handleIngressResult with Success dispatches submitScan`() = runTest {
        fakeApiService.scanHandler = { Response.success(makeScanResponse()) }
        viewModel.handleIngressResult(IngressResult.Success(validRequest))
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.scanState.first() is ScanUiState.Success)
    }

    @Test
    fun `handleIngressResult with SuccessOffline queues and transitions`() = runTest {
        viewModel.handleIngressResult(IngressResult.SuccessOffline(validRequest))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, offlineQueue.size())
        assertEquals(ScanUiState.Queued, viewModel.scanState.first())
    }

    @Test
    fun `handleIngressResult with EmptyContent failure shows error`() = runTest {
        viewModel.handleIngressResult(IngressResult.Failure(IngressError.EmptyContent))
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.scanState.first()
        assertTrue(state is ScanUiState.Error)
        assertTrue((state as ScanUiState.Error).isMalformed)
    }

    @Test
    fun `handleIngressResult with PermissionDenied failure shows error`() = runTest {
        viewModel.handleIngressResult(IngressResult.Failure(IngressError.PermissionDenied))
        val state = viewModel.scanState.first()
        assertTrue(state is ScanUiState.Error)
        assertTrue((state as ScanUiState.Error).message.contains("Camera"))
    }

    @Test
    fun `400 error transitions to malformed Error state`() = runTest {
        fakeApiService.scanHandler = {
            Response.error(400, "bad request".toResponseBody(null))
        }
        viewModel.submitScan(validRequest)
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.scanState.first() as ScanUiState.Error
        assertTrue(state.isMalformed)
    }

    @Test
    fun `resetScanState returns to Idle`() = runTest {
        fakeApiService.scanHandler = { Response.success(makeScanResponse()) }
        viewModel.submitScan(validRequest)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.scanState.first() is ScanUiState.Success)
        viewModel.resetScanState()
        assertEquals(ScanUiState.Idle, viewModel.scanState.first())
    }

    @Test
    fun `retry resubmits the pending request`() = runTest {
        fakeApiService.scanHandler = { Response.success(makeScanResponse()) }
        viewModel.retry(validRequest)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, fakeApiService.scanCallCount)
        assertTrue(viewModel.scanState.first() is ScanUiState.Success)
    }

    @Test
    fun `offline queue size is exposed via StateFlow`() = runTest {
        networkAvailable = false
        viewModel.submitScan(validRequest)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.offlineQueueSize.first())
    }
}
