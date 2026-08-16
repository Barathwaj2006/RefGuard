package com.refguard.app.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit API interface mapping to the contracts/api.yaml endpoints.
 */
interface RefGuardApiService {

    /** POST /api/v1/scan */
    @POST("api/v1/scan")
    suspend fun scan(@Body request: ScanRequestDto): Response<ScanResponseDto>

    /** POST /api/v1/report */
    @POST("api/v1/report")
    suspend fun report(@Body report: ScamReportDto): Response<ReportResponseDto>

    /** POST /api/v1/feedback */
    @POST("api/v1/feedback")
    suspend fun submitFeedback(@Body feedback: FeedbackRequestDto): Response<FeedbackResponseDto>
}
