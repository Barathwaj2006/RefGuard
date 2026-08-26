package com.refguard.app

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.refguard.app.api.*
import com.refguard.app.queue.OfflineQueue
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
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ScanViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeApiService: FakeRefGuardApiService
    private lateinit var fakeOfflineQueue: FakeOfflineQueue
    private lateinit var viewModel: ScanViewModel

    private var networkAvailable = true

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeApiService = FakeRefGuardApiService()
        fakeOfflineQueue = FakeOfflineQueue()
        viewModel = ScanViewModel(fakeApiService, fakeOfflineQueue) { networkAvailable }
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
        fakeApiService.scanResponse = Response.success(makeScanResponse())
        viewModel.submitScan(validRequest)
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.scanState.first()
        assertTrue(state is ScanUiState.Success)
    }

    @Test
    fun `CRITICAL severity result is preserved in state`() = runTest {
        fakeApiService.scanResponse = Response.success(makeScanResponse("CRITICAL"))
        viewModel.submitScan(validRequest)
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.scanState.first() as ScanUiState.Success
        assertEquals(com.refguard.app.domain.RiskLevel.CRITICAL, state.result.riskLevel)
    }

    @Test
    fun `IOException transitions to local edge classifier Success state`() = runTest {
        fakeApiService.shouldThrowIoException = true
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
        assertEquals(1, fakeOfflineQueue.size())
        val state = viewModel.scanState.first()
        assertTrue(state is ScanUiState.Success)
        assertTrue((state as ScanUiState.Success).result.isLocalEdgeResult)
    }

    @Test
    fun `handleIngressResult with Success dispatches submitScan`() = runTest {
        fakeApiService.scanResponse = Response.success(makeScanResponse())
        viewModel.handleIngressResult(IngressResult.Success(validRequest))
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.scanState.first() is ScanUiState.Success)
    }

    @Test
    fun `handleIngressResult with SuccessOffline queues and transitions`() = runTest {
        fakeOfflineQueue.enqueue(validRequest)
        viewModel.handleIngressResult(IngressResult.SuccessOffline(validRequest))
        testDispatcher.scheduler.advanceUntilIdle()
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
        fakeApiService.scanResponse = Response.error(400, "bad request".toResponseBody(null))
        viewModel.submitScan(validRequest)
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.scanState.first() as ScanUiState.Error
        assertTrue(state.isMalformed)
    }

    @Test
    fun `resetScanState returns to Idle`() = runTest {
        fakeApiService.scanResponse = Response.success(makeScanResponse())
        viewModel.submitScan(validRequest)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.scanState.first() is ScanUiState.Success)
        viewModel.resetScanState()
        assertEquals(ScanUiState.Idle, viewModel.scanState.first())
    }

    @Test
    fun `retry resubmits the pending request`() = runTest {
        fakeApiService.scanResponse = Response.success(makeScanResponse())
        viewModel.retry(validRequest)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, fakeApiService.scanCallCount)
        assertTrue(viewModel.scanState.first() is ScanUiState.Success)
    }

    @Test
    fun `offline queue size is exposed via StateFlow`() = runTest {
        networkAvailable = false
        fakeOfflineQueue.enqueue(validRequest)
        fakeOfflineQueue.enqueue(validRequest)
        viewModel.submitScan(validRequest)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(3, viewModel.offlineQueueSize.first())
    }
}

// ── Test Doubles ─────────────────────────────────

class FakeOfflineQueue : OfflineQueue {
    private val queue = mutableListOf<ScanRequest>()

    override fun enqueue(request: ScanRequest) {
        queue.add(request)
    }

    override fun dequeueAll(): List<ScanRequest> {
        val copy = ArrayList(queue)
        queue.clear()
        return copy
    }

    override fun size(): Int = queue.size

    override fun clear() {
        queue.clear()
    }
}

class FakeRefGuardApiService : RefGuardApiService {
    var scanResponse: Response<ScanResponseDto>? = null
    var shouldThrowIoException = false
    var scanCallCount = 0

    override suspend fun scan(request: ScanRequestDto): Response<ScanResponseDto> {
        scanCallCount++
        if (shouldThrowIoException) {
            throw IOException("Simulated network failure")
        }
        return scanResponse ?: throw IllegalStateException("scanResponse not set")
    }

    override suspend fun report(report: ScamReportDto): Response<ReportResponseDto> {
        return Response.success(ReportResponseDto("rep_123", "PENDING"))
    }

    override suspend fun submitFeedback(feedback: FeedbackRequestDto): Response<FeedbackResponseDto> {
        return Response.success(FeedbackResponseDto("SUCCESS", feedback.scan_id, feedback.verdict, "Recorded"))
    }
}
