package com.tuapp.buscaya.data.model
import com.google.gson.annotations.SerializedName


data class BusinessSearchResponse(
    @SerializedName("results") val results: List<Business>? = emptyList(),
    @SerializedName("total")   val total: Int? = 0
)


data class Business(
    @SerializedName("nombre")    val name: String?    = null,
    @SerializedName("ubicacion") val address: String? = null,
    @SerializedName("telefono")  val phone: String?   = null,
    @SerializedName("imagen")    val image: String?   = null
)
