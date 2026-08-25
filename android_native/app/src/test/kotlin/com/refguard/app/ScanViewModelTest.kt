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
import org.mockito.kotlin.*
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ScanViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockApiService: RefGuardApiService
    private lateinit var mockOfflineQueue: OfflineScanQueue
    private lateinit var viewModel: ScanViewModel

    private var networkAvailable = true

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockApiService = mock()
        mockOfflineQueue = mock()
        whenever(mockOfflineQueue.size()).thenReturn(0)
        viewModel = ScanViewModel(mockApiService, mockOfflineQueue) { networkAvailable }
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
        whenever(mockApiService.scan(any())).thenReturn(Response.success(makeScanResponse()))
        viewModel.submitScan(validRequest)
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.scanState.first()
        assertTrue(state is ScanUiState.Success)
    }

    @Test
    fun `CRITICAL severity result is preserved in state`() = runTest {
        whenever(mockApiService.scan(any())).thenReturn(Response.success(makeScanResponse("CRITICAL")))
        viewModel.submitScan(validRequest)
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.scanState.first() as ScanUiState.Success
        assertEquals(com.refguard.app.domain.RiskLevel.CRITICAL, state.result.riskLevel)
    }

    @Test
    fun `IOException transitions to network Error state`() = runTest {
        whenever(mockApiService.scan(any())).thenAnswer { throw IOException("no network") }
        viewModel.submitScan(validRequest)
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.scanState.first()
        assertTrue(state is ScanUiState.Error)
        assertTrue((state as ScanUiState.Error).isNetwork)
    }

    @Test
    fun `offline submission queues the request`() = runTest {
        networkAvailable = false
        whenever(mockOfflineQueue.size()).thenReturn(1)
        viewModel.submitScan(validRequest)
        testDispatcher.scheduler.advanceUntilIdle()
        verify(mockOfflineQueue).enqueue(validRequest)
        assertEquals(ScanUiState.Queued, viewModel.scanState.first())
    }

    @Test
    fun `handleIngressResult with Success dispatches submitScan`() = runTest {
        whenever(mockApiService.scan(any())).thenReturn(Response.success(makeScanResponse()))
        viewModel.handleIngressResult(IngressResult.Success(validRequest))
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.scanState.first() is ScanUiState.Success)
    }

    @Test
    fun `handleIngressResult with SuccessOffline queues and transitions`() = runTest {
        whenever(mockOfflineQueue.size()).thenReturn(1)
        viewModel.handleIngressResult(IngressResult.SuccessOffline(validRequest))
        testDispatcher.scheduler.advanceUntilIdle()
        verify(mockOfflineQueue).enqueue(validRequest)
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
        whenever(mockApiService.scan(any())).thenReturn(
            Response.error(400, okhttp3.ResponseBody.create(null, "bad request"))
        )
        viewModel.submitScan(validRequest)
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.scanState.first() as ScanUiState.Error
        assertTrue(state.isMalformed)
    }

    @Test
    fun `resetScanState returns to Idle`() = runTest {
        whenever(mockApiService.scan(any())).thenReturn(Response.success(makeScanResponse()))
        viewModel.submitScan(validRequest)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.scanState.first() is ScanUiState.Success)
        viewModel.resetScanState()
        assertEquals(ScanUiState.Idle, viewModel.scanState.first())
    }

    @Test
    fun `retry resubmits the pending request`() = runTest {
        whenever(mockApiService.scan(any())).thenReturn(Response.success(makeScanResponse()))
        viewModel.retry(validRequest)
        testDispatcher.scheduler.advanceUntilIdle()
        verify(mockApiService, times(1)).scan(any())
        assertTrue(viewModel.scanState.first() is ScanUiState.Success)
    }

    @Test
    fun `offline queue size is exposed via StateFlow`() = runTest {
        networkAvailable = false
        whenever(mockOfflineQueue.size()).thenReturn(3)
        viewModel.submitScan(validRequest)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(3, viewModel.offlineQueueSize.first())
    }
}

