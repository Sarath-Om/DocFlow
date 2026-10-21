package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.DocEntity
import com.example.data.repository.DocFlowRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DocFlowRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createFolder_validPayload_createsFolder() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = DocFlowRepository(firestore)

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createFolder(name = "Finance", color = "#2563EB", icon = "folder")
        }
        assertTrue(result.isSuccess)
        val folderId = result.getOrThrow()
        assertNotNull(folderId)
        assertTrue(folderId.isNotEmpty())
    }

    @Test
    fun createDocument_validPayload_createsDocumentAndReads() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = DocFlowRepository(firestore)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createDocument(
                title = "Quarterly Report",
                folderId = "root",
                category = "Work",
                fileType = "pdf",
                fileName = "report_q3.pdf",
                fileSize = 4096L,
                content = "Quarterly financial summary...",
                isEncrypted = false,
                isOfflineCached = true,
                pageCount = 3L
            )
        }
        assertTrue("Create failed: " + (createResult.exceptionOrNull()?.message ?: "unknown"), createResult.isSuccess)
        val docId = createResult.getOrThrow()
        assertNotNull(docId)

        val docResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getDocumentById(docId)
        }
        assertTrue("Get failed: " + (docResult.exceptionOrNull()?.message ?: "unknown") + " cause: " + docResult.exceptionOrNull()?.cause, docResult.isSuccess)
        val doc = docResult.getOrThrow()
        assertNotNull(doc)
        assertEquals("Quarterly Report", doc?.title)
    }

    @Test
    fun observeDocuments_authenticatedOwner_emitsRealtimeUpdates() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = DocFlowRepository(firestore)

        val docId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createDocument(
                title = "Contract Doc",
                folderId = "root",
                category = "Contract",
                fileType = "docx",
                fileName = "contract.docx",
                fileSize = 8192L,
                content = "Service Level Agreement terms",
                isEncrypted = false,
                isOfflineCached = true,
                pageCount = 2L
            ).getOrThrow()
        }

        val emittedDocs = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeDocuments().first { list: List<DocEntity> ->
                list.any { it.id == docId }
            }
        }
        assertTrue(emittedDocs.any { it.id == docId })
    }

    @Test
    fun getDocumentById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = DocFlowRepository(firestore)
        val docId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createDocument(
                title = "Confidential NDA",
                folderId = "root",
                category = "Legal",
                fileType = "pdf",
                fileName = "nda.pdf",
                fileSize = 1024L,
                content = "Top secret clauses",
                isEncrypted = true,
                encryptionKeyHint = "PIN",
                isOfflineCached = false,
                pageCount = 1L
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        // Bob attempts to query Alice's subcollection directly
        var caughtException: FirebaseFirestoreException? = null
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users").document(aliceUid).collection("documents").document(docId).get().await()
            }
        } catch (e: FirebaseFirestoreException) {
            caughtException = e
        }
        assertNotNull("Expected permission denied exception", caughtException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, caughtException?.code)
    }

    @Test
    fun observeDocuments_unauthenticatedUser_failsWithPermissionDenied() {
        auth.signOut()
        val repository = DocFlowRepository(firestore)

        assertThrows(IllegalStateException::class.java) {
            repository.requireUserId()
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@test.com"
        const val BOB_EMAIL = "bob@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
