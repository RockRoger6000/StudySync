package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import androidx.core.content.FileProvider
import com.example.data.model.ScheduleBlock
import com.example.data.model.TaskItem
import java.io.File
import java.util.Calendar

object CalendarExporter {

    fun addToGoogleCalendar(context: Context, schedule: ScheduleBlock) {
        val cal = Calendar.getInstance()
        // Map dayOfWeek (1=Mon ... 7=Sun)
        val parts = schedule.startTime.split(":")
        val startHour = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val startMin = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val endParts = schedule.endTime.split(":")
        val endHour = endParts.getOrNull(0)?.toIntOrNull() ?: 9
        val endMin = endParts.getOrNull(1)?.toIntOrNull() ?: 30

        cal.set(Calendar.HOUR_OF_DAY, startHour)
        cal.set(Calendar.MINUTE, startMin)
        cal.set(Calendar.SECOND, 0)
        val startMillis = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, endHour)
        cal.set(Calendar.MINUTE, endMin)
        val endMillis = cal.timeInMillis

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, schedule.title)
            putExtra(CalendarContract.Events.EVENT_LOCATION, schedule.locationName ?: "Kampus")
            putExtra(CalendarContract.Events.DESCRIPTION, "Kategori: ${schedule.category} (StudySync)")
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to general intent
        }
    }

    fun exportToExcelCsv(context: Context, schedules: List<ScheduleBlock>, tasks: List<TaskItem>): Boolean {
        return try {
            val sb = StringBuilder()
            sb.append("TIPE,JUDUL,KATEGORI / MATA KULIAH,HARI / DEADLINE,WAKTU,LOKASI / STATUS,CATATAN\n")

            schedules.forEach { s ->
                val dayName = when (s.dayOfWeek) {
                    1 -> "Senin"
                    2 -> "Selasa"
                    3 -> "Rabu"
                    4 -> "Kamis"
                    5 -> "Jumat"
                    6 -> "Sabtu"
                    else -> "Minggu"
                }
                sb.append("Jadwal,\"${s.title}\",\"${s.category}\",\"$dayName\",\"${s.startTime} - ${s.endTime}\",\"${s.locationName ?: "-"}\",\"AutoAdjust: ${s.isAutoAdjustable}\"\n")
            }

            tasks.forEach { t ->
                sb.append("Tugas,\"${t.title}\",\"${t.subject}\",\"${t.dueDate}\",\"${t.dueTime ?: "-"}\",\"${t.status}\",\"Prioritas: ${t.priority}, Durasi: ${t.loggedMinutes}/${t.estimatedMinutes}m\"\n")
            }

            val file = File(context.cacheDir, "jadwal_studysync_export.csv")
            file.writeText(sb.toString())

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "Ekspor Jadwal & Tugas StudySync (Excel CSV)")
                putExtra(Intent.EXTRA_TEXT, "Berikut ekspor data jadwal harian dan tugas siswa/mahasiswa dalam format Excel CSV.")
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Bagikan ke Excel / Spreadsheet").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            true
        } catch (e: Exception) {
            // If FileProvider not configured in XML, fall back to plain text share
            sharePlainText(context, schedules, tasks)
            true
        }
    }

    fun sharePrintablePdfText(context: Context, schedules: List<ScheduleBlock>, tasks: List<TaskItem>) {
        sharePlainText(context, schedules, tasks)
    }

    private fun sharePlainText(context: Context, schedules: List<ScheduleBlock>, tasks: List<TaskItem>) {
        val sb = StringBuilder()
        sb.append("=========================================\n")
        sb.append("       STUDYSYNC - JADWAL & TUGAS       \n")
        sb.append("=========================================\n\n")

        sb.append("--- JADWAL HARIAN TERORGANISIR ---\n")
        val days = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")
        for (i in 1..7) {
            val daySchedules = schedules.filter { it.dayOfWeek == i }
            if (daySchedules.isNotEmpty()) {
                sb.append("[${days[i - 1]}]\n")
                daySchedules.forEach { s ->
                    sb.append(" • ${s.startTime}-${s.endTime} | ${s.title} (${s.category})")
                    if (!s.locationName.isNullOrBlank()) sb.append(" @ ${s.locationName}")
                    sb.append("\n")
                }
                sb.append("\n")
            }
        }

        sb.append("--- DAFTAR TUGAS & DEADLINE ---\n")
        tasks.forEach { t ->
            sb.append(" • [${t.status}] ${t.title} - ${t.subject} (Deadline: ${t.dueDate} ${t.dueTime ?: ""})\n")
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Laporan Jadwal dan Tugas Belajar StudySync")
            putExtra(Intent.EXTRA_TEXT, sb.toString())
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Bagikan Laporan Jadwal (PDF/Teks)").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
