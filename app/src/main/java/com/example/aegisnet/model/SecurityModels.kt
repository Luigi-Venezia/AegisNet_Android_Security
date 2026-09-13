package com.example.aegisnet.model

import kotlinx.serialization.Serializable

//Livelli di sicurezza generali dell'applicativo
enum class SecurityLevel {GOOD,STRONG,
    MODERATE,WARNING,WEAK,CRITICAL
}

//Esito di un singolo controllo
enum class SecurityCheckStatus {
    PASSED,
    WARNING,
    FAILED
}
//Dati del singolo controllo eseguito
@Serializable
data class SecurityCheck(
    val name: String,
    val status: SecurityCheckStatus,
    val description: String
): java.io.Serializable

//Dati dei report inviati alla UI
@Serializable
data class SecurityReport(
    val score: Int,
    val status: SecurityLevel,
    val passedChecks: Int,
    val warnings: Int,
    val checks: List<SecurityCheck> = emptyList()
): java.io.Serializable