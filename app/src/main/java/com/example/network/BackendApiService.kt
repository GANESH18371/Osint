package com.example.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class HealthResponse(
    @Json(name = "status") val status: String,
    @Json(name = "service") val service: String,
    @Json(name = "version") val version: String,
    @Json(name = "ethical_mode") val ethicalMode: Boolean = true
)

@JsonClass(generateAdapter = true)
data class OsintApiRequest(
    @Json(name = "query") val query: String,
    @Json(name = "target_type") val targetType: String,
    @Json(name = "rssi") val rssi: Int? = null,
    @Json(name = "vendor") val vendor: String? = null
)

@JsonClass(generateAdapter = true)
data class ProfileMatchDto(
    @Json(name = "platform") val platform: String,
    @Json(name = "handle") val handle: String,
    @Json(name = "url") val url: String,
    @Json(name = "confidence") val confidence: Float,
    @Json(name = "status") val status: String,
    @Json(name = "details") val details: String
)

@JsonClass(generateAdapter = true)
data class OsintApiResponse(
    @Json(name = "query") val query: String,
    @Json(name = "target_type") val targetType: String,
    @Json(name = "matches") val matches: List<ProfileMatchDto>,
    @Json(name = "risk_assessment") val riskAssessment: String,
    @Json(name = "ethical_notice") val ethicalNotice: String,
    @Json(name = "timestamp") val timestamp: Long
)

@JsonClass(generateAdapter = true)
data class ImageMatchApiResponse(
    @Json(name = "image_hash") val imageHash: String,
    @Json(name = "matched_entities") val matchedEntities: List<String>,
    @Json(name = "public_profile_matches") val publicProfileMatches: List<ProfileMatchDto>,
    @Json(name = "similarity_score") val similarityScore: Float,
    @Json(name = "status_message") val statusMessage: String,
    @Json(name = "ethical_compliance_verified") val ethicalComplianceVerified: Boolean,
    @Json(name = "timestamp") val timestamp: Long
)

interface BackendApiService {
    @GET("health")
    suspend fun checkHealth(): HealthResponse

    @POST("api/v1/osint/query")
    suspend fun queryOsint(@Body request: OsintApiRequest): OsintApiResponse

    @Multipart
    @POST("api/v1/osint/image-match")
    suspend fun matchImage(
        @Part file: MultipartBody.Part,
        @Part("metadata") metadata: RequestBody? = null
    ): ImageMatchApiResponse
}

object NetworkClientProvider {
    private var currentBaseUrl: String = "http://10.0.2.2:8000/"
    private var apiService: BackendApiService? = null

    fun getService(baseUrl: String = currentBaseUrl): BackendApiService {
        val formattedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        if (apiService == null || formattedUrl != currentBaseUrl) {
            currentBaseUrl = formattedUrl
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build()

            apiService = Retrofit.Builder()
                .baseUrl(currentBaseUrl)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(BackendApiService::class.java)
        }
        return apiService!!
    }
}
