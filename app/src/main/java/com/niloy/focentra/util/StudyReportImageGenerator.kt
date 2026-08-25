package com.niloy.focentra.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.content.FileProvider
import com.niloy.focentra.data.repository.StreakInfo
import com.niloy.focentra.engine.PeriodStatistics
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StudyReportImageGenerator {

    fun generateAndShareReportImage(
        context: Context,
        periodName: String,
        stats: PeriodStatistics,
        streak: StreakInfo
    ) {
        val width = 1080
        val height = 1440
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background - Dark AMOLED Gradient / Charcoal
        val bgPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#090A10")
            isAntiAlias = true
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Top Accent Decorative Glow
        val glowPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#131522")
            isAntiAlias = true
        }
        val topRect = RectF(40f, 40f, width - 40f, 220f)
        canvas.drawRoundRect(topRect, 36f, 36f, glowPaint)

        // Header Title
        val brandPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#818CF8") // Vibrant Indigo
            textSize = 34f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText("FOCENTRA • STUDY REPORT", 80f, 110f, brandPaint)

        val dateDf = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
        val dateText = dateDf.format(Date())
        val subDatePaint = Paint().apply {
            color = android.graphics.Color.parseColor("#94A3B8")
            textSize = 28f
            isAntiAlias = true
        }
        canvas.drawText(dateText, 80f, 165f, subDatePaint)

        // Main Focus Summary Card
        val cardPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#181B2E")
            isAntiAlias = true
        }
        val heroRect = RectF(40f, 250f, width - 40f, 620f)
        canvas.drawRoundRect(heroRect, 44f, 44f, cardPaint)

        val heroLabelPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#94A3B8")
            textSize = 32f
            isAntiAlias = true
        }
        canvas.drawText("$periodName Total Focus Time", 80f, 320f, heroLabelPaint)

        val totalHrs = stats.totalFocusedSeconds / 3600
        val totalMins = (stats.totalFocusedSeconds % 3600) / 60
        val heroTimePaint = Paint().apply {
            color = android.graphics.Color.parseColor("#FFFFFF")
            textSize = 96f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText("${totalHrs}h ${totalMins}m", 80f, 430f, heroTimePaint)

        // Mini Metrics row inside hero card
        val linePaint = Paint().apply {
            color = android.graphics.Color.parseColor("#2E3456")
            strokeWidth = 2f
            isAntiAlias = true
        }
        canvas.drawLine(80f, 480f, width - 80f, 480f, linePaint)

        val metricValPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#38BDF8")
            textSize = 42f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val metricLblPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#94A3B8")
            textSize = 26f
            isAntiAlias = true
        }

        // Sessions Count
        canvas.drawText("${stats.sessionCount}", 80f, 540f, metricValPaint)
        canvas.drawText("Sessions", 80f, 580f, metricLblPaint)

        // Focus Score
        canvas.drawText("${stats.focusScore}/100", 380f, 540f, metricValPaint)
        canvas.drawText("Focus Score", 380f, 580f, metricLblPaint)

        // Current Streak
        canvas.drawText("${streak.currentStreak} Days", 700f, 540f, metricValPaint)
        canvas.drawText("Daily Streak", 700f, 580f, metricLblPaint)

        // 4 Detail Grid Cards
        val gridY = 650f
        val colWidth = (width - 110f) / 2f

        // Card 1: Top Subject
        drawStatBox(
            canvas = canvas,
            left = 40f,
            top = gridY,
            right = 40f + colWidth,
            bottom = gridY + 200f,
            title = "TOP SUBJECT",
            value = if (stats.mostStudiedSubject.isBlank() || stats.mostStudiedSubject == "-") "General Study" else stats.mostStudiedSubject,
            accentColor = "#A78BFA"
        )

        // Card 2: Peak Concentration
        drawStatBox(
            canvas = canvas,
            left = 60f + colWidth,
            top = gridY,
            right = width - 40f,
            bottom = gridY + 200f,
            title = "PEAK FOCUS HOUR",
            value = if (stats.mostProductiveHour.isBlank() || stats.mostProductiveHour == "-") "Evening" else stats.mostProductiveHour,
            accentColor = "#34D399"
        )

        // Card 3: Avg Session Duration
        drawStatBox(
            canvas = canvas,
            left = 40f,
            top = gridY + 230f,
            right = 40f + colWidth,
            bottom = gridY + 430f,
            title = "AVG SESSION",
            value = "${stats.averageSessionMinutes} min",
            accentColor = "#FBBF24"
        )

        // Card 4: Longest Streak
        drawStatBox(
            canvas = canvas,
            left = 60f + colWidth,
            top = gridY + 230f,
            right = width - 40f,
            bottom = gridY + 430f,
            title = "BEST STREAK",
            value = "${streak.longestStreak} Days",
            accentColor = "#F472B6"
        )

        // Footer Branding
        val footerPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#64748B")
            textSize = 28f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Built with Discipline • Focentra Offline Study OS", width / 2f, 1360f, footerPaint)

        // Save bitmap to cache directory
        try {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "focentra_study_report_${System.currentTimeMillis()}.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
            stream.close()

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_TEXT, "📊 My Study Performance Report on Focentra!\n\nFocused: ${totalHrs}h ${totalMins}m | Streak: ${streak.currentStreak} Days 🔥")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Share Study Report (PNG)")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun drawStatBox(
        canvas: Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        title: String,
        value: String,
        accentColor: String
    ) {
        val bg = Paint().apply {
            color = android.graphics.Color.parseColor("#131522")
            isAntiAlias = true
        }
        canvas.drawRoundRect(RectF(left, top, right, bottom), 32f, 32f, bg)

        val titleP = Paint().apply {
            color = android.graphics.Color.parseColor(accentColor)
            textSize = 24f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText(title, left + 32f, top + 60f, titleP)

        val valP = Paint().apply {
            color = android.graphics.Color.parseColor("#FFFFFF")
            textSize = 40f
            isFakeBoldText = true
            isAntiAlias = true
        }
        // Truncate long value if needed
        val displayVal = if (value.length > 14) value.take(12) + "…" else value
        canvas.drawText(displayVal, left + 32f, top + 130f, valP)
    }
}
