package com.dicoding.warmapos.data.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * OCR.space API response models
 */
data class OcrResponse(
    val ParsedResults: List<ParsedResult>?,
    val IsErroredOnProcessing: Boolean,
    val ErrorMessage: List<String>?,
    val ErrorDetails: String?
)

data class ParsedResult(
    val ParsedText: String,
    val ErrorMessage: String?,
    val ErrorDetails: String?
)

/**
 * OCR.space API service interface
 */
interface OcrApiService {
    @Multipart
    @POST("parse/image")
    suspend fun parseImage(
        @Part file: MultipartBody.Part,
        @Part("apikey") apiKey: RequestBody,
        @Part("language") language: RequestBody,
        @Part("OCREngine") engine: RequestBody,
        @Part("detectOrientation") detectOrientation: RequestBody,
        @Part("scale") scale: RequestBody,
        @Part("isOverlayRequired") isOverlayRequired: RequestBody
    ): OcrResponse
}
