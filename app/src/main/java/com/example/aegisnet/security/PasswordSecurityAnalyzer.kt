package com.example.aegisnet.security
import com.example.aegisnet.model.SecurityCheck
import com.example.aegisnet.model.SecurityCheckStatus
import com.example.aegisnet.model.SecurityLevel
import com.example.aegisnet.model.SecurityReport
// Gestisce l'analisi locale della password e la generazione del report
class PasswordSecurityAnalyzer {
    fun analyze(password: String):SecurityReport {
        val hasUppercase = password.any { it.isUpperCase()}
        val hasLowercase = password.any { it.isLowerCase()}
        val hasNumber = password.any { it.isDigit()}
        val hasSpecial = password.any { !it.isLetterOrDigit() && !it.isWhitespace() }
        val isTooShort = password.length < MIN_LENGTH
        val hasWeakPattern = containsWeakPattern(password)

        val checks= listOf(
            SecurityCheck(name="Length",status= when {
                    password.length >= RECOMMENDED_LENGTH->SecurityCheckStatus.PASSED
                    password.length >= MIN_LENGTH -> SecurityCheckStatus.WARNING
                    else->SecurityCheckStatus.FAILED},
                description = when {
                    password.length>=RECOMMENDED_LENGTH -> "Length is at least $RECOMMENDED_LENGTH characters."
                    password.length>=MIN_LENGTH -> "Length is acceptable, but $RECOMMENDED_LENGTH+ characters are recommended."
                    else -> "Password is shorter than the minimum of $MIN_LENGTH characters." }),
            SecurityCheck(name = "Uppercase",status =
                    if (hasUppercase) SecurityCheckStatus.PASSED
                    else SecurityCheckStatus.FAILED,
                description =
                    if(hasUppercase){
                        "Contains at least one uppercase letter."}
                    else "Add at least one uppercase letter."),

            SecurityCheck(name = "Lowercase",status =
                    if (hasLowercase) SecurityCheckStatus.PASSED
                    else SecurityCheckStatus.FAILED,
                if (hasLowercase) "Contains at least one lowercase letter."
                    else "Add at least one lowercase letter."),

            SecurityCheck(name ="Number", status= if (hasNumber)SecurityCheckStatus.PASSED
                    else SecurityCheckStatus.FAILED,description = if(hasNumber)
                        "Contains at least one number." else "Add at least one number."
            ),

            SecurityCheck("Special Character",
                if (hasSpecial) SecurityCheckStatus.PASSED
                    else SecurityCheckStatus.FAILED,
               if (hasSpecial) "Contains at least one special character."
                    else "Add at least one special character." ),

            SecurityCheck
                ("Weak Pattern", if (hasWeakPattern)
                        SecurityCheckStatus.WARNING
                    else SecurityCheckStatus.PASSED,
                    if (hasWeakPattern) "Contains an evidently weak or common pattern."
                    else "No evidently weak pattern detected."),

            SecurityCheck("Minimum Length",
                    if (isTooShort) SecurityCheckStatus.FAILED
                    else SecurityCheckStatus.PASSED,
                description= if (isTooShort) "The password is too short for this educational check."
                    else "The minimum length requirement is satisfied.")
        )

        val score = calculateScore(
            length = password.length,
            hasUppercase = hasUppercase,
            hasLowercase = hasLowercase,
            hasNumber = hasNumber,
            hasSpecial = hasSpecial,
            hasWeakPattern = hasWeakPattern)

        val level= when (score) {
            in 80..100 -> SecurityLevel.STRONG
            in 60..79 -> SecurityLevel.MODERATE
            in 40..59 -> SecurityLevel.WEAK
            else -> SecurityLevel.CRITICAL
        }

        return SecurityReport(score,level,checks.count{
                it.status == SecurityCheckStatus.PASSED},
            checks.count {
                it.status == SecurityCheckStatus.WARNING},
            checks = checks)
    }
    //Calcolo del punteggio
    private fun calculateScore(length: Int,hasUppercase:Boolean,hasLowercase: Boolean,
        hasNumber: Boolean,hasSpecial: Boolean,hasWeakPattern: Boolean):Int {
        val lengthPoints = when {
            length >= 16 -> 40
            length >= 12 ->28 + (length - 12) * 3
            length >= 8 ->20 + (length - 8) * 2
            else-> length * 2
        }
        val categoryPoints = listOf(hasUppercase,hasLowercase,hasNumber,hasSpecial)
            .count { it }*CATEGORY_POINTS
        val patternPenalty= if(hasWeakPattern) WEAK_PATTERN_PENALTY else 0
        return(lengthPoints+categoryPoints-patternPenalty).coerceIn(0, 100)
    }


    //Identifica combinazioni di caratteri ripetitive
    private fun containsWeakPattern(password: String): Boolean {
        if (password.isEmpty()){ return false}

        val normalized = password.lowercase()
        val commonPatterns = listOf("password", "qwerty",
            "123456","12345678",
            "abcdef","admin",
            "letmein","welcome")

        val repeatedCharacters= password.all{it == password.first()}

        return commonPatterns.any{ normalized.contains(it)} ||
                repeatedCharacters||hasSequentialCharacters(password)
    }
    private fun hasSequentialCharacters(password: String): Boolean {

        if (password.length < 4) {return false }
        return password.windowed(4).any { window ->val codes = window.map {
                it.code}

            val ascending =codes.zipWithNext().all { (a, b) -> b == a + 1
                }

            val descending = codes.zipWithNext().all { (a, b) -> b == a - 1
                }
            ascending || descending
        }
    }
    companion object {
        private const val MIN_LENGTH = 8
        private const val RECOMMENDED_LENGTH = 12
        private const val CATEGORY_POINTS = 15
        private const val WEAK_PATTERN_PENALTY = 25
    }
}