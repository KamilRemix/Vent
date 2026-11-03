package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.R
import com.example.data.model.BotanicalMood
import com.example.data.model.DiaryEntry
import com.example.data.preferences.PinLockPreferences
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class DiaryRepository(
    private val db: FirebaseFirestore,
    private val appContext: Context? = null
) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            try {
                val resId = context.resources.getIdentifier("firestore_database_id", "string", context.packageName)
                if (resId != 0) context.getString(resId) else "ai-studio-android-e547b8f5-ef0e-49a9-96b8-b8956da99d71"
            } catch (_: Exception) {
                "ai-studio-android-e547b8f5-ef0e-49a9-96b8-b8956da99d71"
            }
        ),
        context.applicationContext
    )

    private val auth get() = Firebase.auth

    private val localPrefs: SharedPreferences? = appContext?.getSharedPreferences(
        "vent_local_guest_entries",
        Context.MODE_PRIVATE
    )

    private val localEntriesFlow = MutableStateFlow(loadLocalGuestEntries())

    private fun isGuestMode(userId: String? = auth.currentUser?.uid): Boolean {
        return userId.isNullOrBlank() || userId == PinLockPreferences.LOCAL_GUEST_UID || auth.currentUser == null
    }

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private val legacyDemoTitles = setOf(
        "Год назад: запах дождя и теплый чай",
        "Два года назад: прогулка в старом парке",
        "Утро без спешки",
        "Тихая задумчивость и время для себя"
    )

    private fun isLegacyDemoEntry(entry: DiaryEntry): Boolean {
        return entry.title in legacyDemoTitles ||
            entry.content.contains("собирали сухие листья для гербария") ||
            entry.content.contains("Перечитывая свои мысли, понимаю, как важно замедляться")
    }

    private fun getInitialHistoricalEntries(): List<DiaryEntry> {
        return listOf(
            DiaryEntry(
                id = "day_2026-11-01_5be877",
                userId = PinLockPreferences.LOCAL_GUEST_UID,
                dateIso = "2026-11-01",
                monthDay = "11-01",
                moodKey = "WARM_ROOT",
                moodIntensity = 4,
                title = "",
                content = "сегодня ходили в бассейн ну было неплохо вечером был папа",
                reflectionPrompt = "В какой момент сегодня я чувствовал себя наиболее естественно и легко?"
            ),
            DiaryEntry(
                id = "day_2026-10-31_756509",
                userId = PinLockPreferences.LOCAL_GUEST_UID,
                dateIso = "2026-10-31",
                monthDay = "10-31",
                moodKey = "CALM_LEAF",
                moodIntensity = 4,
                title = "",
                content = "вчера я провел день дома вечером кодил",
                reflectionPrompt = "За какую маленькую деталь сегодняшнего дня я искренне благодарен?"
            ),
            DiaryEntry(
                id = "day_2026-10-30_6e7ed7",
                userId = PinLockPreferences.LOCAL_GUEST_UID,
                dateIso = "2026-10-30",
                monthDay = "10-30",
                moodKey = "BLOOM",
                moodIntensity = 5,
                title = "пятница",
                content = "сегодня я утром попытался покодить а потом поехал на гудок",
                reflectionPrompt = "Что сегодня дало мне ощущение внутренней опоры и спокойствия?"
            ),
            DiaryEntry(
                id = "day_2026-10-29_0c3bb4",
                userId = PinLockPreferences.LOCAL_GUEST_UID,
                dateIso = "2026-10-29",
                monthDay = "10-29",
                moodKey = "WINTER_SEED",
                moodIntensity = 1,
                title = "",
                content = "к тринадцати часам я ходил к репетитора было все нормально ну в целом день не плохой",
                reflectionPrompt = "Какая эмоция звучала сегодня громче всего и о чем она хотела сказать?"
            ),
            DiaryEntry(
                id = "day_2026-10-28_bf892a",
                userId = PinLockPreferences.LOCAL_GUEST_UID,
                dateIso = "2026-10-28",
                monthDay = "10-28",
                moodKey = "WARM_ROOT",
                moodIntensity = 4,
                title = "",
                content = "сегодня вроде было все неплохо ну утром немного грустненько щас вроде все норм!",
                reflectionPrompt = "За какую маленькую деталь сегодняшнего дня я искренне благодарен?"
            ),
            DiaryEntry(
                id = "day_2026-10-27_cda567",
                userId = PinLockPreferences.LOCAL_GUEST_UID,
                dateIso = "2026-10-27",
                monthDay = "10-27",
                moodKey = "MIST_FERN",
                moodIntensity = 3,
                title = "первый денёк каникул",
                content = "было трудное утро было много слез тоже думал что не смогу жить и в целом день трудный  но вечером было спокойнее",
                reflectionPrompt = "В какой момент сегодня я чувствовал себя наиболее естественно и легко?",
                reflectionAnswer = "вечером"
            ),
            DiaryEntry(
                id = "day_2026-10-26_ac7d00",
                userId = PinLockPreferences.LOCAL_GUEST_UID,
                dateIso = "2026-10-26",
                monthDay = "10-26",
                moodKey = "MIST_FERN",
                moodIntensity = 3,
                title = "sad day",
                content = "очень сильно устал было много стресса было много слез честно даже думал что не смогу жить дальше иза слез и этой двойки было тяжело думал что не смогу жить дальше",
                reflectionPrompt = "What do I need most right now — support, silence, or rest?"
            )
        )
    }

    private fun saveEntriesToPrefs(entries: List<DiaryEntry>) {
        val prefs = localPrefs ?: return
        val arr = JSONArray()
        for (entry in entries) {
            val obj = JSONObject().apply {
                put("id", entry.id)
                put("dateIso", entry.dateIso)
                put("monthDay", entry.monthDay)
                put("moodKey", entry.moodKey)
                put("moodIntensity", entry.moodIntensity)
                put("title", entry.title)
                put("content", entry.content)
                if (!entry.reflectionPrompt.isNullOrBlank()) put("reflectionPrompt", entry.reflectionPrompt)
                if (!entry.reflectionAnswer.isNullOrBlank()) put("reflectionAnswer", entry.reflectionAnswer)
                put("tags", JSONArray(entry.tags))
                put("photoUrls", JSONArray(entry.photoUrls))
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_LOCAL_ENTRIES_JSON, arr.toString()).apply()
    }

    private fun loadLocalGuestEntries(): List<DiaryEntry> {
        val raw = localPrefs?.getString(KEY_LOCAL_ENTRIES_JSON, null)
        if (raw == null) {
            val initial = getInitialHistoricalEntries()
            saveEntriesToPrefs(initial)
            return initial
        }
        return try {
            val arr = JSONArray(raw)
            val result = mutableListOf<DiaryEntry>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val tagsArr = obj.optJSONArray("tags") ?: JSONArray()
                val tags = (0 until tagsArr.length()).mapNotNull { idx ->
                    tagsArr.optString(idx).takeIf { it.isNotBlank() }
                }
                val photosArr = obj.optJSONArray("photoUrls") ?: JSONArray()
                val photos = (0 until photosArr.length()).mapNotNull { idx ->
                    photosArr.optString(idx).takeIf { it.isNotBlank() }
                }
                result.add(
                    DiaryEntry(
                        id = obj.optString("id"),
                        userId = PinLockPreferences.LOCAL_GUEST_UID,
                        dateIso = obj.optString("dateIso"),
                        monthDay = obj.optString("monthDay"),
                        moodKey = obj.optString("moodKey", BotanicalMood.CALM_LEAF.key),
                        moodIntensity = obj.optInt("moodIntensity", 4),
                        title = obj.optString("title"),
                        content = obj.optString("content"),
                        reflectionPrompt = obj.optString("reflectionPrompt").takeIf { it.isNotBlank() },
                        reflectionAnswer = obj.optString("reflectionAnswer").takeIf { it.isNotBlank() },
                        tags = tags,
                        photoUrls = photos
                    )
                )
            }
            result.sortedByDescending { it.dateIso }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun persistLocalGuestEntries(entries: List<DiaryEntry>) {
        val sorted = entries.sortedByDescending { it.dateIso }
        saveEntriesToPrefs(sorted)
        localEntriesFlow.value = sorted
    }

    private suspend fun syncGuestEntriesToCloudIfAny(cloudUid: String) {
        val guestEntries = loadLocalGuestEntries()
        if (guestEntries.isEmpty()) return
        try {
            for (entry in guestEntries) {
                val docRef = db.collection("users")
                    .document(cloudUid)
                    .collection("entries")
                    .document(entry.id)
                val createMap = mutableMapOf<String, Any>(
                    "userId" to cloudUid,
                    "dateIso" to entry.dateIso,
                    "monthDay" to entry.monthDay,
                    "moodKey" to entry.moodKey,
                    "moodIntensity" to entry.moodIntensity.coerceIn(1, 5),
                    "title" to entry.title.take(200),
                    "content" to entry.content.take(10000),
                    "tags" to entry.tags.take(15),
                    "photosBase64" to entry.photoUrls.take(4),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!entry.reflectionPrompt.isNullOrBlank()) {
                    createMap["reflectionPrompt"] = entry.reflectionPrompt.take(500)
                }
                if (!entry.reflectionAnswer.isNullOrBlank()) {
                    createMap["reflectionAnswer"] = entry.reflectionAnswer.take(5000)
                }
                docRef.set(createMap).await()
            }
            persistLocalGuestEntries(emptyList())
        } catch (_: Exception) {
            // Keep local entries if offline
        }
    }

    fun observeEntries(userId: String = auth.currentUser?.uid ?: PinLockPreferences.LOCAL_GUEST_UID): Flow<List<DiaryEntry>> = flow {
        if (isGuestMode(userId)) {
            localEntriesFlow.value = loadLocalGuestEntries()
            emitAll(localEntriesFlow)
            return@flow
        }

        syncGuestEntriesToCloudIfAny(userId)

        val path = "users/$userId/entries"
        emitAll(
            db.collection("users")
                .document(userId)
                .collection("entries")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        val entry = doc.toObject(
                            DiaryEntry::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                        if (entry != null && isLegacyDemoEntry(entry)) {
                            doc.reference.delete()
                            null
                        } else {
                            entry
                        }
                    }.sortedByDescending { it.dateIso }
                }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun getUserEntries(): Result<List<DiaryEntry>> {
        if (isGuestMode()) {
            return Result.success(loadLocalGuestEntries())
        }
        val uid = requireUserId()
        val path = "users/$uid/entries"
        return try {
            val snapshot = db.collection("users")
                .document(uid)
                .collection("entries")
                .whereEqualTo("userId", uid)
                .get()
                .await()
            val list = snapshot.documents.mapNotNull { doc ->
                doc.toObject(
                    DiaryEntry::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }.sortedByDescending { it.dateIso }
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun getEntryById(ownerUid: String, entryId: String): Result<DiaryEntry?> {
        if (isGuestMode(ownerUid)) {
            return Result.success(loadLocalGuestEntries().firstOrNull { it.id == entryId })
        }
        val path = "users/$ownerUid/entries/$entryId"
        return try {
            val doc = db.collection("users")
                .document(ownerUid)
                .collection("entries")
                .document(entryId)
                .get()
                .await()
            Result.success(
                doc.toObject(
                    DiaryEntry::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun saveEntry(
        entryId: String?,
        dateIso: String,
        mood: BotanicalMood,
        moodIntensity: Int,
        title: String,
        content: String,
        reflectionPrompt: String?,
        reflectionAnswer: String?,
        tags: List<String>,
        photoUrls: List<String>,
        isExistingDocument: Boolean = false
    ): Result<String> {
        val cleanId = if (!entryId.isNullOrBlank()) entryId else "day_${dateIso}_${UUID.randomUUID().toString().take(6)}"
        val monthDay = if (dateIso.length >= 10) dateIso.substring(5, 10) else "01-01"
        val cleanTags = tags.map { it.trim().removePrefix("#") }
            .filter { it.isNotBlank() }
            .distinct()
            .take(15)
        // Strictly store only download/file URLs (never Base64 strings)
        val cleanPhotoUrls = photoUrls
            .map { it.trim() }
            .filter { it.startsWith("https://") || it.startsWith("http://") || it.startsWith("file://") }
            .take(4)
        val safeContent = content.trim().ifBlank { "Daily entry" }

        if (isGuestMode()) {
            val current = loadLocalGuestEntries().toMutableList()
            val newEntry = DiaryEntry(
                id = cleanId,
                userId = PinLockPreferences.LOCAL_GUEST_UID,
                dateIso = dateIso,
                monthDay = monthDay,
                moodKey = mood.key,
                moodIntensity = moodIntensity.coerceIn(1, 5),
                title = title.trim().take(200),
                content = safeContent.take(10000),
                reflectionPrompt = reflectionPrompt?.trim()?.take(500),
                reflectionAnswer = reflectionAnswer?.trim()?.take(5000),
                tags = cleanTags,
                photoUrls = cleanPhotoUrls
            )
            val existingIdx = current.indexOfFirst { it.id == cleanId }
            if (existingIdx >= 0) {
                current[existingIdx] = newEntry
            } else {
                current.add(0, newEntry)
            }
            persistLocalGuestEntries(current)
            return Result.success(cleanId)
        }

        val uid = requireUserId()
        val docRef = db.collection("users")
            .document(uid)
            .collection("entries")
            .document(cleanId)

        return try {
            if (isExistingDocument) {
                val updateMap = mutableMapOf<String, Any>(
                    "dateIso" to dateIso,
                    "monthDay" to monthDay,
                    "moodKey" to mood.key,
                    "moodIntensity" to moodIntensity.coerceIn(1, 5),
                    "title" to title.trim().take(200),
                    "content" to safeContent.take(10000),
                    "tags" to cleanTags,
                    "photosBase64" to cleanPhotoUrls,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!reflectionPrompt.isNullOrBlank()) {
                    updateMap["reflectionPrompt"] = reflectionPrompt.trim().take(500)
                }
                if (!reflectionAnswer.isNullOrBlank()) {
                    updateMap["reflectionAnswer"] = reflectionAnswer.trim().take(5000)
                }
                docRef.update(updateMap).await()
            } else {
                val createMap = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "dateIso" to dateIso,
                    "monthDay" to monthDay,
                    "moodKey" to mood.key,
                    "moodIntensity" to moodIntensity.coerceIn(1, 5),
                    "title" to title.trim().take(200),
                    "content" to safeContent.take(10000),
                    "tags" to cleanTags,
                    "photosBase64" to cleanPhotoUrls,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!reflectionPrompt.isNullOrBlank()) {
                    createMap["reflectionPrompt"] = reflectionPrompt.trim().take(500)
                }
                if (!reflectionAnswer.isNullOrBlank()) {
                    createMap["reflectionAnswer"] = reflectionAnswer.trim().take(5000)
                }
                docRef.set(createMap).await()
            }
            Result.success(cleanId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(
                e,
                if (isExistingDocument) OperationType.UPDATE else OperationType.CREATE,
                docRef.path
            )
            Result.failure(e)
        }
    }

    suspend fun deleteEntry(entryId: String): Result<Unit> {
        if (isGuestMode()) {
            val updated = loadLocalGuestEntries().filterNot { it.id == entryId }
            persistLocalGuestEntries(updated)
            return Result.success(Unit)
        }
        val uid = requireUserId()
        val docRef = db.collection("users")
            .document(uid)
            .collection("entries")
            .document(entryId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteAllEntries(): Result<Int> {
        if (isGuestMode()) {
            val count = loadLocalGuestEntries().size
            persistLocalGuestEntries(emptyList())
            return Result.success(count)
        }
        val uid = requireUserId()
        val path = "users/$uid/entries"
        return try {
            val snapshot = db.collection("users")
                .document(uid)
                .collection("entries")
                .whereEqualTo("userId", uid)
                .get()
                .await()
            var deletedCount = 0
            for (doc in snapshot.documents) {
                doc.reference.delete().await()
                deletedCount++
            }
            Result.success(deletedCount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    private companion object {
        const val KEY_LOCAL_ENTRIES_JSON = "local_entries_json"
    }
}
