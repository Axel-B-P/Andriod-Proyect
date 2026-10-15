package com.tuapp.data.repository

import com.tuapp.data.model.Business
import com.tuapp.data.network.RetrofitClient

// ─────────────────────────────────────────────
// Sealed class para manejar los estados de red
// ─────────────────────────────────────────────
sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error(val message: String) : NetworkResult<Nothing>()
    object Loading : NetworkResult<Nothing>()
}

// ─────────────────────────────────────────────
// Repositorio
// ─────────────────────────────────────────────
class BusinessRepository {

    private val apiService = RetrofitClient.businessApiService

    suspend fun searchBusinesses(query: String): NetworkResult<List<Business>> {
        return try {
            val response = apiService.searchBusinesses(query.trim())

            if (response.isSuccessful) {
                val businesses = response.body()?.results ?: emptyList()
                NetworkResult.Success(businesses)
            } else {
                NetworkResult.Error(
                    "Error ${response.code()}: ${response.message()}"
                )
            }
        } catch (e: Exception) {
            NetworkResult.Error(
                e.localizedMessage ?: "Error de conexión desconocido"
            )
        }
    }
}
