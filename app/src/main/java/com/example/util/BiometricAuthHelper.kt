package com.example.util

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal

object BiometricAuthHelper {

    fun isBiometricAvailable(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val bm = context.getSystemService(BiometricManager::class.java) ?: return false
                val res = bm.canAuthenticate(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.BIOMETRIC_WEAK
                )
                res == BiometricManager.BIOMETRIC_SUCCESS
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val bm = context.getSystemService(BiometricManager::class.java) ?: return false
                @Suppress("DEPRECATION")
                bm.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS
            } else {
                context.packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
            }
        } catch (_: Exception) {
            false
        }
    }

    fun authenticate(
        context: Context,
        lang: AppLanguage = AppLocaleHolder.currentLanguage,
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            onError(
                trText(
                    lang,
                    "Biometric authentication is not supported on this Android version",
                    "Биометрия не поддерживается на этой версии Android"
                )
            )
            return
        }

        try {
            val title = trText(lang, "Fingerprint Unlock", "Вход по отпечатку пальца")
            val subtitle = trText(
                lang,
                "Touch the fingerprint sensor to unlock Vent",
                "Приложите палец к сканеру для входа в Vent"
            )
            val usePinText = trText(lang, "Use PIN Code", "Ввести PIN-код")

            val prompt = BiometricPrompt.Builder(context)
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButton(
                    usePinText,
                    context.mainExecutor
                ) { _, _ -> /* User chose to enter PIN manually */ }
                .build()

            val cancellationSignal = CancellationSignal()
            prompt.authenticate(
                cancellationSignal,
                context.mainExecutor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                        super.onAuthenticationSucceeded(result)
                        onSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                        super.onAuthenticationError(errorCode, errString)
                        // Ignore user cancellation / negative button tap
                        if (errorCode != BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED &&
                            errorCode != BiometricPrompt.BIOMETRIC_ERROR_CANCELED
                        ) {
                            errString?.toString()?.let { onError(it) }
                        }
                    }
                }
            )
        } catch (e: Exception) {
            onError(
                e.localizedMessage ?: trText(
                    lang,
                    "Could not start biometric prompt",
                    "Не удалось запустить проверку отпечатка пальца"
                )
            )
        }
    }
}
