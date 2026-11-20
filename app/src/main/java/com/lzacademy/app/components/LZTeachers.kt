package com.lzacademy.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lzacademy.app.data.supabase.SupabaseApi
import com.lzacademy.app.models.Teacher
import com.lzacademy.app.theme.LZ
import com.lzacademy.app.theme.LZPalette
import com.lzacademy.app.theme.LZShapes
import com.lzacademy.app.theme.LZSize

// ------------------------------------------------------------------ avatar

private val teacherBrushes: List<List<Color>> = listOf(
    listOf(LZPalette.Turquoise, LZPalette.PrimaryGreen),
    listOf(LZPalette.Violet, LZPalette.DeepPurple),
    listOf(LZPalette.Info, Color(0xFF2B5FC7)),
    listOf(LZPalette.Gold, LZPalette.Orange),
    listOf(Color(0xFF2AB5A7), Color(0xFF0C6E62)),
)

/** Deterministic gradient per teacher, so an avatar always keeps the same colour. */
@Composable
fun teacherAvatarBrush(teacherId: String): Brush {
    val index = (teacherId.hashCode().toLong() and 0x7FFFFFFF).mod(teacherBrushes.size.toLong()).toInt()
    return Brush.linearGradient(teacherBrushes[index])
}

/**
 * Public URL of a teacher photo, or null when the row has none.
 *
 * `teachers.photo_url` stores `<bucket>/<object>` (`photos/teachers/<uuid>.jpg?v=…`),
 * the same convention as `students.avatar_path`; a value that is already an absolute
 * URL (set by another client) is passed through untouched.
 */
internal fun teacherPhotoUrl(teacher: Teacher, api: SupabaseApi = SupabaseApi()): String? =
    teacher.photoUrl
        ?.trim()
        ?.takeIf { it.isNotEmpty() && it != "null" }
        ?.let { if (it.startsWith("http://") || it.startsWith("https://")) it else api.avatarUrl(it) }

/** Avatar of a teacher: the real photo when the row has one, his initials otherwise. */
@Composable
fun TeacherAvatar(
    teacher: Teacher,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    ring: Boolean = false,
) {
    LZAvatar(
        initials = teacher.initials,
        modifier = modifier,
        size = size,
        ring = ring,
        background = teacherAvatarBrush(teacher.id),
        photo = rememberRemoteAvatar(teacherPhotoUrl(teacher)),
    )
}

// ------------------------------------------------------------------ availability

/** Green "Disponible" / neutral grey "Hors ligne" badge. Colour is never the only signal. */
@Composable
fun TeacherAvailabilityBadge(available: Boolean, modifier: Modifier = Modifier) {
    val c = LZ.colors
    if (available) {
        LZStatusPill("Disponible", c.success, c.successSoft, modifier, dot = true)
    } else {
        LZStatusPill("Hors ligne", c.textSecondary, c.surfaceSoft, modifier, dot = true)
    }
}

// ------------------------------------------------------------------ showcase

/** Poster-style featured card of the student-home carousel: big face, no clutter. */
@Composable
fun TeacherShowcaseCard(
    teacher: Teacher,
    onOpenProfile: () -> Unit,
    onContact: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LZ.colors
    val available = teacher.acceptsStudentMessages
    val photo = rememberRemoteAvatar(teacherPhotoUrl(teacher))
    LZCard(
        modifier = modifier.width(178.dp).height(262.dp),
        onClick = onOpenProfile,
        contentPadding = PaddingValues(0.dp),
        elevation = 6.dp,
    ) {
        Column(Modifier.fillMaxWidth()) {
            // Poster art: big face on a soft lavender backdrop (transparent PNGs read as intentional)
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(c.surfaceSoft),
            ) {
                if (photo != null) {
                    androidx.compose.foundation.Image(
                        painter = photo,
                        contentDescription = teacher.fullName,
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .padding(top = 10.dp),
                    )
                } else {
                    LZAvatar(
                        initials = teacher.initials,
                        size = 88.dp,
                        ring = true,
                        background = teacherAvatarBrush(teacher.id),
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                // Availability: small quiet dot-pill, top-end overlay
                Row(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(c.surface.copy(alpha = 0.92f), LZShapes.pill)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(
                        Modifier.size(7.dp).background(
                            if (available) c.success else c.textSecondary,
                            CircleShape,
                        ),
                    )
                    LZText(
                        if (available) "Dispo" else "Absent",
                        style = LZ.type.labelSmall,
                        color = if (available) c.success else c.textSecondary,
                    )
                }
                // Contact: round icon button, bottom-end overlay (poster stays text-free)
                if (available) {
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                            .size(40.dp)
                            .background(c.primary, CircleShape)
                            .clickable(onClick = onContact),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.Chat,
                            contentDescription = "Contacter ${teacher.fullName}",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
            // Title block: subject as the "credit line", name as the marquee
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(c.surface)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                LZText(
                    teacher.primarySubject.uppercase(),
                    style = LZ.type.labelSmall,
                    color = c.primary,
                    maxLines = 1,
                )
                LZText(
                    teacher.fullName,
                    style = LZ.type.bodyStrong,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * "Nos enseignants" section of the student home: title, subtitle, "Voir tous" link and the
 * horizontal carousel. It never renders the whole directory.
 */
@Composable
fun TeacherShowcaseSection(
    teachers: List<Teacher>,
    onSeeAll: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onContact: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (teachers.isEmpty()) return
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LZSectionHeader(
            title = "Nos enseignants",
            actionLabel = "Voir tous",
            onAction = onSeeAll,
            modifier = Modifier.padding(horizontal = LZSize.ScreenPadding),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 0.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(teachers, key = { it.id }) { teacher ->
                TeacherShowcaseCard(
                    teacher = teacher,
                    onOpenProfile = { onOpenProfile(teacher.id) },
                    onContact = { onContact(teacher.id) },
                )
            }
        }
    }
}

// ------------------------------------------------------------------ directory row

/** Full-width teacher row of the directory and of the admin overview. */
@Composable
fun TeacherDirectoryRow(
    teacher: Teacher,
    onOpenProfile: () -> Unit,
    onContact: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val c = LZ.colors
    val available = teacher.acceptsStudentMessages
    LZCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onOpenProfile,
        contentPadding = PaddingValues(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TeacherAvatar(teacher, size = 46.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                LZText(teacher.fullName, style = LZ.type.bodyStrong, maxLines = 1)
                LZText(teacher.subjectLabel, style = LZ.type.secondary, color = c.textSecondary, maxLines = 1)
                TeacherAvailabilityBadge(available)
            }
            if (trailing != null) {
                trailing()
            } else {
                TeacherContactIconButton(teacher, onContact)
            }
        }
    }
}

/** 44dp contact affordance of the directory (disabled when the teacher is offline). */
@Composable
fun TeacherContactIconButton(
    teacher: Teacher,
    onContact: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LZ.colors
    val available = teacher.acceptsStudentMessages
    val shape = CircleShape
    Box(
        modifier = modifier
            .size(44.dp)
            .alpha(if (available) 1f else 0.4f)
            .background(if (available) c.surfaceSoft else c.border, shape)
            .pressable(shape = shape, enabled = available, onClick = onContact),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.AutoMirrored.Rounded.Chat,
            contentDescription = if (available) {
                "Contacter ${teacher.fullName}"
            } else {
                "${teacher.fullName} n'est pas disponible pour recevoir des messages"
            },
            tint = if (available) c.primary else c.textSecondary,
            modifier = Modifier.size(20.dp),
        )
    }
}

// The labelled ON/OFF switch lives in LZSettings.kt (LZSettingsSwitchRow) with the rest of the
// settings building blocks.
