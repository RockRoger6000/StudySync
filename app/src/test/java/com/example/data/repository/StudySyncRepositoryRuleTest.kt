package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.ScheduleBlock
import com.example.data.model.ScheduleCategory
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class StudySyncRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createTask_validPayload_createsDocumentAndReturnsId() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = StudySyncRepository(firestore)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createTask(
                title = "Tugas Matematika Diskrit",
                subject = "Informatika",
                dueDate = "2026-10-25",
                priority = "HIGH"
            )
        }
        assertTrue(createResult.isSuccess)
        val taskId = createResult.getOrThrow()
        assertNotNull(taskId)
        assertTrue(taskId.isNotEmpty())
    }

    @Test
    fun getUserTasks_authenticatedOwner_returnsMatchingTasks() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = StudySyncRepository(firestore)
        val taskId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createTask(
                title = "Laporan Fisika Dasar",
                subject = "Fisika",
                dueDate = "2026-10-26",
                priority = "MEDIUM"
            ).getOrThrow()
        }

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeTasks().first { list -> list.any { it.id == taskId } }
        }
        assertTrue(observed.any { it.id == taskId })
    }

    @Test
    fun getTaskById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = StudySyncRepository(firestore)
        val taskId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createTask(
                title = "Alice Private Assignment",
                subject = "Biologi",
                dueDate = "2026-10-27"
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = StudySyncRepository(firestore)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                bobRepo.getTaskById(taskId).getOrThrow()
            }
            fail("Expected FirebaseFirestoreException PERMISSION_DENIED")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
    }

    @Test
    fun observeTasks_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = StudySyncRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeTasks().first()
            }
            fail("Expected IllegalStateException for unauthenticated user")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("User must be signed in with Google") == true)
        }
    }

    @Test
    fun createSchedule_validBlock_createsSuccessfully() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = StudySyncRepository(firestore)

        val schedule = ScheduleBlock(
            title = "Kuliah Algoritma",
            category = ScheduleCategory.STUDY.name,
            dayOfWeek = 1,
            startTime = "08:00",
            endTime = "10:00",
            locationName = "Gedung A Ruang 201"
        )
        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createSchedule(schedule)
        }
        assertTrue(result.isSuccess)
    }

    private companion object {
        const val ALICE_EMAIL = "alice@test.com"
        const val BOB_EMAIL = "bob@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
