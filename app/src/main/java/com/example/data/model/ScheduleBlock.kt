package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class ScheduleCategory(val displayName: String) {
    STUDY("Belajar & Kuliah"),
    BREAK("Istirahat"),
    MEAL("Makan"),
    EXERCISE("Olahraga"),
    LEISURE("Waktu Senggang")
}

@IgnoreExtraProperties
data class ScheduleBlock(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val category: String = ScheduleCategory.STUDY.name,
    val dayOfWeek: Int = 1, // 1 = Senin, 7 = Minggu
    val startTime: String = "08:00", // HH:mm
    val endTime: String = "09:30", // HH:mm
    val locationName: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val groupCode: String? = null,
    val isAutoAdjustable: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
