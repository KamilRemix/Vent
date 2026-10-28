package com.example.data.preferences

import android.content.Context
import java.security.MessageDigest

class PinLockPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "soul_herbarium_security_prefs",
        Context.MODE_PRIVATE
    )

    fun isPinEnabled(): Boolean {
        return !prefs.getString(KEY_PIN_HASH, null).isNullOrBlank()
    }

    fun setPin(pin: String) {
        require(pin.length == 4 && pin.all { it.isDigit() }) {
            "PIN must be exactly 4 digits"
        }
        prefs.edit().putString(KEY_PIN_HASH, hashPin(pin)).apply()
    }

    fun verifyPin(pin: String): Boolean {
        val savedHash = prefs.getString(KEY_PIN_HASH, null) ?: return true
        return savedHash == hashPin(pin)
    }

    fun clearPin() {
        prefs.edit().remove(KEY_PIN_HASH).apply()
    }

    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun isAuthSkipped(): Boolean {
        return prefs.getBoolean(KEY_AUTH_SKIPPED, false)
    }

    fun setAuthSkipped(skipped: Boolean) {
        prefs.edit().putBoolean(KEY_AUTH_SKIPPED, skipped).apply()
    }

    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(("herbarium_salt_$pin").toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        const val LOCAL_GUEST_UID = "local_guest"
        private const val KEY_PIN_HASH = "pin_sha256_hash"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_unlock_enabled"
        private const val KEY_AUTH_SKIPPED = "google_auth_skipped"
    }
}

