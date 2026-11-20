package com.lzacademy.app.ui.student

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.lzacademy.app.components.LZCard
import com.lzacademy.app.components.LZEmptyState
import com.lzacademy.app.components.LZPrimaryButton
import com.lzacademy.app.components.LZSectionHeader
import com.lzacademy.app.components.LZStatusPill
import com.lzacademy.app.components.LZText
import com.lzacademy.app.components.LZTopBar
import com.lzacademy.app.data.PlanningLevelUi
import com.lzacademy.app.data.PlanningLevels
import com.lzacademy.app.theme.LZ
import com.lzacademy.app.theme.LZSize
import com.lzacademy.app.theme.LZStatusBarIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate

/**
 * Weekly planning viewed as a real offline PDF: the document is generated with
 * [PdfDocument] into app storage, displayed with [PdfRenderer] and exported to the
 * public "Téléchargements" folder with MediaStore when the student taps download.
 */
@Composable
fun StudentPlanningPdfScreen(
    levelId: String,
    onNavigateBack: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val levels = remember { PlanningLevels.all(LocalDate.now()) }
    val level = remember(levelId, levels) {
        levels.firstOrNull { it.id == levelId } ?: levels.first()
    }

    var downloading by remember { mutableStateOf(false) }
    val pdfState by produceState<PdfPagesState?>(initialValue = null, level.id) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val file = PlanningPdfFile.generate(context, level)
                val pages = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        (0 until renderer.pageCount).map {
                            renderer.openPage(it).use { page ->
                                Bitmap.createBitmap(
                                    page.width,
                                    page.height,
                                    Bitmap.Config.ARGB_8888,
                                ).also { page.render(it, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY) }
                                    .asImageBitmap()
                            }
                        }
                    }
                }
                PdfPagesState(file, pages)
            }.getOrElse { PdfPagesState(null, emptyList()) }
        }
    }

    val pages = pdfState?.pages.orEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LZ.colors.background)
            .testTag("student_planning_pdf_screen"),
    ) {
        LZStatusBarIcons(lightIcons = false)
        LZTopBar(
            title = "Planning PDF • ${level.chipLabel}",
            onBack = onNavigateBack,
        )

        Column(
            Modifier.padding(horizontal = LZSize.ScreenPadding).padding(top = 10.dp, bottom = 2.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LZText(level.sectionLabel, style = LZ.type.cardTitle)
                LZStatusPill("${level.totalSessions} séances / semaine", LZ.colors.primary, LZ.colors.surfaceSoft)
            }
            LZText(
                pdfState?.file?.name ?: "${PlanningPdfFile.fileNameOf(level)} • préparation...",
                style = LZ.type.secondary,
                color = LZ.colors.textSecondary,
            )
        }
        Spacer(Modifier.height(12.dp))

        when {
            pdfState == null -> {
                Column(
                    Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator(color = LZ.colors.primary)
                    Spacer(Modifier.height(10.dp))
                    LZText("Génération du PDF...", style = LZ.type.secondary, color = LZ.colors.textSecondary)
                }
            }

            pages.isEmpty() -> {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    LZEmptyState(
                        title = "Impossible d'afficher le PDF",
                        message = "La génération du planning a échoué. Fermez cet écran puis réessayez.",
                        icon = Icons.Rounded.PictureAsPdf,
                        modifier = Modifier.padding(horizontal = LZSize.ScreenPadding),
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = LZSize.ScreenPadding, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    itemsIndexed(pages) { index, pageBitmap ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Image(
                                bitmap = pageBitmap,
                                contentDescription = "Page ${index + 1} du planning ${level.chipLabel}",
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(210f / 297f)
                                    .border(1.dp, LZ.colors.border),
                            )
                            LZText(
                                "Page ${index + 1} sur ${pages.size}",
                                style = LZ.type.label,
                                color = LZ.colors.textSecondary,
                            )
                        }
                    }
                }
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = LZSize.ScreenPadding, vertical = 12.dp)
                .testTag("download_planning_pdf"),
        ) {
            LZPrimaryButton(
                text = when {
                    pdfState == null -> "Génération du PDF..."
                    downloading -> "Téléchargement..."
                    else -> "Télécharger le PDF"
                },
                enabled = !downloading && pages.isNotEmpty(),
                loading = downloading,
                leadingIcon = if (downloading) null else Icons.Rounded.Download,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val file = pdfState?.file
                    if (file == null) {
                        onShowMessage("Le PDF n'est pas encore prêt, patientez une seconde.")
                    } else {
                        downloading = true
                        scope.launch {
                            val ok = withContext(Dispatchers.IO) {
                                PlanningPdfFile.downloadToDownloads(context, file)
                            }
                            downloading = false
                            onShowMessage(
                                if (ok) "PDF téléchargé dans le dossier « Téléchargements » du téléphone"
                                else "Échec du téléchargement du PDF",
                            )
                        }
                    }
                },
            )
        }
    }
}

/** Generated file + its rendered pages. */
private data class PdfPagesState(val file: File?, val pages: List<ImageBitmap>)

// ------------------------------------------------------------------ PDF engine

/** Pure Android-graphics helper: builds the planning PDF outside the Compose world. */
object PlanningPdfFile {

    // Brand palette as raw ints so nothing collides with the Compose Color type here.
    private val brandGreen = 0xFF07966F.toInt()
    private val darkGreen = 0xFF056B54.toInt()
    private val rowSoft = 0xFFEAF7F2.toInt()
    private val borderGrey = 0xFFCCD9D3.toInt()
    private val textDark = 0xFF182521.toInt()
    private val textGrey = 0xFF77847F.toInt()

    private const val PAGE_W = 595 // A4 72dpi
    private const val PAGE_H = 842

    fun fileNameOf(level: PlanningLevelUi): String =
        "Planning_${level.id}_Semaine_${level.days.firstOrNull()?.number ?: 1}.pdf"

    fun generate(context: Context, level: PlanningLevelUi): File {
        val dir = File(context.filesDir, "plannings")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, fileNameOf(level))

        val document = PdfDocument()
        val page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create())
        val canvas = page.canvas
        val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 0.8f }
        val today = LocalDate.now()

        // Header band
        canvas.drawColor(Color.WHITE)
        fill.color = brandGreen
        canvas.drawRect(0f, 0f, PAGE_W.toFloat(), 92f, fill)
        fill.color = darkGreen
        canvas.drawRect(0f, 78f, PAGE_W.toFloat(), 92f, fill)
        canvas.drawText("LZ Academy - Chéraga", 40f, 36f, paintBold(17f, 0xFFFFFFFF.toInt()))
        canvas.drawText("Planning des cours - ${level.sectionLabel}", 40f, 60f, paintRegular(12f, 0xFFDFF5F1.toInt()))
        canvas.drawText(
            "Semaine du ${level.days.firstOrNull()?.number ?: ""} ${monthShort(today)} - Annee academique 2025/2026",
            40f, 79f, paintRegular(9.5f, 0xFFDFF5F1.toInt()),
        )

        // Table header
        var y = 128f
        val left = 40f
        val right = (PAGE_W - 40).toFloat()
        fill.color = darkGreen
        canvas.drawRoundRect(left, y, right, y + 26f, 6f, 6f, fill)
        val colDay = left + 12f
        val colSubject = colDay + 86f
        val colTime = colSubject + 200f
        val colRoom = colTime + 108f
        canvas.drawText("Jour", colDay, y + 17f, paintBold(10f, 0xFFFFFFFF.toInt()))
        canvas.drawText("Matière", colSubject, y + 17f, paintBold(10f, 0xFFFFFFFF.toInt()))
        canvas.drawText("Horaire", colTime, y + 17f, paintBold(10f, 0xFFFFFFFF.toInt()))
        canvas.drawText("Salle", colRoom, y + 17f, paintBold(10f, 0xFFFFFFFF.toInt()))

        // Table rows
        y += 26f
        var rowIndex = 0
        for (day in level.days) {
            val lessons = level.lessonsByDay[day.index].orEmpty()
            for (lesson in lessons) {
                if (rowIndex % 2 == 0) {
                    fill.color = rowSoft
                    canvas.drawRect(left, y, right, y + 26f, fill)
                }
                stroke.color = borderGrey
                canvas.drawLine(left, y, right, y, stroke)
                canvas.drawText(dayLong(day.index), colDay, y + 18f, paintRegular(10f, textGrey))
                canvas.drawText(lesson.subject, colSubject, y + 18f, paintBold(10f, textDark))
                canvas.drawText(lesson.timeRange, colTime, y + 18f, paintRegular(10f, textDark))
                canvas.drawText(lesson.room, colRoom, y + 18f, paintRegular(10f, textDark))
                y += 26f
                rowIndex++
            }
        }
        stroke.color = borderGrey
        canvas.drawLine(left, y, right, y, stroke)

        // Footer
        y += 42f
        canvas.drawText(
            "Les horaires peuvent évoluer en cours de semaine : consultez les actualités de l'académie.",
            left, y, paintRegular(9f, textGrey),
        )
        canvas.drawText(
            "Généré le ${today.dayOfMonth} ${monthShort(today)} ${today.year} • App LZ Academy (hors ligne)",
            left, y + 16f, paintRegular(9f, textGrey),
        )

        document.finishPage(page)
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    /** Copies the PDF into the public Downloads collection (API 29+, no permission needed). */
    fun downloadToDownloads(context: Context, file: File): Boolean = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver
                .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return@runCatching false
            context.contentResolver.openOutputStream(uri)?.use { out ->
                file.inputStream().use { it.copyTo(out) }
            } ?: return@runCatching false
            true
        } else {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return@runCatching false
            file.copyTo(File(dir, file.name), overwrite = true)
            true
        }
    }.getOrDefault(false)

    private fun dayLong(index: Int): String =
        listOf("Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche")[index.coerceIn(0, 6)]

    private fun monthShort(today: LocalDate): String =
        listOf("Jan", "Fév", "Mar", "Avr", "Mai", "Juin", "Juil", "Août", "Sep", "Oct", "Nov", "Déc")[today.monthValue - 1]

    private fun paintBold(size: Float, color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        isFakeBoldText = true
        this.color = color
    }

    private fun paintRegular(size: Float, color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
    }
}
