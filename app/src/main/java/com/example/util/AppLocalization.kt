package com.example.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale

enum class AppLanguage(val code: String, val displayName: String) {
    EN("en", "English"),
    RU("ru", "Русский");

    companion object {
        fun fromSystemLocale(): AppLanguage {
            val sysLang = Locale.getDefault().language.lowercase(Locale.ROOT)
            return if (sysLang.startsWith("ru")) RU else EN
        }
    }
}

enum class AppLanguageMode(val key: String) {
    SYSTEM("SYSTEM"),
    EN("EN"),
    RU("RU");

    fun resolveLanguage(): AppLanguage {
        return when (this) {
            SYSTEM -> AppLanguage.fromSystemLocale()
            EN -> AppLanguage.EN
            RU -> AppLanguage.RU
        }
    }

    companion object {
        fun fromKey(key: String?): AppLanguageMode {
            return entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: SYSTEM
        }
    }
}

class LanguagePreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "vent_language_prefs",
        Context.MODE_PRIVATE
    )

    fun getLanguageMode(): AppLanguageMode {
        val raw = prefs.getString(KEY_LANGUAGE_MODE, AppLanguageMode.SYSTEM.key)
        return AppLanguageMode.fromKey(raw)
    }

    fun setLanguageMode(mode: AppLanguageMode) {
        prefs.edit().putString(KEY_LANGUAGE_MODE, mode.key).apply()
        AppLocaleHolder.currentLanguage = mode.resolveLanguage()
    }

    fun getResolvedLanguage(): AppLanguage {
        val resolved = getLanguageMode().resolveLanguage()
        AppLocaleHolder.currentLanguage = resolved
        return resolved
    }

    private companion object {
        const val KEY_LANGUAGE_MODE = "app_language_mode"
    }
}

object AppLocaleHolder {
    @Volatile
    var currentLanguage: AppLanguage = AppLanguage.fromSystemLocale()
}

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.fromSystemLocale() }

@Composable
@ReadOnlyComposable
fun tr(en: String, ru: String): String {
    return if (LocalAppLanguage.current == AppLanguage.RU) ru else en
}

fun trText(lang: AppLanguage, en: String, ru: String): String {
    return if (lang == AppLanguage.RU) ru else en
}
