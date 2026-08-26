package com.refguard.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.refguard.app.api.RefGuardApiService
import com.refguard.app.api.ScamReportDto
import com.refguard.app.domain.ScanResult
import com.refguard.app.domain.toDomain
import com.refguard.app.domain.toDto
import com.refguard.app.edge.LocalEdgeClassifier
import com.refguard.app.history.HistoryItem
import com.refguard.app.history.InvestigationHistoryManager
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
    object Queued : ScanUiState()   // Offline fallback
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

enum class SyncStatus {
    IDLE, SYNCING, SYNCED, FAILED
}

class ScanViewModel(
    private val apiService: RefGuardApiService,
    private val offlineQueue: com.refguard.app.queue.OfflineQueue,
    private val historyManager: InvestigationHistoryManager? = null,
    private val isNetworkAvailable: () -> Boolean = { true }
) : ViewModel() {

    constructor(
        apiService: RefGuardApiService,
        offlineQueue: com.refguard.app.queue.OfflineQueue,
        isNetworkAvailable: () -> Boolean
    ) : this(apiService, offlineQueue, null, isNetworkAvailable)

    private val _scanState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val scanState: StateFlow<ScanUiState> = _scanState.asStateFlow()

    private val _reportState = MutableStateFlow<ReportUiState>(ReportUiState.Idle)
    val reportState: StateFlow<ReportUiState> = _reportState.asStateFlow()

    private val _offlineQueueSize = MutableStateFlow(offlineQueue.size())
    val offlineQueueSize: StateFlow<Int> = _offlineQueueSize.asStateFlow()

    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _history = MutableStateFlow<List<HistoryItem>>(emptyList())
    val history: StateFlow<List<HistoryItem>> = _history.asStateFlow()

    init {
        refreshHistory()
    }

    fun refreshHistory() {
        historyManager?.let {
            _history.value = it.getHistory()
        }
    }

    /**
     * Process an ingress result from the platform layer.
     * Maps IngressResult → API call or Offline Queue → ScanUiState.
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
                        "Content could not be read: "
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
                    val localResult = scanLocally(request)
                    _scanState.value = ScanUiState.Success(localResult)
                    return@launch
                }

                val response = apiService.scan(request.toDto())

                when {
                    response.isSuccessful && response.body() != null -> {
                        val result = response.body()!!.toDomain()
                        historyManager?.saveInvestigation(result)
                        refreshHistory()
                        _scanState.value = ScanUiState.Success(result)
                    }
                    response.code() == 400 -> {
                        _scanState.value = ScanUiState.Error(
                            message = "Invalid content (). Please check what you're scanning.",
                            isMalformed = true
                        )
                    }
                    else -> {
                        val localResult = scanLocally(request)
                        _scanState.value = ScanUiState.Success(localResult)
                    }
                }
            } catch (e: IOException) {
                val localResult = scanLocally(request)
                _scanState.value = ScanUiState.Success(localResult)
            } catch (e: Exception) {
                val localResult = scanLocally(request)
                _scanState.value = ScanUiState.Success(localResult)
            }
        }
    }

    fun scanLocally(request: ScanRequest): ScanResult {
        val result = LocalEdgeClassifier.classify(request)
        historyManager?.saveInvestigation(result)
        refreshHistory()
        return result
    }

    fun openHistoryInvestigation(scanId: String) {
        historyManager?.getResult(scanId)?.let {
            _scanState.value = ScanUiState.Success(it)
        }
    }

    fun saveInvestigationManually(result: ScanResult) {
        historyManager?.saveInvestigation(result)
        refreshHistory()
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
            if (!isNetworkAvailable() || offlineQueue.size() == 0) return@launch
            _syncStatus.value = SyncStatus.SYNCING
            val queued = offlineQueue.dequeueAll()
            _offlineQueueSize.value = 0
            var allSucceeded = true
            for (request in queued) {
                try {
                    val res = apiService.scan(request.toDto())
                    if (res.isSuccessful && res.body() != null) {
                        historyManager?.saveInvestigation(res.body()!!.toDomain())
                    } else {
                        offlineQueue.enqueue(request)
                        allSucceeded = false
                    }
                } catch (e: Exception) {
                    offlineQueue.enqueue(request)
                    allSucceeded = false
                }
            }
            _offlineQueueSize.value = offlineQueue.size()
            _syncStatus.value = if (allSucceeded) SyncStatus.SYNCED else SyncStatus.FAILED
            refreshHistory()
        }
    }

    /**
     * Submit user feedback on verdict accuracy (Confirmed Scam vs False Alarm).
     */
    fun submitFeedback(
        scanId: String,
        indicator: String?,
        isConfirmedFraud: Boolean,
        userNotes: String? = null
    ) {
        viewModelScope.launch {
            try {
                val dto = com.refguard.app.api.FeedbackRequestDto(
                    scan_id = scanId,
                    indicator = indicator,
                    verdict = if (isConfirmedFraud) "CONFIRMED_FRAUD" else "FALSE_ALARM",
                    user_notes = userNotes
                )
                apiService.submitFeedback(dto)
            } catch (e: Exception) {
                // Non-critical background telemetry
            }
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
                    report_id = "rep_",
                    reported_indicator = reportedIndicator,
                    report_category = category,
                    description = description.take(500),
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
                    _reportState.value = ReportUiState.Error("Failed to submit report ().")
                }
            } catch (e: Exception) {
                _reportState.value = ReportUiState.Error("Could not reach server: ")
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
        private val offlineQueue: com.refguard.app.queue.OfflineQueue,
        private val isNetworkAvailable: () -> Boolean,
        private val historyManager: InvestigationHistoryManager? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ScanViewModel::class.java)) {
                return ScanViewModel(apiService, offlineQueue, historyManager, isNetworkAvailable) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
