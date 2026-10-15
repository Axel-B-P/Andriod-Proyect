package com.tuapp.data.model

import com.google.gson.annotations.SerializedName

// --- Respuesta raíz de la API ---
data class BusinessSearchResponse(
    @SerializedName("results") val results: List<Business>? = emptyList(),
    @SerializedName("total")   val total: Int? = 0
)

// --- Negocio individual ---
data class Business(
    @SerializedName("id")       val id: String?      = null,
    @SerializedName("name")     val name: String?    = null,
    @SerializedName("address")  val address: String? = null,
    @SerializedName("phone")    val phone: String?   = null,
    @SerializedName("image")    val image: String?   = null
)
