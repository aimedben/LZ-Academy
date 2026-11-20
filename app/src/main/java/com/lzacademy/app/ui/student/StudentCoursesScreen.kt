package com.lzacademy.app.ui.student

import android.content.Intent
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.lzacademy.app.components.LZCard
import com.lzacademy.app.components.LZCourseCard
import com.lzacademy.app.components.LZEmptyState
import com.lzacademy.app.components.LZFilterChip
import com.lzacademy.app.components.LZGradientHeader
import com.lzacademy.app.components.LZGreetingRow
import com.lzacademy.app.components.LZLinearProgress
import com.lzacademy.app.components.LZPrimaryButton
import com.lzacademy.app.components.LZSearchBar
import com.lzacademy.app.components.LZSecondaryButton
import com.lzacademy.app.components.LZSectionHeader
import com.lzacademy.app.components.LZStatusPill
import com.lzacademy.app.components.LZText
import com.lzacademy.app.components.color
import com.lzacademy.app.models.LessonAccent
import com.lzacademy.app.theme.LZ
import com.lzacademy.app.theme.LZSize

data class CourseItem(
    val id: String,
    val subject: String,
    val teacher: String,
    val progress: Float,
    val chaptersCount: Int,
    val completedChapters: Int,
    val icon: ImageVector,
    val accent: LessonAccent,
    val category: String,
    val nextExam: String,
)

@Composable
fun StudentCoursesScreen(
    onNavigateBack: () -> Unit,
    onShowMessage: (String) -> Unit,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("Toutes") }
    var selectedCourse by remember { mutableStateOf<CourseItem?>(null) }

    // ---- Documents réels du téléphone (SAF, sans permission de stockage) ----
    val context = LocalContext.current
    val documentPrefs = remember { context.getSharedPreferences("lz_phone_documents", Context.MODE_PRIVATE) }
    var phoneDocUris by remember {
        mutableStateOf(documentPrefs.getStringSet("uris", emptySet()).orEmpty())
    }

    val pickDocuments = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                }
            }
            phoneDocUris = (phoneDocUris + uris.map(Uri::toString)).toSet()
            documentPrefs.edit().putStringSet("uris", phoneDocUris).apply()
            onShowMessage("${uris.size} document(s) ajouté(s) depuis le stockage du téléphone")
        }
    }

    val phoneDocuments = remember(phoneDocUris) {
        phoneDocUris.mapNotNull { raw ->
            runCatching { parsePhoneDocument(context, Uri.parse(raw), raw) }.getOrNull()
        }
    }

    fun openPhoneDocument(doc: PhoneDocument) {
        val openIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(doc.uri, doc.mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { context.startActivity(Intent.createChooser(openIntent, "Ouvrir avec")) }
            .onFailure { onShowMessage("Aucune application installée ne peut ouvrir « ${doc.name} »") }
    }

    fun removePhoneDocument(raw: String) {
        runCatching {
            context.contentResolver.releasePersistableUriPermission(
                Uri.parse(raw),
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        phoneDocUris = phoneDocUris - raw
        documentPrefs.edit().putStringSet("uris", phoneDocUris).apply()
        onShowMessage("Document retiré de la liste")
    }

    val allCourses = remember {
        listOf(
            CourseItem(
                id = "c1",
                subject = "Mathématiques 3AS",
                teacher = "M. Lebbah • 4h/semaine",
                progress = 0.85f,
                chaptersCount = 8,
                completedChapters = 7,
                icon = Icons.Rounded.Calculate,
                accent = LessonAccent.Green,
                category = "Scientifique",
                nextExam = "BAC Blanc - 18 Mai",
            ),
            CourseItem(
                id = "c2",
                subject = "Physique - Chimie",
                teacher = "M. Said • 3h/semaine",
                progress = 0.78f,
                chaptersCount = 10,
                completedChapters = 8,
                icon = Icons.Rounded.Science,
                accent = LessonAccent.Violet,
                category = "Scientifique",
                nextExam = "Interrogation - 12 Mai",
            ),
            CourseItem(
                id = "c3",
                subject = "Sciences de la Nature & Vie",
                teacher = "Mme Bensalem • 3h/semaine",
                progress = 0.90f,
                chaptersCount = 6,
                completedChapters = 5,
                icon = Icons.Rounded.MenuBook,
                accent = LessonAccent.Green,
                category = "Scientifique",
                nextExam = "Devoir surveillé - 22 Mai",
            ),
            CourseItem(
                id = "c4",
                subject = "Langue Française",
                teacher = "Mme Haddad • 2h/semaine",
                progress = 0.70f,
                chaptersCount = 5,
                completedChapters = 3,
                icon = Icons.Rounded.HistoryEdu,
                accent = LessonAccent.Orange,
                category = "Littéraire",
                nextExam = "Production écrite - 15 Mai",
            ),
            CourseItem(
                id = "c5",
                subject = "Langue Anglaise",
                teacher = "M. Touati • 2h/semaine",
                progress = 0.80f,
                chaptersCount = 6,
                completedChapters = 5,
                icon = Icons.Rounded.Language,
                accent = LessonAccent.Blue,
                category = "Littéraire",
                nextExam = "Oral & Vocabulaire - 19 Mai",
            ),
            CourseItem(
                id = "c6",
                subject = "Philosophie",
                teacher = "M. Amara • 2h/semaine",
                progress = 0.65f,
                chaptersCount = 4,
                completedChapters = 2,
                icon = Icons.Rounded.Psychology,
                accent = LessonAccent.Orange,
                category = "Littéraire",
                nextExam = "Dissertation - 25 Mai",
            ),
        )
    }

    val categories = listOf("Toutes", "Scientifique", "Littéraire")

    val filteredCourses = remember(searchQuery, selectedCategory) {
        allCourses.filter { course ->
            val matchesCategory = selectedCategory == "Toutes" || course.category == selectedCategory
            val matchesSearch = course.subject.contains(searchQuery, ignoreCase = true) ||
                course.teacher.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LZ.colors.background)
            .testTag("student_courses_screen"),
    ) {
        // Gradient Header
        LZGradientHeader(brush = LZ.colors.studentHeaderBrush) {
            LZGreetingRow(
                initials = "RB",
                title = "Mes Matières & Cours",
                subtitle = "Programme BAC 2026 • 3AS",
                onAvatarClick = onAvatarClick,
                photo = rememberOwnStudentPhoto(),
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Search Bar
            item {
                Box(Modifier.padding(horizontal = LZSize.ScreenPadding).padding(top = 16.dp)) {
                    LZSearchBar(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = "Rechercher une matière, un enseignant...",
                    )
                }
            }

            // Category filter chips
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = LZSize.ScreenPadding),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(categories) { category ->
                        LZFilterChip(
                            label = category,
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                        )
                    }
                }
            }

            // Global progress summary
            item {
                LZCard(
                    modifier = Modifier
                        .padding(horizontal = LZSize.ScreenPadding)
                        .fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            LZText("Progression globale", style = LZ.type.cardTitle)
                            LZText("6 matières inscrites • 30/39 chapitres achevés", style = LZ.type.secondary, color = LZ.colors.textSecondary)
                        }
                        LZStatusPill("En avance", LZ.colors.success, LZ.colors.successSoft, icon = Icons.Rounded.CheckCircle)
                    }
                    Spacer(Modifier.height(12.dp))
                    LZLinearProgress(progress = 0.78f, color = LZ.colors.primary)
                }
            }

            // Courses list
            if (filteredCourses.isEmpty()) {
                item {
                    LZEmptyState(
                        title = "Aucune matière trouvée",
                        message = "Essayez un autre mot-clé ou réinitialisez le filtre.",
                        icon = Icons.Rounded.MenuBook,
                        actionLabel = "Réinitialiser",
                        onAction = {
                            searchQuery = ""
                            selectedCategory = "Toutes"
                        },
                        modifier = Modifier.padding(horizontal = LZSize.ScreenPadding, vertical = 24.dp),
                    )
                }
            } else {
                item {
                    LZSectionHeader(
                        title = "Matières & Avancement",
                        actionLabel = "${filteredCourses.size} matières",
                        modifier = Modifier.padding(horizontal = LZSize.ScreenPadding),
                    )
                }
                items(filteredCourses) { course ->
                    Box(Modifier.padding(horizontal = LZSize.ScreenPadding)) {
                        LZCourseCard(
                            subject = course.subject,
                            teacher = course.teacher,
                            progress = course.progress,
                            icon = course.icon,
                            accent = course.accent,
                            onClick = { selectedCourse = course },
                        )
                    }
                }
            }

            // ---- Documents réels : stockage du téléphone (SAF, hors ligne) ----
            item {
                Column(Modifier.padding(horizontal = LZSize.ScreenPadding).testTag("phone_documents_section")) {
                    LZSectionHeader(
                        title = "Documents du téléphone",
                        actionLabel = if (phoneDocuments.isEmpty()) "Stockage" else "${phoneDocuments.size} fichier(s)",
                    )
                    Spacer(Modifier.height(10.dp))
                    LZSecondaryButton(
                        text = "Parcourir le stockage",
                        onClick = { pickDocuments.launch(arrayOf("*/*")) },
                        modifier = Modifier.fillMaxWidth().testTag("storage_browse_button"),
                        leadingIcon = Icons.Rounded.FolderOpen,
                    )
                    if (phoneDocuments.isEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        LZText(
                            "Vos vrais PDF, documents et cours enregistrés sur ce téléphone " +
                                "s'afficheront ici. Choisissez un ou plusieurs fichiers.",
                            style = LZ.type.secondary,
                            color = LZ.colors.textSecondary,
                        )
                    }
                }
            }
            items(phoneDocuments, key = { it.uri.toString() }) { doc ->
                val docIcon = when {
                    doc.isPdf -> Icons.Rounded.PictureAsPdf
                    doc.mime.startsWith("image/") -> Icons.Rounded.Image
                    else -> Icons.Rounded.Description
                }
                Box(Modifier.padding(horizontal = LZSize.ScreenPadding)) {
                    LZCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { openPhoneDocument(doc) },
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(docIcon, null, tint = LZ.colors.primary, modifier = Modifier.size(26.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                LZText(doc.name, style = LZ.type.body, maxLines = 1)
                                LZText(doc.sizeLabel, style = LZ.type.label, color = LZ.colors.textSecondary, maxLines = 1)
                            }
                            IconButton(onClick = { removePhoneDocument(doc.uri.toString()) }) {
                                Icon(Icons.Rounded.Close, "Retirer", tint = LZ.colors.textSecondary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Course Detail Sheet
    selectedCourse?.let { course ->
        Dialog(onDismissRequest = { selectedCourse = null }) {
            LZCard(modifier = Modifier.fillMaxWidth()) {
                LZText(course.subject, style = LZ.type.pageTitle)
                LZText("${course.completedChapters}/${course.chaptersCount} chapitres complétés • ${course.nextExam}", style = LZ.type.secondary, color = LZ.colors.textSecondary)
                Spacer(Modifier.height(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    LZText("Progression du programme", style = LZ.type.bodyStrong)
                    LZLinearProgress(progress = course.progress, color = course.accent.color())
                    Spacer(Modifier.height(4.dp))
                    LZText("Chapitres clés étudiés :", style = LZ.type.secondary, color = LZ.colors.textSecondary)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.CheckCircle, null, tint = LZ.colors.success, modifier = Modifier.size(16.dp))
                            LZText("Chapitre 1 : Notions fondamentales & Définitions", style = LZ.type.body)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.CheckCircle, null, tint = LZ.colors.success, modifier = Modifier.size(16.dp))
                            LZText("Chapitre 2 : Méthodes de résolution & Problèmes types", style = LZ.type.body)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.Description, null, tint = course.accent.color(), modifier = Modifier.size(16.dp))
                            LZText("Chapitre 3 : Entraînement intensif sur annales du BAC", style = LZ.type.body)
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                LZSecondaryButton("Télécharger le polycopié", {
                    onShowMessage("Polycopié de ${course.subject} téléchargé avec succès !")
                    selectedCourse = null
                }, Modifier.fillMaxWidth(), leadingIcon = Icons.Rounded.Download)
                Spacer(Modifier.height(8.dp))
                LZPrimaryButton("Fermer", { selectedCourse = null }, Modifier.fillMaxWidth())
            }
        }
    }
}

// --------------------------------------------------------- real phone documents helpers

/** A real file picked from the phone storage via the system file picker (SAF). */
private data class PhoneDocument(
    val uri: Uri,
    val name: String,
    val sizeLabel: String,
    val mime: String,
    val isPdf: Boolean,
)

/** Reads the display name / size / mime of a persisted document URI. */
private fun parsePhoneDocument(context: Context, uri: Uri, raw: String): PhoneDocument? {
    if (uri.toString() != raw) return null
    var name = raw.substringAfterLast('/')
    var sizeBytes = -1L
    runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.getString(nameIndex) != null) {
                    name = cursor.getString(nameIndex)
                }
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) sizeBytes = cursor.getLong(sizeIndex)
            }
        }
    }
    val mime = runCatching { context.contentResolver.getType(uri) }.getOrNull() ?: "application/octet-stream"
    return PhoneDocument(
        uri = uri,
        name = name,
        sizeLabel = formatFileSize(sizeBytes, mime),
        mime = mime,
        isPdf = mime == "application/pdf" || name.endsWith(".pdf", ignoreCase = true),
    )
}

private fun formatFileSize(bytes: Long, mime: String): String {
    val size = when {
        bytes <= 0 -> null
        bytes >= 1_048_576L -> String.format(java.util.Locale.FRANCE, "%.1f Mo", bytes / 1_048_576.0)
        else -> String.format(java.util.Locale.FRANCE, "%.1f Ko", bytes / 1024.0)
    }
    return listOfNotNull(size, mime).joinToString(" • ")
}
