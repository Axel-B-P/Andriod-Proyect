package com.tuapp.buscaya.data.network
import com.tuapp.buscaya.data.model.BusinessSearchResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit


interface BusinessApiService {

    @GET("hybrid-search-android")
    suspend fun searchBusinesses(
        @Query("q") query: String
    ): Response<BusinessSearchResponse>
}


object RetrofitClient {

    private const val BASE_URL = "https://www.buscaya.com.ar/api/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
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
