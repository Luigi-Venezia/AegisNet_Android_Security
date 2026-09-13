package com.example.aegisnet

import com.example.aegisnet.model.SecurityCheckStatus
import com.example.aegisnet.model.SecurityLevel
import com.example.aegisnet.security.UrlSecurityAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlSecurityAnalyzerTest {

    private val analyzer =
        UrlSecurityAnalyzer()

    @Test
    fun httpsExampleCom_isLowRisk() {

        val report =
            analyzer.analyze(
                "https://example.com"
            )

        assertEquals(
            100,
            report.score
        )

        assertEquals(
            SecurityLevel.STRONG,
            report.status
        )

        assertEquals(
            7,
            report.passedChecks
        )

        assertEquals(
            0,
            report.warnings
        )
    }

    @Test
    fun httpUrl_generatesEncryptionWarnings() {

        val report =
            analyzer.analyze(
                "http://example.com"
            )

        assertEquals(
            70,
            report.score
        )

        assertEquals(
            SecurityLevel.MODERATE,
            report.status
        )

        assertTrue(
            report.checks.any {
                it.name == "HTTPS" &&
                        it.status ==
                        SecurityCheckStatus.WARNING
            }
        )
    }

    @Test
    fun ipHostname_generatesWarning() {

        val report =
            analyzer.analyze(
                "https://192.168.1.1"
            )

        assertEquals(
            80,
            report.score
        )

        assertEquals(
            SecurityLevel.MODERATE,
            report.status
        )

        assertTrue(
            report.checks.any {
                it.name == "IP Address" &&
                        it.status ==
                        SecurityCheckStatus.WARNING
            }
        )
    }

    @Test
    fun nonStandardPort_generatesWarning() {

        val report =
            analyzer.analyze(
                "https://example.com:8080"
            )

        assertEquals(
            85,
            report.score
        )

        assertEquals(
            SecurityLevel.MODERATE,
            report.status
        )

        assertTrue(
            report.checks.any {
                it.name == "Non-standard Port" &&
                        it.status ==
                        SecurityCheckStatus.WARNING
            }
        )
    }

    @Test
    fun missingScheme_isInvalid() {

        val report =
            analyzer.analyze(
                "example.com"
            )

        assertEquals(
            0,
            report.score
        )

        assertEquals(
            SecurityLevel.CRITICAL,
            report.status
        )

        assertEquals(
            SecurityCheckStatus.FAILED,
            report.checks.first().status
        )
    }

    @Test
    fun emptyInput_isInvalid() {

        val report =
            analyzer.analyze("")

        assertEquals(
            0,
            report.score
        )

        assertEquals(
            SecurityLevel.CRITICAL,
            report.status
        )

        assertTrue(
            report.checks
                .first()
                .description
                .contains(
                    "empty",
                    ignoreCase = true
                )
        )
    }

    @Test
    fun malformedUrl_isInvalid() {

        val report =
            analyzer.analyze(
                "https://[bad"
            )

        assertEquals(
            0,
            report.score
        )

        assertEquals(
            SecurityLevel.CRITICAL,
            report.status
        )

        assertEquals(
            SecurityCheckStatus.FAILED,
            report.checks.first().status
        )
    }
}