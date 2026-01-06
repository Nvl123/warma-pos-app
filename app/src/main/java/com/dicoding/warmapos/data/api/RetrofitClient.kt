package com.dicoding.warmapos.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    const val DEFAULT_BASE_URL = "https://api.ocr.space/"
    
    private var currentBaseUrl: String = DEFAULT_BASE_URL
    private var ocrService: OcrApiService? = null

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun createRetrofit(baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    fun getOcrApiService(baseUrl: String = DEFAULT_BASE_URL): OcrApiService {
        // Recreate service if URL changed
        if (baseUrl != currentBaseUrl || ocrService == null) {
            currentBaseUrl = baseUrl
            ocrService = createRetrofit(baseUrl).create(OcrApiService::class.java)
        }
        return ocrService!!
    }
    
    // Legacy accessor for backward compatibility
    val ocrApiService: OcrApiService
        get() = getOcrApiService()
}
