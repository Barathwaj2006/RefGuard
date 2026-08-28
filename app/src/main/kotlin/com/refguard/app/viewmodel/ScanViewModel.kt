package com.refguard.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.refguard.app.api.*
import com.refguard.app.domain.*
import com.refguard.app.edge.LocalEdgeClassifier
import com.refguard.app.queue.OfflineScanQueue
import com.refguard.platform.models.ContentType
import com.refguard.platform.models.IngressError
import com.refguard.platform.models.IngressResult
import com.refguard.platform.models.ScanRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

sealed class ScanUiState {
    object Idle : ScanUiState()
    object Scanning : ScanUiState()
    object Queued : ScanUiState()
    data class Success(val result: ScanResult) : ScanUiState()
    data class Error(val message: String, val isMalformed: Boolean = false) : ScanUiState()
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
    private val isNetworkAvailable: () -> Boolean = { true }
) : ViewModel() {

    private val _scanState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val scanState: StateFlow<ScanUiState> = _scanState.asStateFlow()

    private val _reportState = MutableStateFlow<ReportUiState>(ReportUiState.Idle)
    val reportState: StateFlow<ReportUiState> = _reportState.asStateFlow()

    private val _offlineQueueSize = MutableStateFlow(offlineQueue.size())
    val offlineQueueSize: StateFlow<Int> = _offlineQueueSize.asStateFlow()

    fun resetScanState() {
        _scanState.value = ScanUiState.Idle
    }

    fun resetReportState() {
        _reportState.value = ReportUiState.Idle
    }

    fun saveInvestigationManually(result: ScanResult) {
        // No-op or saved to local storage
    }

    fun retry(request: ScanRequest) {
        submitScan(request)
    }

    fun handleIngressResult(result: IngressResult) {
        when (result) {
            is IngressResult.Success -> {
                submitScan(result.request)
            }
            is IngressResult.SuccessOffline -> {
                offlineQueue.enqueue(result.request)
                _offlineQueueSize.value = offlineQueue.size()
                _scanState.value = ScanUiState.Queued
            }
            is IngressResult.Failure -> {
                val err = result.error
                when (err) {
                    is IngressError.EmptyContent -> {
                        _scanState.value = ScanUiState.Error("No content provided to scan.", isMalformed = true)
                    }
                    is IngressError.PermissionDenied -> {
                        _scanState.value = ScanUiState.Error("Camera permission required for QR scanning.")
                    }
                    is IngressError.HardwareUnavailable -> {
                        _scanState.value = ScanUiState.Error("Camera hardware unavailable on device.")
                    }
                    is IngressError.InvalidPayload -> {
                        _scanState.value = ScanUiState.Error(err.message, isMalformed = true)
                    }
                    is IngressError.NoData -> {
                        _scanState.value = ScanUiState.Error(err.message)
                    }
                    is IngressError.Unknown -> {
                        _scanState.value = ScanUiState.Error(err.cause.message ?: "Unknown error")
                    }
                }
            }
        }
    }

    fun submitScan(request: ScanRequest) {
        _scanState.value = ScanUiState.Scanning

        if (!isNetworkAvailable()) {
            offlineQueue.enqueue(request)
            _offlineQueueSize.value = offlineQueue.size()
            val localResult = LocalEdgeClassifier.classify(request)
            _scanState.value = ScanUiState.Success(localResult)
            return
        }

        viewModelScope.launch {
            try {
                val dto = ScanRequestDto(
                    content_type = request.contentType.name,
                    content_value = request.contentValue,
                    source_context = request.sourceContext,
                    timestamp = request.timestamp
                )
                val response = apiService.scan(dto)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val riskSeverity = body.risk_assessment?.risk_severity ?: "UNKNOWN"
                    val riskLevel = when (riskSeverity.uppercase()) {
                        "LOW", "SAFE" -> RiskLevel.SAFE
                        "MEDIUM", "WARNING" -> RiskLevel.WARNING
                        "HIGH" -> RiskLevel.HIGH
                        "CRITICAL" -> RiskLevel.CRITICAL
                        else -> RiskLevel.UNKNOWN
                    }
                    val mismatchStatus = if (body.payment_intent_mismatch?.mismatch_status == "DETECTED") {
                        MismatchStatus.DETECTED
                    } else {
                        MismatchStatus.NOT_DETECTED
                    }

                    val protectionAction = when (body.protection_decision?.action?.uppercase()) {
                        "DISCOURAGE_PROCEED", "BLOCK" -> ProtectionAction.DISCOURAGE_PROCEED
                        "REQUIRE_CONFIRMATION" -> ProtectionAction.REQUIRE_CONFIRMATION
                        "WARN_CAUTION", "WARN" -> ProtectionAction.WARN_CAUTION
                        else -> ProtectionAction.ALLOW
                    }

                    val result = ScanResult(
                        scanId = body.scan_id,
                        timestamp = body.timestamp,
                        riskLevel = riskLevel,
                        riskScore = body.risk_assessment?.risk_score ?: 0,
                        riskConfidence = body.risk_assessment?.confidence ?: 0.9,
                        signals = body.risk_assessment?.signals ?: emptyList(),
                        humanExplanation = body.risk_assessment?.human_explanation ?: "",
                        recommendedAction = body.risk_assessment?.recommended_action ?: "",
                        protectionAction = protectionAction,
                        detectedSummary = body.protection_decision?.detected_summary ?: "",
                        whyItMatters = body.protection_decision?.why_it_matters ?: "",
                        userInstruction = body.protection_decision?.user_instruction ?: "",
                        mismatchStatus = mismatchStatus,
                        statedIntent = body.payment_intent_mismatch?.stated_intent,
                        actualPaymentAction = body.payment_intent_mismatch?.actual_payment_action ?: "NONE",
                        paymentDirection = body.payment_intent_mismatch?.payment_direction ?: "NONE",
                        mismatchAmount = body.payment_intent_mismatch?.amount,
                        recipientVpa = body.payment_intent_mismatch?.recipient_vpa,
                        scamChainNodes = body.scam_chain?.nodes ?: emptyList(),
                        scamChainEdges = body.scam_chain?.edges ?: emptyList(),
                        evidenceItems = body.evidence_pack?.items ?: emptyList(),
                        isLocalEdgeResult = false
                    )
                    _scanState.value = ScanUiState.Success(result)
                } else if (response.code() in 400..499) {
                    _scanState.value = ScanUiState.Error("Malformed scan request (code ${response.code()})", isMalformed = true)
                } else {
                    val localResult = LocalEdgeClassifier.classify(request)
                    _scanState.value = ScanUiState.Success(localResult)
                }
            } catch (e: IOException) {
                // Fallback to local classification when network fails
                val localResult = LocalEdgeClassifier.classify(request)
                _scanState.value = ScanUiState.Success(localResult)
            } catch (e: Exception) {
                _scanState.value = ScanUiState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }

    fun submitReport(indicator: String, category: String, description: String) {
        _reportState.value = ReportUiState.Submitting
        viewModelScope.launch {
            try {
                val reportDto = ScamReportDto(
                    indicator = indicator,
                    scam_type = category,
                    description = description
                )
                val response = apiService.report(reportDto)
                if (response.isSuccessful && response.body() != null) {
                    _reportState.value = ReportUiState.Success(response.body()!!.report_id)
                } else {
                    _reportState.value = ReportUiState.Error("Failed to submit report")
                }
            } catch (e: Exception) {
                _reportState.value = ReportUiState.Error(e.message ?: "Network error submitting report")
            }
        }
    }

    fun submitFeedback(scanId: String, recipientVpa: String? = null, isConfirmedFraud: Boolean = true, comments: String? = null) {
        viewModelScope.launch {
            try {
                val feedbackType = if (isConfirmedFraud) "CONFIRMED_FRAUD" else "FALSE_POSITIVE"
                val dto = FeedbackRequestDto(
                    scan_id = scanId,
                    feedback_type = feedbackType,
                    recipient_vpa = recipientVpa,
                    comments = comments
                )
                apiService.submitFeedback(dto)
            } catch (_: Exception) {}
        }
    }
}
