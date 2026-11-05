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
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.SetOptions
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

private const val KEY_LOCAL_ENTRIES_JSON = "local_entries_json"
private const val KEY_PENDING_SYNC_JSON = "pending_sync_entries_json"

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
        ).apply {
            try {
                firestoreSettings = FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(
                        PersistentCacheSettings.newBuilder()
                            .setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                            .build()
                    )
                    .build()
            } catch (_: Exception) {}
        },
        context.applicationContext
    )

    private val auth get() = Firebase.auth

    private val localPrefs: SharedPreferences? = appContext?.getSharedPreferences(
        "vent_local_guest_entries",
        Context.MODE_PRIVATE
    )

    private val tombstonePrefs: SharedPreferences? = appContext?.getSharedPreferences(
        "vent_tombstone_deleted_ids",
        Context.MODE_PRIVATE
    )

    private val offlineSyncPrefs: SharedPreferences? = appContext?.getSharedPreferences(
        "vent_pending_cloud_sync",
        Context.MODE_PRIVATE
    )

    private val localEntriesFlow = MutableStateFlow(loadLocalGuestEntries())

    private fun getTombstonedIds(): Set<String> {
        return tombstonePrefs?.getStringSet("deleted_ids", emptySet()) ?: emptySet()
    }

    private fun markEntryAsDeleted(entryId: String) {
        val current = getTombstonedIds().toMutableSet()
        current.add(entryId)
        tombstonePrefs?.edit()?.putStringSet("deleted_ids", current)?.apply()
    }

    private fun unmarkEntryAsDeleted(entryId: String) {
        val current = getTombstonedIds().toMutableSet()
        if (current.remove(entryId)) {
            tombstonePrefs?.edit()?.putStringSet("deleted_ids", current)?.apply()
        }
    }

    private fun isEntryDeleted(entryId: String): Boolean {
        return getTombstonedIds().contains(entryId)
    }

    private fun savePendingOfflineEntry(entry: DiaryEntry) {
        val prefs = offlineSyncPrefs ?: return
        val current = loadPendingOfflineEntries().toMutableList()
        val idx = current.indexOfFirst { it.id == entry.id }
        if (idx >= 0) {
            current[idx] = entry
        } else {
            current.add(entry)
        }
        val arr = JSONArray()
        for (e in current) {
            arr.put(entryToJson(e))
        }
        prefs.edit().putString(KEY_PENDING_SYNC_JSON, arr.toString()).apply()
    }

    private fun removePendingOfflineEntry(entryId: String) {
        val prefs = offlineSyncPrefs ?: return
        val current = loadPendingOfflineEntries().filterNot { it.id == entryId }
        val arr = JSONArray()
        for (e in current) {
            arr.put(entryToJson(e))
        }
        prefs.edit().putString(KEY_PENDING_SYNC_JSON, arr.toString()).apply()
    }

    private fun loadPendingOfflineEntries(): List<DiaryEntry> {
        val raw = offlineSyncPrefs?.getString(KEY_PENDING_SYNC_JSON, null)
        if (raw.isNullOrBlank()) return emptyList()
        return parseEntriesJson(raw).filterNot { isEntryDeleted(it.id) }
    }

    private fun isGuestMode(userId: String? = auth.currentUser?.uid): Boolean {
        return userId.isNullOrBlank() || userId == PinLockPreferences.LOCAL_GUEST_UID
    }

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private val legacyDemoTitles = setOf(
        "Год назад: запах дождя и теплый чай",
        "Два года назад: прогулка в старом парке",
        "Утро без спешки",
        "Тихая задумчивость и время для себя",
        "первый денёк каникул",
        "sad day",
        "пятница"
    )

    private fun isLegacyDemoOrPersonalEntry(entry: DiaryEntry): Boolean {
        return entry.title in legacyDemoTitles ||
            entry.id.startsWith("day_2026-10-") ||
            entry.id.startsWith("day_2026-11-01") ||
            entry.content.contains("собирали сухие листья для гербария") ||
            entry.content.contains("ходили в бассейн") ||
            entry.content.contains("вечером кодил") ||
            entry.content.contains("поехал на гудок") ||
            entry.content.contains("к репетитора") ||
            entry.content.contains("было много слез") ||
            entry.content.contains("Перечитывая свои мысли, понимаю, как важно замедляться")
    }

    private fun isLegacyDemoEntry(entry: DiaryEntry): Boolean {
        return entry.title in setOf(
            "Год назад: запах дождя и теплый чай",
            "Два года назад: прогулка в старом парке",
            "Утро без спешки",
            "Тихая задумчивость и время для себя"
        ) ||
            entry.content.contains("собирали сухие листья для гербария") ||
            entry.content.contains("Перечитывая свои мысли, понимаю, как важно замедляться")
    }

    private fun entryToJson(entry: DiaryEntry): JSONObject {
        return JSONObject().apply {
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
    }

    private fun parseEntriesJson(raw: String): List<DiaryEntry> {
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
                val entry = DiaryEntry(
                    id = obj.optString("id"),
                    userId = auth.currentUser?.uid ?: PinLockPreferences.LOCAL_GUEST_UID,
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
                if (!isLegacyDemoOrPersonalEntry(entry) && !isEntryDeleted(entry.id)) {
                    result.add(entry)
                }
            }
            result.sortedByDescending { it.dateIso }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun entryToCloudMap(entry: DiaryEntry, uid: String): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "userId" to uid,
            "dateIso" to entry.dateIso,
            "monthDay" to entry.monthDay,
            "moodKey" to entry.moodKey,
            "moodIntensity" to entry.moodIntensity.coerceIn(1, 5),
            "title" to entry.title.take(200),
            "content" to entry.content.take(10000),
            "tags" to entry.tags.take(15),
            "photosBase64" to entry.photoUrls.take(4),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!entry.reflectionPrompt.isNullOrBlank()) {
            map["reflectionPrompt"] = entry.reflectionPrompt!!.take(500)
        }
        if (!entry.reflectionAnswer.isNullOrBlank()) {
            map["reflectionAnswer"] = entry.reflectionAnswer!!.take(5000)
        }
        return map
    }

    private fun saveEntriesToPrefs(entries: List<DiaryEntry>) {
        val prefs = localPrefs ?: return
        val arr = JSONArray()
        for (entry in entries) {
            arr.put(entryToJson(entry))
        }
        prefs.edit().putString(KEY_LOCAL_ENTRIES_JSON, arr.toString()).apply()
    }

    private fun loadLocalGuestEntries(): List<DiaryEntry> {
        val raw = localPrefs?.getString(KEY_LOCAL_ENTRIES_JSON, null)
        if (raw.isNullOrBlank()) return emptyList()
        return parseEntriesJson(raw).filterNot { isEntryDeleted(it.id) }
    }

    private fun persistLocalGuestEntries(entries: List<DiaryEntry>) {
        val sorted = entries.filterNot { isEntryDeleted(it.id) }.sortedByDescending { it.dateIso }
        saveEntriesToPrefs(sorted)
        localEntriesFlow.value = sorted
    }

    private suspend fun syncGuestEntriesToCloudIfAny(cloudUid: String) {
        val guestEntries = loadLocalGuestEntries().filterNot { isEntryDeleted(it.id) }
        if (guestEntries.isEmpty()) return
        try {
            for (entry in guestEntries) {
                if (isEntryDeleted(entry.id)) continue
                val docRef = db.collection("users")
                    .document(cloudUid)
                    .collection("entries")
                    .document(entry.id)
                val createMap = entryToCloudMap(entry, cloudUid)
                docRef.set(createMap, SetOptions.merge()).await()
            }
            persistLocalGuestEntries(emptyList())
        } catch (_: Exception) {
            // Keep local entries if offline
        }
    }

    private suspend fun syncPendingCloudEntries(cloudUid: String) {
        val pending = loadPendingOfflineEntries()
        if (pending.isEmpty()) return
        for (entry in pending) {
            if (isEntryDeleted(entry.id)) {
                removePendingOfflineEntry(entry.id)
                continue
            }
            try {
                val docRef = db.collection("users")
                    .document(cloudUid)
                    .collection("entries")
                    .document(entry.id)
                val map = entryToCloudMap(entry, cloudUid)
                docRef.set(map, SetOptions.merge()).await()
                removePendingOfflineEntry(entry.id)
            } catch (_: Exception) {
                // Keep in pending queue until network connects
            }
        }
    }

    fun observeEntries(userId: String = auth.currentUser?.uid ?: PinLockPreferences.LOCAL_GUEST_UID): Flow<List<DiaryEntry>> = flow {
        if (isGuestMode(userId)) {
            val local = loadLocalGuestEntries().filterNot { isEntryDeleted(it.id) }
            localEntriesFlow.value = local
            emitAll(localEntriesFlow)
            return@flow
        }

        syncGuestEntriesToCloudIfAny(userId)
        syncPendingCloudEntries(userId)

        val path = "users/$userId/entries"
        emitAll(
            db.collection("users")
                .document(userId)
                .collection("entries")
                .snapshots()
                .map { snapshot ->
                    val serverEntries = snapshot.documents.mapNotNull { doc ->
                        val entry = doc.toObject(
                            DiaryEntry::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                        if (entry != null) {
                            if (isLegacyDemoEntry(entry) || isEntryDeleted(entry.id)) {
                                doc.reference.delete()
                                null
                            } else {
                                entry
                            }
                        } else {
                            null
                        }
                    }
                    val pendingLocal = loadPendingOfflineEntries().filterNot { isEntryDeleted(it.id) }
                    val mergedMap = mutableMapOf<String, DiaryEntry>()
                    serverEntries.forEach { mergedMap[it.id] = it }
                    pendingLocal.forEach { mergedMap[it.id] = it }
                    mergedMap.values.filterNot { isEntryDeleted(it.id) }.sortedByDescending { it.dateIso }
                }
                .catch { error ->
                    val pending = loadPendingOfflineEntries().filterNot { isEntryDeleted(it.id) }
                    if (pending.isNotEmpty()) {
                        emit(pending)
                    } else if (error is Exception) {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
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
                .get()
                .await()
            val list = snapshot.documents.mapNotNull { doc ->
                val entry = doc.toObject(
                    DiaryEntry::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (entry != null && (isLegacyDemoEntry(entry) || isEntryDeleted(entry.id))) {
                    null
                } else {
                    entry
                }
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
        unmarkEntryAsDeleted(cleanId)
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

        val newEntry = DiaryEntry(
            id = cleanId,
            userId = auth.currentUser?.uid ?: PinLockPreferences.LOCAL_GUEST_UID,
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

        if (isGuestMode()) {
            val current = loadLocalGuestEntries().toMutableList()
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

        val dataMap = entryToCloudMap(newEntry, uid).toMutableMap()
        if (!isExistingDocument) {
            dataMap["createdAt"] = FieldValue.serverTimestamp()
        }

        return try {
            docRef.set(dataMap, SetOptions.merge()).await()
            removePendingOfflineEntry(cleanId)
            Result.success(cleanId)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Network failure or offline: save locally in pendingSync queue so entry is NEVER lost!
            savePendingOfflineEntry(newEntry)
            Result.success(cleanId)
        }
    }

    suspend fun deleteEntry(entryId: String): Result<Unit> {
        markEntryAsDeleted(entryId)
        removePendingOfflineEntry(entryId)
        val updated = loadLocalGuestEntries().filterNot { it.id == entryId }
        persistLocalGuestEntries(updated)

        val uid = auth.currentUser?.uid
        if (!uid.isNullOrBlank() && uid != PinLockPreferences.LOCAL_GUEST_UID) {
            val docRef = db.collection("users")
                .document(uid)
                .collection("entries")
                .document(entryId)
            try {
                docRef.delete().await()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Offline deletion handled via tombstones
            }
        }
        return Result.success(Unit)
    }

    suspend fun deleteAllEntries(): Result<Int> {
        val currentGuest = loadLocalGuestEntries()
        currentGuest.forEach { markEntryAsDeleted(it.id) }
        loadPendingOfflineEntries().forEach { markEntryAsDeleted(it.id) }
        persistLocalGuestEntries(emptyList())
        offlineSyncPrefs?.edit()?.clear()?.apply()

        val uid = auth.currentUser?.uid
        if (!uid.isNullOrBlank() && uid != PinLockPreferences.LOCAL_GUEST_UID) {
            val path = "users/$uid/entries"
            try {
                val snapshot = db.collection("users")
                    .document(uid)
                    .collection("entries")
                    .get()
                    .await()
                var deletedCount = 0
                for (doc in snapshot.documents) {
                    markEntryAsDeleted(doc.id)
                    doc.reference.delete().await()
                    deletedCount++
                }
                return Result.success(deletedCount)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {}
        }
        return Result.success(currentGuest.size)
    }
}
