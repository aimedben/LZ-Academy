package com.lzacademy.app.ui.student

import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import com.lzacademy.app.data.FrenchDates
import com.lzacademy.app.models.LessonUi
import com.lzacademy.app.models.StudentDashboardData
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Génère le planning complet de la semaine en PDF (A4 paysage) :
 * bandeau titre, une colonne par jour, une carte colorée par séance, pied de page.
 */
object PlanningPdf {

    private const val PAGE_W = 842  // A4 paysage en points
    private const val PAGE_H = 595
    private const val MARGIN = 28f
    private const val FILE_NAME = "planning_semaine.pdf"

    private val PURPLE = Color.parseColor("#534AB7")
    private val PURPLE_SOFT = Color.parseColor("#EEEDFE")
    private val INK = Color.parseColor("#1F1D3A")
    private val MUTED = Color.parseColor("#6B6A80")
    private val LINE = Color.parseColor("#E2E0F0")

    /** [colorOf] renvoie la couleur ARGB de l'accent d'une séance (résolue côté UI). */
    fun create(
        context: Context,
        data: StudentDashboardData,
        colorOf: (LessonUi) -> Int,
    ): File {
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create())
        draw(page.canvas, data, colorOf)
        doc.finishPage(page)

        val dir = File(context.cacheDir, "planning").apply { mkdirs() }
        val file = File(dir, FILE_NAME)
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    private fun draw(c: Canvas, data: StudentDashboardData, colorOf: (LessonUi) -> Int) {
        val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        val text = TextPaint(Paint.ANTI_ALIAS_FLAG)

        // Fond
        c.drawColor(Color.WHITE)

        // ---- Bandeau titre ----
        val headerH = 78f
        fill.color = PURPLE
        c.drawRect(0f, 0f, PAGE_W.toFloat(), headerH, fill)

        // pastille initiales
        fill.color = Color.parseColor("#AFA9EC")
        c.drawCircle(MARGIN + 21f, headerH / 2, 21f, fill)
        text.apply { color = Color.parseColor("#26215C"); textSize = 15f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER }
        c.drawText(data.initials, MARGIN + 21f, headerH / 2 + 5.5f, text)

        text.apply { color = Color.WHITE; textSize = 22f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.LEFT }
        c.drawText("Planning de la semaine", MARGIN + 56f, 37f, text)

        val first = data.week.firstOrNull()
        val last = data.week.lastOrNull()
        val range = if (first != null && last != null) "Semaine du ${first.number} au ${last.number}" else "Semaine en cours"
        text.apply { color = Color.parseColor("#CECBF6"); textSize = 12f; typeface = Typeface.DEFAULT }
        c.drawText(range, MARGIN + 56f, 57f, text)

        // total à droite
        val total = data.lessonsByDay.values.sumOf { it.size }
        text.apply { color = Color.WHITE; textSize = 12f; textAlign = Paint.Align.RIGHT; typeface = Typeface.DEFAULT_BOLD }
        c.drawText("$total séance(s) cette semaine", PAGE_W - MARGIN, 43f, text)
        text.textAlign = Paint.Align.LEFT

        // ---- Colonnes ----
        val days = data.week
        val n = days.size.coerceAtLeast(1)
        val gap = 8f
        val colW = (PAGE_W - 2 * MARGIN - gap * (n - 1)) / n
        val top = headerH + 18f
        val dayHeadH = 40f
        val bottom = PAGE_H - 44f
        val cardsTop = top + dayHeadH + 8f
        val cardsH = bottom - cardsTop
        val maxCount = days.maxOfOrNull { data.lessonsByDay[it.index].orEmpty().size }?.coerceAtLeast(1) ?: 1
        val cardGap = 6f
        val cardH = ((cardsH - cardGap * (maxCount - 1)) / maxCount).coerceIn(40f, 84f)

        days.forEachIndexed { i, day ->
            val x = MARGIN + i * (colW + gap)
            val isToday = day.index == data.todayIndex

            // fond de colonne
            fill.color = Color.parseColor("#F7F6FD")
            c.drawRoundRect(RectF(x, top, x + colW, bottom), 10f, 10f, fill)

            // en-tête du jour
            fill.color = if (isToday) PURPLE else PURPLE_SOFT
            c.drawRoundRect(RectF(x, top, x + colW, top + dayHeadH), 10f, 10f, fill)
            text.apply {
                color = if (isToday) Color.WHITE else INK
                textSize = 12f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER
            }
            val label = FrenchDates.dayLong.getOrElse(day.index) { day.label }
            c.drawText(fit(label, text, colW - 8f), x + colW / 2, top + 17f, text)
            text.apply { textSize = 11f; typeface = Typeface.DEFAULT; color = if (isToday) Color.parseColor("#CECBF6") else MUTED }
            c.drawText("${day.number}", x + colW / 2, top + 32f, text)
            text.textAlign = Paint.Align.LEFT

            // séances
            val lessons = data.lessonsByDay[day.index].orEmpty()
            if (lessons.isEmpty()) {
                text.apply { color = MUTED; textSize = 11f; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT }
                c.drawText("Libre", x + colW / 2, cardsTop + 22f, text)
                text.textAlign = Paint.Align.LEFT
            }
            lessons.forEachIndexed { j, lesson ->
                val y = cardsTop + j * (cardH + cardGap)
                val accent = colorOf(lesson)
                val r = RectF(x + 5f, y, x + colW - 5f, y + cardH)

                fill.color = Color.WHITE
                c.drawRoundRect(r, 8f, 8f, fill)
                fill.style = Paint.Style.STROKE; fill.strokeWidth = 0.8f; fill.color = LINE
                c.drawRoundRect(r, 8f, 8f, fill)
                fill.style = Paint.Style.FILL

                // barre d'accent
                fill.color = accent
                c.drawRoundRect(RectF(r.left, r.top, r.left + 5f, r.bottom), 3f, 3f, fill)

                val tx = r.left + 12f
                val tw = r.width() - 18f
                var ty = r.top + 15f
                text.apply { color = INK; textSize = 11f; typeface = Typeface.DEFAULT_BOLD }
                c.drawText(fit(lesson.subject, text, tw), tx, ty, text)
                text.apply { color = accent; textSize = 9.5f; typeface = Typeface.DEFAULT_BOLD }
                ty += 13f
                c.drawText(fit(lesson.timeRange, text, tw), tx, ty, text)
                text.apply { color = MUTED; textSize = 9f; typeface = Typeface.DEFAULT }
                if (cardH >= 52f) { ty += 12f; c.drawText(fit(lesson.room, text, tw), tx, ty, text) }
                if (cardH >= 66f) { ty += 11f; c.drawText(fit(lesson.person, text, tw), tx, ty, text) }
            }
        }

        // ---- Pied de page ----
        fill.color = LINE
        c.drawRect(MARGIN, PAGE_H - 30f, PAGE_W - MARGIN, PAGE_H - 29.4f, fill)
        val now = SimpleDateFormat("d MMMM yyyy 'à' HH:mm", Locale.FRENCH).format(Date())
        text.apply { color = MUTED; textSize = 9f; typeface = Typeface.DEFAULT }
        c.drawText("LZ Academy • Planning généré le $now", MARGIN, PAGE_H - 14f, text)
    }

    private fun fit(s: String, p: TextPaint, width: Float): String =
        TextUtils.ellipsize(s, p, width, TextUtils.TruncateAt.END).toString()

    // ------------------------------------------------------------------ actions

    private fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    /** Ouvre le PDF dans le lecteur du téléphone ; à défaut, propose le partage. */
    fun open(context: Context, file: File) {
        val uri = uriFor(context, file)
        val view = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(view)
        } catch (e: ActivityNotFoundException) {
            share(context, file)
        }
    }

    fun share(context: Context, file: File) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uriFor(context, file))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(send, "Partager le planning").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    /** Copie le PDF dans le dossier Téléchargements. Renvoie true si réussi. */
    fun saveToDownloads(context: Context, file: File): Boolean = try {
        val name = "planning_semaine_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.FRENCH).format(Date())}.pdf"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri == null) false else {
                context.contentResolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
                true
            }
        } else {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            if (dir == null) false else {
                file.copyTo(File(dir, name), overwrite = true)
                true
            }
        }
    } catch (e: Exception) {
        false
    }
}
