package com.tuapp.data.network

import com.tuapp.data.model.BusinessSearchResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// ─────────────────────────────────────────────
// 1. Interfaz del servicio
// ─────────────────────────────────────────────
interface BusinessApiService {

    /**
     * GET https://www.buscaya.com.ar/api/hybrid-search-android?q={query}
     */
    @GET("hybrid-search-android")
    suspend fun searchBusinesses(
        @Query("q") query: String
    ): Response<BusinessSearchResponse>
}

// ─────────────────────────────────────────────
// 2. Cliente Retrofit (Singleton)
// ─────────────────────────────────────────────
object RetrofitClient {

    private const val BASE_URL = "https://www.buscaya.com.ar/api/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY   // Cambiá a NONE en producción
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val businessApiService: BusinessApiService by lazy {
        retrofit.create(BusinessApiService::class.java)
    }
}
