package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class BotanicalMood(
    val key: String,
    val titleRu: String,
    val subtitleRu: String,
    val defaultIntensity: Int,
    val primaryColorHex: Long,
    val softBgHex: Long,
    val accentHex: Long
) {
    BLOOM(
        key = "BLOOM",
        titleRu = "Радость",
        subtitleRu = "Вдохновение, энергия и подъем сил",
        defaultIntensity = 5,
        primaryColorHex = 0xFFC87A5B,
        softBgHex = 0xFFF9EAE2,
        accentHex = 0xFFD4A359
    ),
    CALM_LEAF(
        key = "CALM_LEAF",
        titleRu = "Спокойствие",
        subtitleRu = "Баланс, ясность и умиротворение",
        defaultIntensity = 4,
        primaryColorHex = 0xFF4A6B53,
        softBgHex = 0xFFE6EFE8,
        accentHex = 0xFF7A9E84
    ),
    WARM_ROOT(
        key = "WARM_ROOT",
        titleRu = "Тепло",
        subtitleRu = "Уют, благодарность и забота",
        defaultIntensity = 4,
        primaryColorHex = 0xFFB8863B,
        softBgHex = 0xFFF7EFE0,
        accentHex = 0xFFD4A359
    ),
    MIST_FERN(
        key = "MIST_FERN",
        titleRu = "Грусть",
        subtitleRu = "Задумчивость и тихая печаль",
        defaultIntensity = 3,
        primaryColorHex = 0xFF5B757A,
        softBgHex = 0xFFE6EEF0,
        accentHex = 0xFF88A4A9
    ),
    AUTUMN_WIND(
        key = "AUTUMN_WIND",
        titleRu = "Тревога",
        subtitleRu = "Волнение, спешка и беспокойство",
        defaultIntensity = 2,
        primaryColorHex = 0xFFA95C47,
        softBgHex = 0xFFF5E5E0,
        accentHex = 0xFFD0836C
    ),
    WINTER_SEED(
        key = "WINTER_SEED",
        titleRu = "Усталость",
        subtitleRu = "Мало сил, потребность в отдыхе и сне",
        defaultIntensity = 1,
        primaryColorHex = 0xFF6E6259,
        softBgHex = 0xFFEDE8E3,
        accentHex = 0xFF9C8E83
    );

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
    val photosBase64: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun botanicalMood(): BotanicalMood = BotanicalMood.fromKey(moodKey)
    fun mood(): BotanicalMood = botanicalMood()
}

object ReflectionPrompts {
    val allPrompts = listOf(
        "Что сегодня дало мне ощущение внутренней опоры и безопасности?",
        "За какую маленькую деталь сегодняшнего дня я чувствую благодарность?",
        "Какая эмоция звучала во мне громче всего, и о чем она говорит?",
        "В какой момент сегодня я чувствовал(а) себя наиболее комфортно?",
        "Что я могу спокойно отпустить перед сном, чтобы завтра было легче?",
        "Как я сегодня позаботился(ась) о себе и своем покое?",
        "Какой разговор или момент сегодняшнего дня хочется сохранить в памяти?",
        "В чем я сейчас больше всего нуждаюсь — в поддержке, тишине или отдыхе?"
    )

    val suggestedTags = listOf(
        "благодарность",
        "семья",
        "прогулка",
        "тишина",
        "мысли",
        "работа",
        "отдых",
        "уют",
        "самочувствие"
    )
}
