package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.BotanicalMood
import com.example.data.model.DiaryEntry
import com.example.util.DateAndPhotoUtils
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class DiaryRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth get() = Firebase.auth

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

    fun observeEntries(userId: String = requireUserId()): Flow<List<DiaryEntry>> = flow {
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
                            // Automatically delete previously seeded demo entries from Firestore
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
        photosBase64: List<String>,
        isExistingDocument: Boolean = false
    ): Result<String> {
        val uid = requireUserId()
        val cleanId = if (!entryId.isNullOrBlank()) entryId else "day_${dateIso}_${UUID.randomUUID().toString().take(6)}"
        val docRef = db.collection("users")
            .document(uid)
            .collection("entries")
            .document(cleanId)
        val monthDay = if (dateIso.length >= 10) dateIso.substring(5, 10) else "01-01"
        val cleanTags = tags.map { it.trim().removePrefix("#") }
            .filter { it.isNotBlank() }
            .distinct()
            .take(15)
        val cleanPhotos = photosBase64.take(4)
        val safeContent = content.trim().ifBlank { "Запись дня" }

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
                    "photosBase64" to cleanPhotos,
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
                    "photosBase64" to cleanPhotos,
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
}
