package com.example.util

import com.example.data.preferences.PinLockPreferences
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

object GoogleAuthValidator {
    /**
     * Validates that the active user is genuinely authenticated via Google OAuth / Firebase Auth.
     * Strictly rejects:
     * - Null sessions
     * - Anonymous Firebase sessions
     * - Guest Mode / Local placeholder UID (PinLockPreferences.LOCAL_GUEST_UID)
     * - Any account lacking a verified `google.com` provider with email
     */
    fun isGenuineGoogleUser(): Boolean {
        val user = Firebase.auth.currentUser ?: return false
        if (user.isAnonymous) return false
        val uid = user.uid
        if (uid.isBlank() || uid == PinLockPreferences.LOCAL_GUEST_UID) return false
        
        val hasGoogleProvider = user.providerData.any { it.providerId == "google.com" }
        val hasEmail = !user.email.isNullOrBlank()
        return hasGoogleProvider && hasEmail
    }

    fun getAiAuthRequiredMessage(isRussian: Boolean): String {
        return if (isRussian) {
            "Персонализация и психологический ИИ-анализ доступны только для авторизованных пользователей Google. Пожалуйста, выполните вход через Google."
        } else {
            "AI Personalization and psychological reflections require a verified Google account. Please sign in with Google to continue."
        }
    }
}
