package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class StudySession(
    @DocumentId val id: String = "",
    val userId: String = "",
    val subject: String = "",
    val taskId: String? = null,
    val durationMinutes: Int = 25,
    val dateStr: String = "", // YYYY-MM-DD
    val qualityRating: Int = 5, // 1 to 5
    val notes: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)
