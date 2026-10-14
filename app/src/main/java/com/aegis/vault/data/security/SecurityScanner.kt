package com.aegis.vault.data.security

import com.aegis.vault.data.model.LoginItem

/** How the L4 "Needs attention" rows classify a login. */
enum class LoginIssue { None, Weak, Reused, Breached, NoTwoFactor }

/** Result of scanning the vault, shown on the L11 Security screen. */
data class SecurityReport(
    val score: Int,
    val scoreLabel: String,
    val scoreDescription: String,
    val weakCount: Int,
    val reusedCount: Int,
    val missing2faCount: Int,
    val breachedCount: Int,
    val weakLogins: List<LoginItem>,
) {
    val totalIssues: Int get() = weakCount + reusedCount + missing2faCount + breachedCount
}

/** The top ~30 passwords that show up in every credential-stuffing wordlist. */
private val COMMON_PASSWORDS: Set<String> = setOf(
    "123456", "123456789", "12345678", "12345", "1234567", "password", "password1",
    "qwerty", "abc123", "111111", "iloveyou", "monkey", "dragon", "letmein",
    "123123", "welcome", "football", "baseball", "admin", "login", "princess",
    "sunshine", "master", "shadow", "superman", "michael", "qwerty123", "batman",
    "trustno1", "hello",
)

/**
 * Runs the local security analysis behind the Security tab. Everything is
 * computed on the device from the vault contents — nothing is transmitted.
 */
class SecurityScanner {

    fun scan(logins: List<LoginItem>): SecurityReport {
        val passwordUse = logins.groupBy { it.password }

        val weak = logins.filter { isWeak(it.password) }
        val reused = logins.filter { (passwordUse[it.password]?.size ?: 0) > 1 }
        val missing2fa = logins.filter { it.totpSecret.isBlank() }
        val breached = logins.filter { BreachDatabase.isBreached(it.domain) }

        val score = computeScore(logins.size, weak.size, reused.size, missing2fa.size, breached.size)
        val label = scoreLabel(score)

        val description = when {
            score >= 90 -> "Your vault is in great shape. Nothing needs attention."
            logins.isEmpty() -> "Add logins and Aegis will score their strength here."
            else -> {
                val parts = mutableListOf<String>()
                if (weak.isNotEmpty()) parts += "${weak.size} weak"
                if (reused.isNotEmpty()) parts += "${reused.size} reused"
                if (breached.isNotEmpty()) parts += "${breached.size} breached"
                if (missing2fa.isNotEmpty()) parts += "${missing2fa.size} without two-factor"
                "Your vault needs attention: " + parts.joinToString(", ") + "."
            }
        }

        return SecurityReport(
            score = score,
            scoreLabel = label,
            scoreDescription = description,
            weakCount = weak.size,
            reusedCount = reused.size,
            missing2faCount = missing2fa.size,
            breachedCount = breached.size,
            weakLogins = (breached + weak).distinctBy { it.id }.take(4),
        )
    }

    /** A password is weak when short, too simple, or a known common password. */
    fun isWeak(password: String): Boolean {
        if (password.length < 12) return true
        if (password.length > 64) return false
        val hasLower = password.any { it.isLowerCase() }
        val hasUpper = password.any { it.isUpperCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSymbol = password.any { !it.isLetterOrDigit() }
        if (!hasLower && !hasUpper) return true
        if (!hasDigit && !hasSymbol) return true
        return password.lowercase() in COMMON_PASSWORDS
    }

    /** 0-4, matching the L2/L10 segmented strength meters. */
    fun strengthLevel(password: String): Int {
        if (password.isBlank()) return 0
        var points = 0
        if (password.length >= 8) points++
        if (password.length >= 12) points++
        if (password.length >= 16) points++
        if (password.any { it.isDigit() } && password.any { !it.isLetterOrDigit() }) points++
        val classes = listOf(
            password.any { it.isLowerCase() },
            password.any { it.isUpperCase() },
            password.any { it.isDigit() },
            password.any { !it.isLetterOrDigit() },
        ).count { it }
        if (classes >= 3) points++
        return points.coerceIn(0, 5)
    }

    /** The three L2 requirement checks. */
    fun requirementStatus(password: String): Triple<Boolean, Boolean, Boolean> {
        val meetsLength = password.length >= 12
        val hasCases = password.any { it.isLowerCase() } && password.any { it.isUpperCase() }
        val hasNumberOrSymbol = password.any { it.isDigit() } || password.any { !it.isLetterOrDigit() }
        return Triple(meetsLength, hasCases, hasNumberOrSymbol)
    }

    private fun computeScore(
        total: Int,
        weak: Int,
        reused: Int,
        missing2fa: Int,
        breached: Int,
    ): Int {
        if (total == 0) return 100
        val penalties = weak * 3 + reused * 2 + missing2fa + breached * 5
        return (100 - penalties).coerceIn(0, 100)
    }

    private fun scoreLabel(score: Int): String = when {
        score >= 90 -> "Excellent"
        score >= 80 -> "Good"
        score >= 60 -> "Needs work"
        else -> "At risk"
    }
}
