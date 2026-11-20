package com.lzacademy.app.ui.admin

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lzacademy.app.components.LZAvatar
import com.lzacademy.app.components.LZCard
import com.lzacademy.app.components.LZDivider
import com.lzacademy.app.components.LZGradientHeader
import com.lzacademy.app.components.LZGreetingRow
import com.lzacademy.app.components.LZHeaderIconButton
import com.lzacademy.app.components.LZPrimaryButton
import com.lzacademy.app.components.LZSecondaryButton
import com.lzacademy.app.components.LZStatusPill
import com.lzacademy.app.components.LZText
import com.lzacademy.app.components.LZTextInput
import com.lzacademy.app.components.rememberLocalPhotoPainter
import com.lzacademy.app.data.preferences.AppPreferences
import com.lzacademy.app.theme.LZ
import com.lzacademy.app.theme.LZPalette
import com.lzacademy.app.theme.LZSize

/**
 * Profil admin — 100 % local (SharedPreferences + fichier photo dans filesDir).
 * Aucune donnée n'est envoyée à Supabase, par choix produit.
 *
 * La page est d'abord une page de profil en lecture seule ; « Modifier mes informations »
 * bascule la même page en formulaire d'édition (rien n'est inventé : un champ vide affiche
 * honnêtement « non renseigné »).
 */

/** Photo de profil admin (locale) pour les headers — null tant qu'aucune photo n'est choisie. */
@Composable
internal fun rememberAdminLocalPhoto(): Painter? {
    val context = androidx.compose.ui.platform.LocalContext.current
    val path = AppPreferences(context).adminPhotoPath
    return rememberLocalPhotoPainter(path)
}

@Composable
fun AdminProfileScreen(
    onShowMessage: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAcademy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }

    var editing by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(prefs.adminName.orEmpty()) }
    var title by remember { mutableStateOf(prefs.adminTitle.orEmpty()) }
    var phone by remember { mutableStateOf(prefs.adminPhone.orEmpty()) }
    var email by remember { mutableStateOf(prefs.adminEmail.orEmpty()) }
    var photoPath by remember { mutableStateOf(prefs.adminPhotoPath) }
    var photoVersion by remember { mutableStateOf(0) } // force re-decode after replace

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            val path = saveProfilePhoto(context, it)
            if (path != null) {
                prefs.adminPhotoPath = path
                photoPath = path
                photoVersion++
                onShowMessage("Photo de profil mise à jour.")
            } else {
                onShowMessage("Impossible de lire cette image.")
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LZ.colors.background)
            .verticalScroll(rememberScrollState()),
    ) {
        key(photoVersion) {
            LZGradientHeader(brush = LZ.colors.adminHeaderBrush) {
                LZGreetingRow(
                    initials = profileInitials(name.ifBlank { "Admin" }),
                    title = "Mon profil",
                    subtitle = title.ifBlank { "Espace administrateur" },
                    onAvatarClick = onOpenSettings,
                    photo = rememberLocalPhotoPainter(photoPath),
                ) {
                    LZHeaderIconButton(
                        icon = Icons.Rounded.Settings,
                        contentDescription = "Paramètres",
                        onClick = onOpenSettings,
                    )
                }
            }
        }

        Column(
            Modifier.padding(horizontal = LZSize.ScreenPadding).padding(top = 22.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (editing) {
                AdminIdentityForm(
                    name = name,
                    onNameChange = { name = it },
                    title = title,
                    onTitleChange = { title = it },
                    phone = phone,
                    onPhoneChange = { phone = it },
                    email = email,
                    onEmailChange = { email = it },
                    photoPath = photoPath,
                    photoVersion = photoVersion,
                    onPickPhoto = {
                        pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onSave = {
                        prefs.adminName = name.trim()
                        prefs.adminTitle = title.trim()
                        prefs.adminPhone = phone.trim()
                        prefs.adminEmail = email.trim()
                        onShowMessage("Profil admin enregistré.")
                        editing = false
                    },
                    onCancel = {
                        // Discard the draft and fall back to what is actually stored.
                        name = prefs.adminName.orEmpty()
                        title = prefs.adminTitle.orEmpty()
                        phone = prefs.adminPhone.orEmpty()
                        email = prefs.adminEmail.orEmpty()
                        editing = false
                    },
                )
            } else {
                AdminIdentityCard(
                    name = name,
                    title = title,
                    phone = phone,
                    email = email,
                    photoPath = photoPath,
                    photoVersion = photoVersion,
                    onEdit = { editing = true },
                )
            }

            // Settings — the one control surface: appearance, notifications, account.
            LZCard(modifier = Modifier.fillMaxWidth()) {
                LZText("Paramètres", style = LZ.type.cardTitle)
                Spacer(Modifier.height(6.dp))
                LZText(
                    "Apparence, notifications et compte connecté.",
                    style = LZ.type.secondary,
                    color = LZ.colors.textSecondary,
                )
                Spacer(Modifier.height(12.dp))
                LZSecondaryButton(
                    text = "Ouvrir les paramètres",
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = Icons.Rounded.Settings,
                )
            }

            // Academy • the rest of the admin tools, one tap away at the bottom of the profile.
            LZCard(modifier = Modifier.fillMaxWidth()) {
                LZText("Academy", style = LZ.type.cardTitle)
                Spacer(Modifier.height(6.dp))
                LZText(
                    "Packs, abonnements, sauvegardes et informations de l'établissement.",
                    style = LZ.type.secondary,
                    color = LZ.colors.textSecondary,
                )
                Spacer(Modifier.height(12.dp))
                LZSecondaryButton(
                    text = "Academy",
                    onClick = onOpenAcademy,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = Icons.Rounded.School,
                )
            }
        }
    }
}

/** Read-only identity card: real stored values, honest empty state, and the edit entry point. */
@Composable
private fun AdminIdentityCard(
    name: String,
    title: String,
    phone: String,
    email: String,
    photoPath: String?,
    photoVersion: Int,
    onEdit: () -> Unit,
) {
    LZCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            key(photoVersion) {
                ProfileAvatar(photoPath, initials = profileInitials(name.ifBlank { "Admin" }), size = 64.dp)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LZText(
                    name.ifBlank { "Aucune donnée — nom non renseigné" },
                    style = LZ.type.cardTitle,
                )
                LZStatusPill("Administrateur", LZ.colors.primary, LZ.colors.surfaceSoft)
            }
        }
        Spacer(Modifier.height(14.dp))
        LZDivider()
        Spacer(Modifier.height(6.dp))
        IdentityRow(Icons.Rounded.Badge, "Fonction", title.ifBlank { "Aucune donnée — non renseignée" })
        IdentityRow(Icons.Rounded.Phone, "Téléphone", phone.ifBlank { "Aucune donnée — non renseigné" })
        IdentityRow(Icons.Rounded.Email, "E-mail", email.ifBlank { "Aucune donnée — non renseigné" })
        Spacer(Modifier.height(6.dp))
        LZText(
            "Ces informations restent sur cet appareil : elles ne sont jamais envoyées au serveur de l'académie.",
            style = LZ.type.labelSmall,
            color = LZ.colors.textSecondary,
        )
        Spacer(Modifier.height(12.dp))
        LZSecondaryButton(
            text = "Modifier mes informations",
            onClick = onEdit,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = Icons.Rounded.Edit,
        )
    }
}

/** One read-only fact of the identity card. */
@Composable
private fun IdentityRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = LZ.colors.primary, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            LZText(label, style = LZ.type.label, color = LZ.colors.textSecondary)
            LZText(value, style = LZ.type.bodyStrong, maxLines = 2)
        }
    }
}

/** The existing edit form, unchanged in substance: photo + four local fields. */
@Composable
private fun AdminIdentityForm(
    name: String,
    onNameChange: (String) -> Unit,
    title: String,
    onTitleChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    photoPath: String?,
    photoVersion: Int,
    onPickPhoto: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        key(photoVersion) {
            ProfileAvatar(photoPath, initials = profileInitials(name.ifBlank { "Admin" }), size = 104.dp)
        }
        LZSecondaryButton(
            text = if (photoPath == null) "Ajouter une photo" else "Changer la photo",
            onClick = onPickPhoto,
            leadingIcon = Icons.Rounded.Edit,
        )
    }

    Spacer(Modifier.height(4.dp))

    LZTextInput(value = name, onValueChange = onNameChange, placeholder = "Nom complet", leadingIcon = Icons.Rounded.Person)
    LZTextInput(value = title, onValueChange = onTitleChange, placeholder = "Fonction (ex. Directeur)", leadingIcon = Icons.Rounded.Badge)
    LZTextInput(value = phone, onValueChange = onPhoneChange, placeholder = "Téléphone", leadingIcon = Icons.Rounded.Phone)
    LZTextInput(value = email, onValueChange = onEmailChange, placeholder = "Email", leadingIcon = Icons.Rounded.Email)

    LZText(
        "Ces informations restent sur cet appareil : elles ne sont jamais envoyées au serveur de l'académie.",
        style = LZ.type.labelSmall,
        color = LZ.colors.textSecondary,
    )

    LZPrimaryButton(
        text = "Enregistrer",
        onClick = onSave,
        modifier = Modifier.fillMaxWidth(),
    )
    LZSecondaryButton(
        text = "Annuler",
        onClick = onCancel,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ProfileAvatar(photoPath: String?, initials: String, size: androidx.compose.ui.unit.Dp) {
    val bitmap = remember(photoPath) {
        photoPath?.let { path ->
            runCatching { BitmapFactory.decodeFile(path) }.getOrNull()
        }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Photo de profil",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .border(3.dp, LZ.colors.surface, CircleShape),
        )
    } else {
        LZAvatar(
            initials = initials,
            size = size,
            ring = true,
            background = Brush.linearGradient(listOf(LZPalette.PrimaryGreen, LZPalette.DarkGreen)),
        )
    }
}

private fun saveProfilePhoto(context: android.content.Context, uri: Uri): String? = runCatching {
    val source = context.contentResolver.openInputStream(uri) ?: return null
    val decoded = BitmapFactory.decodeStream(source)
    source.close()
    val scaled = com.lzacademy.app.ui.admin.scaleForProfile(decoded)
    val file = java.io.File(context.filesDir, "admin_profile.jpg")
    java.io.FileOutputStream(file).use { out ->
        scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 88, out)
    }
    file.absolutePath
}.getOrNull()

private fun profileInitials(fullName: String): String =
    fullName.split(" ").mapNotNull { it.firstOrNull()?.uppercaseChar() }.take(2).joinToString("")

private fun scaleForProfile(source: android.graphics.Bitmap): android.graphics.Bitmap {
    val maxSide = 1024
    val largest = maxOf(source.width, source.height)
    if (largest <= maxSide) return source
    val scale = maxSide.toFloat() / largest
    return android.graphics.Bitmap.createScaledBitmap(
        source,
        (source.width * scale).toInt().coerceAtLeast(1),
        (source.height * scale).toInt().coerceAtLeast(1),
        true,
    )
}
