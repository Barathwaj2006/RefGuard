package com.refguard.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.refguard.app.api.RefGuardApiService
import com.refguard.app.api.ScamReportDto
import com.refguard.app.domain.ScanResult
import com.refguard.app.domain.toDomain
import com.refguard.app.domain.toDto
import com.refguard.app.queue.OfflineScanQueue
import com.refguard.platform.models.IngressResult
import com.refguard.platform.models.ScanRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.Instant
import java.util.UUID

/**
 * UI states for the scan screen.
 */
sealed class ScanUiState {
    object Idle : ScanUiState()
    object Loading : ScanUiState()
    data class Success(val result: ScanResult) : ScanUiState()
    object Queued : ScanUiState()   // Offline — queued for later
    data class Error(
        val message: String,
        val isNetwork: Boolean = false,
        val isMalformed: Boolean = false,
        val pendingRequest: ScanRequest? = null   // for retry
    ) : ScanUiState()
}

sealed class ReportUiState {
    object Idle : ReportUiState()
    object Submitting : ReportUiState()
    data class Success(val reportId: String) : ReportUiState()
    data class Error(val message: String) : ReportUiState()
}

class ScanViewModel(
    private val apiService: RefGuardApiService,
    private val offlineQueue: OfflineScanQueue,
    private val isNetworkAvailable: () -> Boolean
) : ViewModel() {

    private val _scanState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val scanState: StateFlow<ScanUiState> = _scanState.asStateFlow()

    private val _reportState = MutableStateFlow<ReportUiState>(ReportUiState.Idle)
    val reportState: StateFlow<ReportUiState> = _reportState.asStateFlow()

    private val _offlineQueueSize = MutableStateFlow(0)
    val offlineQueueSize: StateFlow<Int> = _offlineQueueSize.asStateFlow()

    /**
     * Process an ingress result from the platform layer.
     * Maps IngressResult → API call → ScanUiState.
     */
    fun handleIngressResult(ingressResult: IngressResult) {
        when (ingressResult) {
            is IngressResult.Success -> submitScan(ingressResult.request)
            is IngressResult.SuccessOffline -> {
                offlineQueue.enqueue(ingressResult.request)
                _offlineQueueSize.value = offlineQueue.size()
                _scanState.value = ScanUiState.Queued
            }
            is IngressResult.Failure -> {
                val msg = when (ingressResult.error) {
                    is com.refguard.platform.models.IngressError.EmptyContent ->
                        "No content to scan. Please enter or share something."
                    is com.refguard.platform.models.IngressError.PermissionDenied ->
                        "Camera permission required for QR scanning."
                    is com.refguard.platform.models.IngressError.HardwareUnavailable ->
                        "Camera not available on this device."
                    is com.refguard.platform.models.IngressError.UnsupportedContent ->
                        "Image is too large or unsupported. Try a different image."
                    is com.refguard.platform.models.IngressError.MalformedContent ->
                        "Content could not be read: ${(ingressResult.error as com.refguard.platform.models.IngressError.MalformedContent).reason}"
                }
                _scanState.value = ScanUiState.Error(msg, isMalformed = true)
            }
        }
    }

    /**
     * Submit a ScanRequest to the API.
     */
    fun submitScan(request: ScanRequest) {
        viewModelScope.launch {
            _scanState.value = ScanUiState.Loading
            try {
                if (!isNetworkAvailable()) {
                    offlineQueue.enqueue(request)
                    _offlineQueueSize.value = offlineQueue.size()
                    _scanState.value = ScanUiState.Queued
                    return@launch
                }

                val response = apiService.scan(request.toDto())

                when {
                    response.isSuccessful && response.body() != null -> {
                        val result = response.body()!!.toDomain()
                        _scanState.value = ScanUiState.Success(result)
                    }
                    response.code() == 400 -> {
                        _scanState.value = ScanUiState.Error(
                            message = "Invalid content (${response.code()}). Please check what you're scanning.",
                            isMalformed = true
                        )
                    }
                    else -> {
                        _scanState.value = ScanUiState.Error(
                            message = "Server error (${response.code()}). Please try again.",
                            isNetwork = false,
                            pendingRequest = request
                        )
                    }
                }
            } catch (e: IOException) {
                _scanState.value = ScanUiState.Error(
                    message = "Cannot reach RefGuard server. Check your connection.",
                    isNetwork = true,
                    pendingRequest = request
                )
            } catch (e: Exception) {
                _scanState.value = ScanUiState.Error(
                    message = "Unexpected error: ${e.message}",
                    pendingRequest = request
                )
            }
        }
    }

    /**
     * Retry the last failed scan if a pending request is available.
     */
    fun retry(pendingRequest: ScanRequest) {
        submitScan(pendingRequest)
    }

    /**
     * Flush the offline queue when connectivity is restored.
     */
    fun flushOfflineQueue() {
        viewModelScope.launch {
            if (!isNetworkAvailable()) return@launch
            val queued = offlineQueue.dequeueAll()
            _offlineQueueSize.value = 0
            for (request in queued) {
                try {
                    apiService.scan(request.toDto())
                } catch (e: Exception) {
                    offlineQueue.enqueue(request)
                }
            }
            _offlineQueueSize.value = offlineQueue.size()
        }
    }

    /**
     * Submit a community scam report for the current scan result.
     * Security: caller MUST NOT pass PINs, OTPs, CVVs, or bank passwords.
     */
    fun submitReport(
        reportedIndicator: String,
        category: String,
        description: String
    ) {
        viewModelScope.launch {
            _reportState.value = ReportUiState.Submitting
            try {
                val report = ScamReportDto(
                    report_id = "rep_${UUID.randomUUID().toString().replace("-", "").take(12)}",
                    reported_indicator = reportedIndicator,
                    report_category = category,
                    description = description.take(500),   // bounded
                    evidence_references = null,
                    submission_timestamp = Instant.now().toString(),
                    moderation_status = "PENDING",
                    confidence = 0.85,
                    provenance = "USER_SUBMISSION"
                )
                val response = apiService.report(report)
                if (response.isSuccessful && response.body() != null) {
                    _reportState.value = ReportUiState.Success(response.body()!!.report_id)
                } else {
                    _reportState.value = ReportUiState.Error("Failed to submit report (${response.code()}).")
                }
            } catch (e: Exception) {
                _reportState.value = ReportUiState.Error("Could not reach server: ${e.message}")
            }
        }
    }

    fun resetScanState() {
        _scanState.value = ScanUiState.Idle
    }

    fun resetReportState() {
        _reportState.value = ReportUiState.Idle
    }

    class Factory(
        private val apiService: RefGuardApiService,
        private val offlineQueue: OfflineScanQueue,
        private val isNetworkAvailable: () -> Boolean
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ScanViewModel::class.java)) {
                return ScanViewModel(apiService, offlineQueue, isNetworkAvailable) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
