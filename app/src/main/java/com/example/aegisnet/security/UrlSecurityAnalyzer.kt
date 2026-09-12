package com.example.aegisnet.security

import com.example.aegisnet.model.SecurityCheck
import com.example.aegisnet.model.SecurityCheckStatus
import com.example.aegisnet.model.SecurityLevel
import com.example.aegisnet.model.SecurityReport
import java.net.URI

 /* Analizza l'URL localmente senza effettuare connessioni.
    Il risultato è solo indicativo e non garantisce che il sito sia sicuro */
class UrlSecurityAnalyzer {

     companion object {
         private const val HTTP_SCHEME= "http"
         private const val HTTPS_SCHEME= "https"
     }


     //Analizza l'URL e restituisce un report riutilizzando i model comuni
    fun analyze(input: String): SecurityReport {

        val value = input.trim() //Il valore viene letto eliminando eventuali spazi vuoti inutili
        if (value.isEmpty()) {   //Viene controllato se la casella è vuota
            return invalidReport("Input is empty.")
        }

        val uri = try {
            URI(value)
        } catch (_: Exception) {
            return invalidReport("The URL is malformed.")
        }

        val scheme= uri.scheme?.lowercase()
        val hostname= uri.host

        val validSchema= scheme == HTTP_SCHEME || scheme == HTTPS_SCHEME
        val validHostname= hostname != null && isValidHostname(hostname)

        //Si verifica se entrambi i valori sono true, in questo caso l'url è valido
        val validUrl= validSchema && validHostname

        if (!validSchema || !validHostname) {
            return invalidReport(
                when {
                    !validSchema ->
                        "Only HTTP and HTTPS URLs are supported."

                    !validHostname ->
                        "The URL does not contain a valid hostname."

                    else ->
                        "The URL is not valid."
                }
            )
        }

        //Dopo il controllo possiamo usare questi valori senza null
        val safeScheme= scheme!!
        val safeHostname= hostname!!


        val isHttps= safeScheme == HTTPS_SCHEME
        val isHttp= safeScheme == HTTP_SCHEME

        val isIpAddress= hostnameIsIp(safeHostname)
        val hasNonStandardPort= hasNonStandardPort(uri,safeScheme)
        val hasSuspiciousCharacteristics= hasSuspiciousCharacteristics(uri,safeHostname)

        val checks = listOf(

            SecurityCheck(
                name = "Valid URL",
                status = SecurityCheckStatus.PASSED,
                description =
                    "The URL has a supported scheme and a valid hostname."
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
                        "HTTPS encrypts the connection, but it does not prove that the site is trustworthy."
                    } else {
                        "The URL does not use HTTPS, so the connection is not protected by HTTPS encryption."
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
                        "HTTP is unencrypted and should be treated as a warning."
                    } else {
                        "The URL does not use plain HTTP."
                    }
            ),

            SecurityCheck(
                name = "Hostname",
                status = SecurityCheckStatus.PASSED,
                description = "Hostname: $safeHostname"
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
                        "The hostname is an IP address rather than a conventional domain name."
                    } else {
                        "The hostname is represented by a domain name."
                    }
            ),

            SecurityCheck(
                name = "Non-standard Port",
                status =
                    if (hasNonStandardPort)
                        SecurityCheckStatus.WARNING
                    else
                        SecurityCheckStatus.PASSED,
                description =
                    if (hasNonStandardPort) {
                        "The URL uses a non-standard port: ${uri.port}."
                    } else {
                        "The URL uses the standard port for its scheme or no explicit port."
                    }
            ),

            SecurityCheck(
                name = "Suspicious Characteristics",
                status =
                    if (hasSuspiciousCharacteristics)
                        SecurityCheckStatus.WARNING
                    else
                        SecurityCheckStatus.PASSED,
                description =
                    suspiciousDescription(
                        uri,
                        safeHostname,
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

    /* Riutilizza SecurityLevel già presente nel progetto
       La UI del URL Checker traduce questi livelli nei quattro livelli
       richiesti: LOW, MODERATE, HIGH e CRITICAL */
    private fun scoreToSecurityLevel(
        score: Int
    ): SecurityLevel =
        when (score) {

            in 90..100 ->
                SecurityLevel.STRONG

            in 70..89 ->
                SecurityLevel.MODERATE

            in 40..69 ->
                SecurityLevel.WEAK

            else ->
                SecurityLevel.CRITICAL
        }

    /* Report restituito quando l'URL non è analizzabile.
       Un valore non valido non viene trattato come un sito sicuro:
       l'analisi viene interrotta e il controllo Valid URL fallisce */
    private fun invalidReport(
        reason: String
    ): SecurityReport {

        val checks =
            listOf(
                SecurityCheck(
                    name = "Valid URL",
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

    /* HTTP utilizza normalmente la porta 80.
       HTTPS utilizza normalmente la porta 443.
       Una porta esplicita diversa da quella standard viene segnalata,
       ma non viene considerata automaticamente malevola */
    private fun hasNonStandardPort(
        uri: URI,
        scheme: String
    ): Boolean {

        if (uri.port == -1) {
            return false
        }

        return when (scheme) {

            HTTPS_SCHEME ->
                uri.port != 443

            HTTP_SCHEME ->
                uri.port != 80

            else ->
                true
        }
    }

    /* Controlli limitati a caratteristiche evidenti:
      - user-info inserito nell'authority
      - hostname
      - numero elevato di sottodomini
       Non vengono utilizzati blacklist, database esterni o threat
       intelligence */
    private fun hasSuspiciousCharacteristics(
        uri: URI,
        hostname: String
    ): Boolean {

        val labels =
            hostname
                .split('.')
                .filter {
                    it.isNotEmpty()
                }

        val hasUserInfo =
            !uri.userInfo.isNullOrBlank()

        val hasPunycode =
            labels.any {
                it.startsWith("xn--")
            }

        val hasManySubdomains =
            labels.size >= 5

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
            return "No evident suspicious characteristic was detected by the local heuristic."
        }

        val reasons =
            mutableListOf<String>()

        if (!uri.userInfo.isNullOrBlank()) {
            reasons += "embedded user information"
        }

        if (
            hostname
                .split('.')
                .any {
                    it.startsWith("xn--")
                }
        ) {
            reasons += "punycode hostname"
        }

        if (
            hostname
                .split('.')
                .count {
                    it.isNotEmpty()
                } >= 5
        ) {
            reasons += "many subdomains"
        }

        return "Potentially suspicious characteristic: " +
                reasons.joinToString(", ") +
                "."
    }

    /* Controllo sintattico del hostname
       Sono accettati hostname DNS convenzionali e indirizzi IP */
    private fun isValidHostname(
        hostname: String
    ): Boolean {

        val value =
            hostname.trimEnd('.')

        if (
            value.isEmpty() ||
            value.length > 253
        ) {
            return false
        }

        if (hostnameIsIp(value)) {
            return true
        }

        val labels =
            value.split('.')

        return labels.all { label ->

            label.isNotEmpty() &&
                    label.length <= 63 &&
                    label.firstOrNull()?.isLetterOrDigit() == true &&
                    label.lastOrNull()?.isLetterOrDigit() == true &&
                    label.all {
                        it.isLetterOrDigit() ||
                                it == '-'
                    }
        }
    }

    /* Riconoscimento sintattico locale di IPv4/IPv6
       Non viene utilizzato DNS perché il checker
       deve funzionare completamente offline */
    private fun hostnameIsIp(
        hostname: String
    ): Boolean {

        val value =
            hostname
                .removePrefix("[")
                .removeSuffix("]")

        val ipv4Parts =
            value.split('.')

        if (
            ipv4Parts.size == 4 &&
            ipv4Parts.all { part ->

                part
                    .toIntOrNull()
                    ?.let {
                            number ->
                        number in 0..255
                    } == true
            }
        ) {
            return true
        }

        if (!value.contains(':')) {
            return false
        }

        val hextets =
            value.split(':')

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

    /* Estrae l'hostname di un URL già valido secondo
       le regole del checker
       Questo metodo viene riutilizzato dal networking
       per inviare al servizio remoto soltanto l'hostname
       validato, non l'intero testo dell'utente */
    fun extractHostname(
        input: String
    ): String? {

        val value= input.trim()

        if (value.isEmpty()) {
            return null
        }

        val uri =
            try {
                URI(value)
            } catch (_: Exception) {
                return null
            }

        val scheme =
            uri.scheme?.lowercase()

        val hostname =
            uri.host

        if (
            scheme != HTTP_SCHEME &&
            scheme != HTTPS_SCHEME
        ) {
            return null
        }

        return hostname
            ?.takeIf {
                isValidHostname(it)
            }
    }
}