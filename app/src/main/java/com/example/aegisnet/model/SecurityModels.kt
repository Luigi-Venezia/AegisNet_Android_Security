package com.example.aegisnet.model

import kotlinx.serialization.Serializable

/* Classe enumerativa, le sue costanti rappresentano
   i livelli di sicurezza */
enum class SecurityLevel {
    GOOD,
    STRONG,
    MODERATE,
    WARNING,
    WEAK,
    CRITICAL
}

//Le costanti rappresentano l'esito di un singolo controllo di sicurezza
enum class SecurityCheckStatus {
    PASSED,
    WARNING,
    FAILED
}

/* Data class, usata principalmente per contenere i dati della Dashboard,
   offrendo di default metodi come: copy(), equals(), hashCode() e toString()
  Rappresenta un controllo eseguito da uno strumento di sicurezza */

@Serializable
data class SecurityCheck(
    val name: String,
    val status: SecurityCheckStatus,
    val description: String
): java.io.Serializable

/* Data class usata per trasferire il risultato della valutazione alla UI */

@Serializable
data class SecurityReport(
    val score: Int,
    val status: SecurityLevel,
    val passedChecks: Int,
    val warnings: Int,
    val checks: List<SecurityCheck> = emptyList()
): java.io.Serializable