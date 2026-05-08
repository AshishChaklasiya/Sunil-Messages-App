package com.smsnew.messenger.helpers

object OtpDetector {

    private val OTP_KEYWORDS = listOf(
        "otp", "one time password", "one-time password",
        "verification code", "verify code", "security code",
        "auth code", "authentication code", "login code",
        "passcode", "pin code", "confirmation code",
        "is your code", "is your otp", "use code", "your code is"
    )

    // 4 to 8 digit number (typical OTP length)
    private val OTP_DIGIT_REGEX = Regex("\\b\\d{4,8}\\b")

    fun isOtpMessage(body: String?): Boolean {
        if (body.isNullOrBlank()) return false
        val lower = body.lowercase()

        val hasKeyword = OTP_KEYWORDS.any { lower.contains(it) }
        val hasDigits = OTP_DIGIT_REGEX.containsMatchIn(body)

        // Must have BOTH a keyword AND a digit code to be considered OTP
        // This prevents false positives on normal messages with numbers
        return hasKeyword && hasDigits
    }
}
