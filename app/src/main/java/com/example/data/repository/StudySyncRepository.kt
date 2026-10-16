package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.GroupSchedule
import com.example.data.model.ScheduleBlock
import com.example.data.model.ScheduleCategory
import com.example.data.model.StudySession
import com.example.data.model.TaskItem
import com.example.data.model.UserProfile
import com.example.data.security.CryptoUtils
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class StudySyncRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing StudySync data.")
    }

    // --- Tasks ---

    fun observeTasks(): Flow<List<TaskItem>> = flow {
        val uid = requireUserId()
        val path = "tasks"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot -> snapshot.toObjects(TaskItem::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createTask(
        title: String,
        subject: String,
        description: String? = null,
        dueDate: String,
        dueTime: String? = null,
        priority: String = "MEDIUM",
        estimatedMinutes: Int = 60
    ): Result<String> {
        return try {
            val uid = requireUserId()
            val taskId = "task_" + UUID.randomUUID().toString().replace("-", "").take(16)
            val path = "tasks/$taskId"
            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "title" to title,
                "subject" to subject,
                "dueDate" to dueDate,
                "priority" to priority,
                "status" to "PENDING",
                "estimatedMinutes" to estimatedMinutes,
                "loggedMinutes" to 0,
                "reminderMinutesBefore" to 60,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!description.isNullOrBlank()) payload["description"] = description
            if (!dueTime.isNullOrBlank()) payload["dueTime"] = dueTime

            db.collection("tasks").document(taskId).set(payload).await()
            Result.success(taskId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "tasks")
            Result.failure(e)
        }
    }

    suspend fun getTaskById(taskId: String): Result<TaskItem?> {
        return try {
            val path = "tasks/$taskId"
            val snapshot = db.collection("tasks").document(taskId).get().await()
            if (!snapshot.exists()) {
                Result.success(null)
            } else {
                Result.success(snapshot.toObject(TaskItem::class.java))
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "tasks/$taskId")
            Result.failure(e)
        }
    }

    suspend fun updateTask(task: TaskItem): Result<Unit> {
        return try {
            val uid = requireUserId()
            val docRef = db.collection("tasks").document(task.id)
            val updates = mutableMapOf<String, Any>(
                "title" to task.title,
                "subject" to task.subject,
                "dueDate" to task.dueDate,
                "priority" to task.priority,
                "status" to task.status,
                "estimatedMinutes" to task.estimatedMinutes,
                "loggedMinutes" to task.loggedMinutes,
                "reminderMinutesBefore" to task.reminderMinutesBefore,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (task.description != null) updates["description"] = task.description
            if (task.dueTime != null) updates["dueTime"] = task.dueTime

            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "tasks/${task.id}")
            Result.failure(e)
        }
    }

    suspend fun deleteTask(taskId: String): Result<Unit> {
        return try {
            db.collection("tasks").document(taskId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "tasks/$taskId")
            Result.failure(e)
        }
    }

    // --- Schedules ---

    fun observeSchedules(): Flow<List<ScheduleBlock>> = flow {
        val uid = requireUserId()
        val path = "schedules"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot -> snapshot.toObjects(ScheduleBlock::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createSchedule(schedule: ScheduleBlock): Result<String> {
        return try {
            val uid = requireUserId()
            val scheduleId = if (schedule.id.isNotBlank()) schedule.id else "sched_" + UUID.randomUUID().toString().replace("-", "").take(16)
            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "title" to schedule.title,
                "category" to schedule.category,
                "dayOfWeek" to schedule.dayOfWeek,
                "startTime" to schedule.startTime,
                "endTime" to schedule.endTime,
                "isAutoAdjustable" to schedule.isAutoAdjustable,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!schedule.locationName.isNullOrBlank()) payload["locationName"] = schedule.locationName
            if (schedule.latitude != null) payload["latitude"] = schedule.latitude
            if (schedule.longitude != null) payload["longitude"] = schedule.longitude
            if (!schedule.groupCode.isNullOrBlank()) payload["groupCode"] = schedule.groupCode

            db.collection("schedules").document(scheduleId).set(payload).await()
            Result.success(scheduleId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "schedules")
            Result.failure(e)
        }
    }

    suspend fun updateSchedule(schedule: ScheduleBlock): Result<Unit> {
        return try {
            val docRef = db.collection("schedules").document(schedule.id)
            val updates = mutableMapOf<String, Any>(
                "title" to schedule.title,
                "category" to schedule.category,
                "dayOfWeek" to schedule.dayOfWeek,
                "startTime" to schedule.startTime,
                "endTime" to schedule.endTime,
                "isAutoAdjustable" to schedule.isAutoAdjustable,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (schedule.locationName != null) updates["locationName"] = schedule.locationName
            if (schedule.latitude != null) updates["latitude"] = schedule.latitude
            if (schedule.longitude != null) updates["longitude"] = schedule.longitude
            if (schedule.groupCode != null) updates["groupCode"] = schedule.groupCode

            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "schedules/${schedule.id}")
            Result.failure(e)
        }
    }

    suspend fun deleteSchedule(scheduleId: String): Result<Unit> {
        return try {
            db.collection("schedules").document(scheduleId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "schedules/$scheduleId")
            Result.failure(e)
        }
    }

    // --- Study Sessions ---

    fun observeStudySessions(): Flow<List<StudySession>> = flow {
        val uid = requireUserId()
        val path = "study_sessions"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot -> snapshot.toObjects(StudySession::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun logStudySession(
        subject: String,
        taskId: String? = null,
        durationMinutes: Int,
        qualityRating: Int = 5,
        notes: String? = null
    ): Result<String> {
        return try {
            val uid = requireUserId()
            val sessionId = "session_" + UUID.randomUUID().toString().replace("-", "").take(16)
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "subject" to subject,
                "durationMinutes" to durationMinutes,
                "dateStr" to dateStr,
                "qualityRating" to qualityRating,
                "createdAt" to FieldValue.serverTimestamp()
            )
            if (!taskId.isNullOrBlank()) payload["taskId"] = taskId
            if (!notes.isNullOrBlank()) payload["notes"] = notes

            db.collection("study_sessions").document(sessionId).set(payload).await()

            // If linked to a task, increment logged minutes
            if (!taskId.isNullOrBlank()) {
                val taskDoc = db.collection("tasks").document(taskId)
                taskDoc.update("loggedMinutes", FieldValue.increment(durationMinutes.toLong())).await()
            }

            Result.success(sessionId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "study_sessions")
            Result.failure(e)
        }
    }

    // --- User Profile & Encrypted Backup ---

    fun observeUserProfile(): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { it.toObject(UserProfile::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                    throw error
                }
        )
    }

    suspend fun saveUserProfile(displayName: String, email: String?, targetMinutes: Int, darkMode: Boolean): Result<Unit> {
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                val payload = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "displayName" to displayName,
                    "targetDailyStudyMinutes" to targetMinutes,
                    "darkModeEnabled" to darkMode,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!email.isNullOrBlank()) payload["email"] = email
                docRef.set(payload).await()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "displayName" to displayName,
                    "targetDailyStudyMinutes" to targetMinutes,
                    "darkModeEnabled" to darkMode,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!email.isNullOrBlank()) updates["email"] = email
                docRef.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}")
            Result.failure(e)
        }
    }

    suspend fun backupDataEncrypted(
        passphrase: String,
        currentSchedules: List<ScheduleBlock>,
        currentTasks: List<TaskItem>
    ): Result<String> {
        return try {
            val uid = requireUserId()
            val rootJson = JSONObject().apply {
                val schedulesArr = JSONArray()
                currentSchedules.forEach { s ->
                    schedulesArr.put(JSONObject().apply {
                        put("title", s.title)
                        put("category", s.category)
                        put("dayOfWeek", s.dayOfWeek)
                        put("startTime", s.startTime)
                        put("endTime", s.endTime)
                        put("locationName", s.locationName ?: "")
                        put("isAutoAdjustable", s.isAutoAdjustable)
                    })
                }
                val tasksArr = JSONArray()
                currentTasks.forEach { t ->
                    tasksArr.put(JSONObject().apply {
                        put("title", t.title)
                        put("subject", t.subject)
                        put("description", t.description ?: "")
                        put("dueDate", t.dueDate)
                        put("dueTime", t.dueTime ?: "")
                        put("priority", t.priority)
                        put("status", t.status)
                        put("estimatedMinutes", t.estimatedMinutes)
                        put("loggedMinutes", t.loggedMinutes)
                    })
                }
                put("schedules", schedulesArr)
                put("tasks", tasksArr)
                put("exportedAt", System.currentTimeMillis())
            }

            val encryptedPayload = CryptoUtils.encrypt(rootJson.toString(), passphrase)
            val docRef = db.collection("users").document(uid)

            val updates = mapOf(
                "encryptedBackupPayload" to encryptedPayload,
                "backupTimestamp" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(updates).await()
            Result.success(encryptedPayload)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/${auth.currentUser?.uid}")
            Result.failure(e)
        }
    }

    suspend fun restoreDataFromEncryptedBackup(passphrase: String): Result<Pair<Int, Int>> {
        return try {
            val uid = requireUserId()
            val doc = db.collection("users").document(uid).get().await()
            val payload = doc.getString("encryptedBackupPayload")
                ?: return Result.failure(IllegalStateException("Belum ada data cadangan di cloud"))

            val decryptedJson = CryptoUtils.decrypt(payload, passphrase)
            val json = JSONObject(decryptedJson)
            val schedulesArr = json.optJSONArray("schedules") ?: JSONArray()
            val tasksArr = json.optJSONArray("tasks") ?: JSONArray()

            var restoredSchedules = 0
            for (i in 0 until schedulesArr.length()) {
                val obj = schedulesArr.getJSONObject(i)
                val block = ScheduleBlock(
                    title = obj.getString("title"),
                    category = obj.optString("category", ScheduleCategory.STUDY.name),
                    dayOfWeek = obj.optInt("dayOfWeek", 1),
                    startTime = obj.optString("startTime", "08:00"),
                    endTime = obj.optString("endTime", "09:30"),
                    locationName = if (obj.has("locationName")) obj.getString("locationName") else null,
                    isAutoAdjustable = obj.optBoolean("isAutoAdjustable", true)
                )
                createSchedule(block)
                restoredSchedules++
            }

            var restoredTasks = 0
            for (i in 0 until tasksArr.length()) {
                val obj = tasksArr.getJSONObject(i)
                createTask(
                    title = obj.getString("title"),
                    subject = obj.optString("subject", "Umum"),
                    description = if (obj.has("description")) obj.getString("description") else null,
                    dueDate = obj.optString("dueDate", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())),
                    dueTime = if (obj.has("dueTime")) obj.getString("dueTime") else null,
                    priority = obj.optString("priority", "MEDIUM"),
                    estimatedMinutes = obj.optInt("estimatedMinutes", 60)
                )
                restoredTasks++
            }

            Result.success(Pair(restoredSchedules, restoredTasks))
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "users/${auth.currentUser?.uid}")
            Result.failure(e)
        }
    }

    // --- Group Schedule Sharing ---

    fun observeGroupSchedules(): Flow<List<GroupSchedule>> = flow {
        val uid = requireUserId()
        val path = "group_schedules"
        emitAll(
            db.collection(path)
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snapshot -> snapshot.toObjects(GroupSchedule::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createGroupSchedule(name: String, description: String? = null): Result<String> {
        return try {
            val uid = requireUserId()
            val groupId = "grp_" + UUID.randomUUID().toString().replace("-", "").take(12)
            // Generate clean 6-digit share code
            val code = (100000..999999).random().toString()
            val payload = mutableMapOf<String, Any>(
                "groupCode" to code,
                "name" to name,
                "ownerId" to uid,
                "memberUids" to listOf(uid),
                "createdAt" to FieldValue.serverTimestamp()
            )
            if (!description.isNullOrBlank()) payload["description"] = description

            db.collection("group_schedules").document(groupId).set(payload).await()
            Result.success(code)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "group_schedules")
            Result.failure(e)
        }
    }

    suspend fun joinGroupByCode(code: String): Result<String> {
        return try {
            val uid = requireUserId()
            val query = db.collection("group_schedules")
                .whereEqualTo("groupCode", code.trim())
                .get()
                .await()

            if (query.isEmpty) {
                return Result.failure(IllegalArgumentException("Kode grup tidak ditemukan"))
            }

            val doc = query.documents.first()
            val groupId = doc.id
            val members = doc.get("memberUids") as? List<*> ?: emptyList<Any>()
            if (!members.contains(uid)) {
                db.collection("group_schedules").document(groupId)
                    .update("memberUids", FieldValue.arrayUnion(uid))
                    .await()
            }
            Result.success(doc.getString("name") ?: "Grup Belajar")
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "group_schedules")
            Result.failure(e)
        }
    }

    // --- Smart Auto-Rescheduler (Penyesuaian Jadwal Otomatis) ---

    suspend fun autoAdjustSchedulesForUrgentTask(urgentTask: TaskItem, allSchedules: List<ScheduleBlock>): Result<Int> {
        return try {
            // Find current flexible/leisure blocks to convert or optimize into study blocks for urgent task
            var adjustedCount = 0
            val flexibleBlocks = allSchedules.filter {
                it.isAutoAdjustable && (it.category == ScheduleCategory.LEISURE.name || it.category == ScheduleCategory.BREAK.name)
            }

            for (block in flexibleBlocks.take(2)) {
                val updated = block.copy(
                    title = "Fokus: ${urgentTask.title} (${urgentTask.subject})",
                    category = ScheduleCategory.STUDY.name
                )
                updateSchedule(updated)
                adjustedCount++
            }

            Result.success(adjustedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
