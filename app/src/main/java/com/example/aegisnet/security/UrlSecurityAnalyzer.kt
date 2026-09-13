package com.example.aegisnet.security

import com.example.aegisnet.model.SecurityCheck
import com.example.aegisnet.model.SecurityCheckStatus
import com.example.aegisnet.model.SecurityLevel
import com.example.aegisnet.model.SecurityReport
import java.net.URI

/* Ho fatto un analisi sull URL in locale, senza connessioni. */
class UrlSecurityAnalyzer {
    companion object {
        private const val HTTP_SCHEME = "http"
        private const val HTTPS_SCHEME = "https"
    }

    fun analyze(input: String): SecurityReport {
        val value = input.trim()
        if (value.isBlank()) {
            return invalidReport("Inserisci un URL")
        }
        val uri = try {
            URI(value)
        } catch (_: Exception) {
            return invalidReport("L'URL non è valido")
        }
        val scheme = uri.scheme?.lowercase()
        val hostname = uri.host

        if (scheme != HTTP_SCHEME && scheme != HTTPS_SCHEME) {
            return invalidReport("Sono supportati soltanto URL Http e Https");
        }
        if (hostname == null || !isValidHostname(hostname)) {
            return invalidReport("L'url non contiene un host name valido");
        }

        val normalizedScheme = scheme ?: return invalidReport("Schema non valido.")
        val normalizedHostname = hostname ?: return invalidReport("Hostname non valido.")

        val isHttps = normalizedScheme == HTTPS_SCHEME
        val isHttp = normalizedScheme == HTTP_SCHEME

        val isIpAddress = hostnameIsIp(normalizedHostname)
        val hasNonStandardPort = hasNonStandardPort(uri, normalizedScheme)
        val hasSuspiciousCharacteristics = hasSuspiciousCharacteristics(uri, normalizedHostname)

        val checks = listOf(
            SecurityCheck(
                name = "Valid URL",
                status = SecurityCheckStatus.PASSED,
                description =
                    "L'URL utilizza uno schema supportato e contiene un hostname valido."
            ),

            SecurityCheck(
                name = "HTTPS",
                status =
                    if (isHttps)
                        SecurityCheckStatus.PASSED
                    else
                        SecurityCheckStatus.WARNING,
                description =
                    if (isHttps) {
                        "HTTPS cifra la connessione, però non è detto che sia affidabile."
                    } else {
                        "HTTPS cifra la connessione, ma non garantisce che il sito sia affidabile."
                    }
            ),

            SecurityCheck(
                name = "HTTP",
                status =
                    if (isHttp)
                        SecurityCheckStatus.WARNING
                    else
                        SecurityCheckStatus.PASSED,
                description =
                    if (isHttp) {
                        "HTTP non cifra. Elemento a rischio"
                    } else {
                        "L'URL non HTTP in maniera chiara"
                    }
            ),

            SecurityCheck(
                name = "Hostname",
                status = SecurityCheckStatus.PASSED,
                description = "Hostname: $hostname"
            ),

            SecurityCheck(
                name = "IP Address",
                status =
                    if (isIpAddress)
                        SecurityCheckStatus.WARNING
                    else
                        SecurityCheckStatus.PASSED,
                description =
                    if (isIpAddress) {
                        "L'hostname è un indirizzo IP anziché un normale nome di dominio."
                    } else {
                        "L'hostname è rappresentato da un nome di dominio"
                    }
            ),

            SecurityCheck(
                name = "Porta Non standard",
                status =
                    if (hasNonStandardPort)
                        SecurityCheckStatus.WARNING
                    else
                        SecurityCheckStatus.PASSED,
                description =
                    if (hasNonStandardPort) {
                        "L'URL usa una porta non standard: ${uri.port}."
                    } else {
                        "The URL usa uno schema e contiene il giusto hostname"
                    }
            ),

            SecurityCheck(
                name = "Caratteristiche sospette",
                status =
                    if (hasSuspiciousCharacteristics)
                        SecurityCheckStatus.WARNING
                    else
                        SecurityCheckStatus.PASSED,
                description =
                    suspiciousDescription(
                        uri,
                        normalizedHostname,
                        hasSuspiciousCharacteristics
                    )
            )
        )

        val score =
            calculateScore(
                isHttps = isHttps,
                isHttp = isHttp,
                isIpAddress = isIpAddress,
                hasNonStandardPort = hasNonStandardPort,
                hasSuspiciousCharacteristics =
                    hasSuspiciousCharacteristics
            )

        return SecurityReport(
            score = score,
            status = scoreToSecurityLevel(score),
            passedChecks =
                checks.count {
                    it.status == SecurityCheckStatus.PASSED
                },
            warnings =
                checks.count {
                    it.status == SecurityCheckStatus.WARNING
                },
            checks = checks
        )
    }

    /* Il punteggio parte da 100 e diminuisce
       quando vengono trovati elementi sospetti */
    private fun calculateScore(
        isHttps: Boolean,
        isHttp: Boolean,
        isIpAddress: Boolean,
        hasNonStandardPort: Boolean,
        hasSuspiciousCharacteristics: Boolean
    ): Int {
        var score = 100
        if (!isHttps) {
            score -= 20
        }
        if (isHttp) {
            score -= 10
        }
        if (isIpAddress) {
            score -= 20
        }
        if (hasNonStandardPort) {
            score -= 15
        }
        if (hasSuspiciousCharacteristics) {
            score -= 25
        }

        return score.coerceIn(0, 100)
    }

    private fun scoreToSecurityLevel(score: Int): SecurityLevel =
        when (score) {
            in 90..100 -> SecurityLevel.STRONG
            in 70..89 -> SecurityLevel.MODERATE
            in 40..69 -> SecurityLevel.WEAK
            else -> SecurityLevel.CRITICAL
        }

    /* Se l'input non è valido, dà che il report è fallito con score pari a 0 */
    private fun invalidReport(
        reason: String
    ): SecurityReport {
        val checks = listOf(
            SecurityCheck(
                name = "URL valido",
                status = SecurityCheckStatus.FAILED,
                description = reason
            )
        )

        return SecurityReport(
            score = 0,
            status = SecurityLevel.CRITICAL,
            passedChecks = 0,
            warnings = 0,
            checks = checks
        )
    }

    /*
       HTTP utilizza la porta 80 mentre HTTPS 443
       se la porta è diversa da quella standard viene segnalata(perciò)
       però non viene considerata malevola
       */
    private fun hasNonStandardPort(
        uri: URI,
        scheme: String
    ): Boolean {
        if (uri.port == -1) {
            return false
        }
        return when (scheme) {
            HTTPS_SCHEME -> uri.port != 443
            HTTP_SCHEME -> uri.port != 80
            else -> true
        }
    }

    //Abbiamo fatto qualche controllo tipo controlli se la blacklist viene utilizzata
    private fun hasSuspiciousCharacteristics(
        uri: URI,
        hostname: String
    ): Boolean {

        val labels = hostname.split('.').filter { it.isNotEmpty() }
        val hasUserInfo = !uri.userInfo.isNullOrBlank()
        val hasPunycode = labels.any { it.startsWith("xn--") }
        val hasManySubdomains = labels.size >= 5

        return hasUserInfo ||
                hasPunycode ||
                hasManySubdomains
    }

    private fun suspiciousDescription(
        uri: URI,
        hostname: String,
        suspicious: Boolean
    ): String {

        if (!suspicious) {
            return "No rilevamento caratteristiche sospette."
        }
        val hostnameParts = hostname.split(".").filter{it.isNotEmpty()}
        val reasons =
            mutableListOf<String>()
        if (!uri.userInfo.isNullOrBlank()) {
            reasons += "informazioni utente incorporate nell'URL"
        }
        if (hostnameParts.any { it.startsWith("xn--") }) {
            reasons += "hostname codificato in punycode"
        }

        if (hostnameParts.size >= 5) {
            reasons += "many subdomains"
        }
        return "Possibili caratteristiche sospette: ${reasons.joinToString(", ")}."
    }

    /* Controllo sintattico del hostname
       Sono accettati hostname DNS convenzionali e indirizzi IP */
    private fun isValidHostname(hostname: String): Boolean {
        val normalizedHostname = hostname.trimEnd('.')
        if (
            normalizedHostname.isEmpty() ||
            normalizedHostname.length > 253
        ) {
            return false
        }
        if (hostnameIsIp(normalizedHostname)) {
            return true
        }
        val labels = normalizedHostname.split('.')
        return labels.all { label ->
            label.isNotEmpty() &&
                    label.length <= 63 &&
                    label.firstOrNull()?.isLetterOrDigit() == true &&
                    label.lastOrNull()?.isLetterOrDigit() == true &&
                    label.all { it.isLetterOrDigit() || it == '-' }
        }
    }

    // Riconoscimento locale di IPv4 e IPv6, senza richieste DNS.
    private fun hostnameIsIp(
        hostname: String
    ): Boolean {
        val value = hostname.removePrefix("[").removeSuffix("]")
        val ipv4Parts = value.split('.')
        if (ipv4Parts.size == 4 &&
            ipv4Parts.all { part ->
                part.toIntOrNull()?.let { number -> number in 0..255 } == true
            }
        ) {
            return true
        }
        if (!value.contains(':')) {
            return false
        }
        val hextets = value.split(':')
        if (hextets.size > 8) {
            return false
        }
        var emptyGroups = 0
        for (hextet in hextets) {
            if (hextet.isEmpty()) {
                emptyGroups++
            } else if (
                hextet.length > 4 ||
                hextet.any {
                    it.digitToIntOrNull(16) == null
                }
            ) {

                return false
            }
        }
        return emptyGroups <= 2
    }

    // Restituisce soltanto l'hostname validato per le richieste di rete.
    fun extractHostname(input: String): String? {
        val value = input.trim()
        if (value.isBlank()) {
            return null
        }
        val uri = try {
            URI(value)
        } catch (_: Exception) {
            return null
        }
        val scheme = uri.scheme?.lowercase()
        if (scheme != HTTP_SCHEME && scheme != HTTPS_SCHEME) {
            return null
        }
        return uri.host?.takeIf { isValidHostname(it) }
    }
}