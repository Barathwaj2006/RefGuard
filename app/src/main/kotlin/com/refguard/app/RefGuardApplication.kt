package com.refguard.app

import android.app.Application
import com.refguard.app.api.RefGuardApiService
import com.refguard.app.queue.OfflineScanQueue
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class RefGuardApplication : Application() {

    lateinit var apiService: RefGuardApiService
        private set

    lateinit var offlineQueue: OfflineScanQueue
        private set

    override fun onCreate() {
        super.onCreate()
        offlineQueue = OfflineScanQueue(this)

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.SCAN_API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(RefGuardApiService::class.java)
    }
}
