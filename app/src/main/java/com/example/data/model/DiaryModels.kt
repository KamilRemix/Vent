package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.util.AppLanguage
import com.example.util.AppLocaleHolder
import com.example.util.DateAndPhotoUtils
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Calendar
import java.util.Locale

enum class BotanicalMood(
    val key: String,
    val titleEn: String,
    val subtitleEn: String,
    val titleRu: String,
    val subtitleRu: String,
    val defaultIntensity: Int,
    val primaryColorHex: Long,
    val softBgHex: Long,
    val accentHex: Long
) {
    BLOOM(
        key = "BLOOM",
        titleEn = "Joy",
        subtitleEn = "Energy & light",
        titleRu = "Радость",
        subtitleRu = "Энергия и свет",
        defaultIntensity = 5,
        primaryColorHex = 0xFFC87A5B,
        softBgHex = 0xFFF9EAE2,
        accentHex = 0xFFD4A359
    ),
    CALM_LEAF(
        key = "CALM_LEAF",
        titleEn = "Calm",
        subtitleEn = "Clarity & peace",
        titleRu = "Спокойствие",
        subtitleRu = "Ясность и покой",
        defaultIntensity = 4,
        primaryColorHex = 0xFF4A6B53,
        softBgHex = 0xFFE6EFE8,
        accentHex = 0xFF7A9E84
    ),
    WARM_ROOT(
        key = "WARM_ROOT",
        titleEn = "Warmth",
        subtitleEn = "Comfort & warmth",
        titleRu = "Тепло",
        subtitleRu = "Уют и тепло",
        defaultIntensity = 4,
        primaryColorHex = 0xFFB8863B,
        softBgHex = 0xFFF7EFE0,
        accentHex = 0xFFD4A359
    ),
    MIST_FERN(
        key = "MIST_FERN",
        titleEn = "Sadness",
        subtitleEn = "Quiet reflection",
        titleRu = "Грусть",
        subtitleRu = "Тихая грусть",
        defaultIntensity = 3,
        primaryColorHex = 0xFF5B757A,
        softBgHex = 0xFFE6EEF0,
        accentHex = 0xFF88A4A9
    ),
    AUTUMN_WIND(
        key = "AUTUMN_WIND",
        titleEn = "Anxiety",
        subtitleEn = "Racing thoughts",
        titleRu = "Тревога",
        subtitleRu = "Поток мыслей",
        defaultIntensity = 2,
        primaryColorHex = 0xFFA95C47,
        softBgHex = 0xFFF5E5E0,
        accentHex = 0xFFD0836C
    ),
    WINTER_SEED(
        key = "WINTER_SEED",
        titleEn = "Tiredness",
        subtitleEn = "Rest & stillness",
        titleRu = "Усталость",
        subtitleRu = "Отдых и тишина",
        defaultIntensity = 1,
        primaryColorHex = 0xFF6E6259,
        softBgHex = 0xFFEDE8E3,
        accentHex = 0xFF9C8E83
    );

    val title: String
        get() = if (AppLocaleHolder.currentLanguage == AppLanguage.RU) titleRu else titleEn

    val subtitle: String
        get() = if (AppLocaleHolder.currentLanguage == AppLanguage.RU) subtitleRu else subtitleEn

    fun localizedTitle(lang: AppLanguage = AppLocaleHolder.currentLanguage): String =
        if (lang == AppLanguage.RU) titleRu else titleEn

    fun localizedSubtitle(lang: AppLanguage = AppLocaleHolder.currentLanguage): String =
        if (lang == AppLanguage.RU) subtitleRu else subtitleEn

    val primaryColor: Color get() = Color(primaryColorHex)
    val softBgColor: Color get() = Color(softBgHex)
    val accentColor: Color get() = Color(accentHex)

    companion object {
        fun fromKey(key: String?): BotanicalMood {
            return entries.firstOrNull { it.key == key } ?: CALM_LEAF
        }
    }
}

@IgnoreExtraProperties
data class DiaryEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateIso: String = "",
    val monthDay: String = "",
    val moodKey: String = BotanicalMood.CALM_LEAF.key,
    val moodIntensity: Int = 4,
    val title: String = "",
    val content: String = "",
    val reflectionPrompt: String? = null,
    val reflectionAnswer: String? = null,
    val tags: List<String> = emptyList(),
    @get:PropertyName("photosBase64")
    @set:PropertyName("photosBase64")
    var photoUrls: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun botanicalMood(): BotanicalMood = BotanicalMood.fromKey(moodKey)
    fun mood(): BotanicalMood = botanicalMood()
    fun isFutureLetter(): Boolean =
        id.startsWith("capsule_") || reflectionPrompt?.startsWith("WRITTEN_AT:") == true

    fun writtenDateIso(): String {
        val raw = reflectionPrompt ?: return dateIso
        if (!raw.startsWith("WRITTEN_AT:")) return dateIso
        return raw.removePrefix("WRITTEN_AT:").take(10).takeIf { it.length == 10 } ?: dateIso
    }

    fun unlockTimeHhMm(): String {
        val raw = reflectionPrompt ?: return "09:00"
        val after = raw.substringAfter("UNLOCK_TIME:", "")
        if (after.length >= 5 && after[2] == ':') {
            return after.take(5)
        }
        return "09:00"
    }

    fun unlockHour(): Int = unlockTimeHhMm().take(2).toIntOrNull()?.coerceIn(0, 23) ?: 9
    fun unlockMinute(): Int = unlockTimeHhMm().takeLast(2).toIntOrNull()?.coerceIn(0, 59) ?: 0

    fun unlockTimestampMillis(): Long {
        val cal = DateAndPhotoUtils.parseIsoToCalendar(dateIso).apply {
            set(Calendar.HOUR_OF_DAY, unlockHour())
            set(Calendar.MINUTE, unlockMinute())
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun isUnlocked(todayIso: String = DateAndPhotoUtils.todayIso()): Boolean {
        val hasExplicitTime = reflectionPrompt?.contains("UNLOCK_TIME:") == true
        return if (hasExplicitTime) {
            System.currentTimeMillis() >= unlockTimestampMillis()
        } else {
            dateIso <= todayIso
        }
    }

    companion object {
        fun buildFutureLetterMetadata(writtenDateIso: String, hour: Int, minute: Int): String {
            val h = hour.coerceIn(0, 23)
            val m = minute.coerceIn(0, 59)
            return String.format(Locale.US, "WRITTEN_AT:%s|UNLOCK_TIME:%02d:%02d", writtenDateIso, h, m)
        }
    }
}

object ReflectionPrompts {
    val allPromptsEn = listOf(
        "What gave me a sense of inner grounding and safety today?",
        "What small detail from today am I genuinely grateful for?",
        "Which emotion spoke the loudest in me today, and what was it trying to say?",
        "When did I feel most at ease with myself today?",
        "What can I gently let go of tonight so tomorrow feels lighter?",
        "How did I care for my mind and body today?",
        "What conversation or quiet moment from today do I want to remember?",
        "What do I need most right now — support, silence, or rest?"
    )

    val allPromptsRu = listOf(
        "Что сегодня дало мне ощущение внутренней опоры и спокойствия?",
        "За какую маленькую деталь сегодняшнего дня я искренне благодарен?",
        "Какая эмоция звучала сегодня громче всего и о чем она хотела сказать?",
        "В какой момент сегодня я чувствовал себя наиболее естественно и легко?",
        "Что я могу мягко отпустить этим вечером, чтобы завтра стало легче?",
        "Как я позаботился о своем теле и душевном состоянии сегодня?",
        "Какой разговор или тихий момент сегодняшнего дня хочется сохранить в памяти?",
        "Что мне сейчас нужнее всего — поддержка, тишина или отдых?"
    )

    val allPrompts: List<String>
        get() = if (AppLocaleHolder.currentLanguage == AppLanguage.RU) allPromptsRu else allPromptsEn

    fun getPrompts(lang: AppLanguage = AppLocaleHolder.currentLanguage): List<String> {
        return if (lang == AppLanguage.RU) allPromptsRu else allPromptsEn
    }

    fun getPromptForDate(dateIso: String, lang: AppLanguage = AppLocaleHolder.currentLanguage): String {
        val list = getPrompts(lang)
        val idx = kotlin.math.abs(dateIso.hashCode()) % list.size
        return list[idx]
    }
}
