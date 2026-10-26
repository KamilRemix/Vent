package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.BotanicalMood
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class DiaryRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveAndObserveEntry_authenticatedOwner_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = DiaryRepository(firestore)
        val uniqueEntryId = "entry_${UUID.randomUUID()}"

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveEntry(
                entryId = uniqueEntryId,
                dateIso = "2026-10-25",
                mood = BotanicalMood.BLOOM,
                moodIntensity = 5,
                title = "Теплый день",
                content = "Запись в личном дневнике",
                reflectionPrompt = "Что сегодня дало мне ощущение опоры?",
                reflectionAnswer = "Спокойная прогулка",
                tags = listOf("природа", "уют"),
                photosBase64 = emptyList(),
                isExistingDocument = false
            )
        }
        assertTrue("Expected saveEntry to succeed: ${saveResult.exceptionOrNull()}", saveResult.isSuccess)

        val entries = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeEntries(aliceUid).first { list -> list.any { it.id == uniqueEntryId } }
        }
        assertTrue(entries.any { it.id == uniqueEntryId && it.moodKey == BotanicalMood.BLOOM.key })
    }

    @Test
    fun getEntryById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = DiaryRepository(firestore)
        val uniqueEntryId = "entry_${UUID.randomUUID()}"

        val createRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.saveEntry(
                entryId = uniqueEntryId,
                dateIso = "2026-10-24",
                mood = BotanicalMood.CALM_LEAF,
                moodIntensity = 4,
                title = "Секретная запись",
                content = "Личные мысли Алисы",
                reflectionPrompt = null,
                reflectionAnswer = null,
                tags = listOf("тишина"),
                photosBase64 = emptyList(),
                isExistingDocument = false
            )
        }
        assertTrue(createRes.isSuccess)

        signInTestUser(BOB_EMAIL)
        val bobRepo = DiaryRepository(firestore)
        val crossReadResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getEntryById(ownerUid = aliceUid, entryId = uniqueEntryId)
        }
        assertTrue(crossReadResult.isFailure)
        val fsEx = crossReadResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    @Test
    fun observeEntries_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repository = DiaryRepository(firestore)

        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeEntries(aliceUid).first()
            }
        } catch (t: Throwable) {
            caught = t
        }
        assertNotNull("Expected unauthenticated observeEntries to throw", caught)
        val firestoreException = caught?.unwrapFirestoreException()
        assertNotNull("Expected nested FirebaseFirestoreException, got $caught", firestoreException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice@herbarium.test"
        const val BOB_EMAIL = "bob@herbarium.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
