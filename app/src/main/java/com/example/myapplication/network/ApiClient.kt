package com.example.myapplication.network

import android.content.Context
import com.example.myapplication.auth.AuthManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    /**
     * BASE_URL options:
     *  - Production (VPS):      https://mrrawthereltech.com/  ← ACTIVE
     *  - Physical device (LAN): http://192.168.31.209:8000/
     *  - Android Emulator:      http://10.0.2.2:8000/
     */
    const val BASE_URL = "https://mrrawthereltech.com/"

    private var authManager: AuthManager? = null

    fun init(context: Context) {
        authManager = AuthManager.getInstance(context)
    }

    private val authInterceptor = Interceptor { chain ->
        val token = authManager?.getCachedAccessToken()
        val request = if (!token.isNullOrEmpty()) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        chain.proceed(request)
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
