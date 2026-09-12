package com.example.aegisnet.model

import com.google.gson.annotations.SerializedName

/* Risposta minima utilizzata da AegisNet
  per il Mozilla HTTP Observatory.
  I campi ricevuti dalla rete sono nullable perché
  una risposta esterna è sempre considerata non trusted
  e potrebbe non avere la struttura attesa */

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