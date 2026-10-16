package com.example.data.model

data class DailyProgressSummary(
    val dateStr: String,
    val totalStudyMinutes: Int,
    val targetStudyMinutes: Int,
    val completedTasksCount: Int,
    val pendingTasksCount: Int,
    val sessionsCount: Int,
    val studyRatio: Float,
    val balanceScore: Int, // 0 - 100
    val feedbackMessage: String
)
