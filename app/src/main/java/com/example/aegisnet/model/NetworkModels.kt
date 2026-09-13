package com.example.aegisnet.model
import com.google.gson.annotations.SerializedName

// Modello di risposta per le API del Mozilla HTTP Observatory
data class ObservatoryResponse(
    val id: Long?,
    @SerializedName("details_url")
    val detailsUrl: String?,
    val grade: String?,
    val score: Int?,
    @SerializedName("status_code")
    val statusCode: Int?,
    @SerializedName("tests_failed")
    val testsFailed: Int?,

    @SerializedName("tests_passed")

    val testsPassed: Int?,
    @SerializedName("tests_quantity")
    val testsQuantity: Int?,

    val error: String?
)