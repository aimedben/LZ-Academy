package com.lzacademy.app.ui.admin

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.lzacademy.app.components.LZCard
import com.lzacademy.app.components.LZDivider
import com.lzacademy.app.components.LZGradientHeader
import com.lzacademy.app.components.LZGreetingRow
import com.lzacademy.app.components.LZPrimaryButton
import com.lzacademy.app.components.LZSecondaryButton
import com.lzacademy.app.components.LZStatusPill
import com.lzacademy.app.components.LZText
import com.lzacademy.app.theme.LZ
import com.lzacademy.app.theme.LZSize

@Composable
fun AdminMoreScreen(
    onNavigateBack: () -> Unit,
    onShowMessage: (String) -> Unit,
    onAvatarClick: () -> Unit,
    onOpenSubscriptions: () -> Unit = {},
    onOpenPacks: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LZ.colors.background)
            .testTag("admin_more_screen"),
    ) {
        // Admin gradient header
        LZGradientHeader(brush = LZ.colors.adminHeaderBrush) {
            LZGreetingRow(
                initials = "LZ",
                title = "Paramètres & Système",
                subtitle = "Configuration Académie • Administration",
                onAvatarClick = onAvatarClick,
                photo = rememberAdminLocalPhoto(),
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Academy details card
            item {
                LZCard(modifier = Modifier.padding(horizontal = LZSize.ScreenPadding).fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Icon(Icons.Rounded.Business, null, tint = LZ.colors.primary, modifier = Modifier.size(36.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                LZText("LZ Academy Chéraga", style = LZ.type.cardTitle)
                                Icon(Icons.Rounded.Verified, "Agrément officiel", tint = LZ.colors.primary, modifier = Modifier.size(18.dp))
                            }
                            LZText("Établissement privé d'enseignement & de soutien", style = LZ.type.secondary, color = LZ.colors.textSecondary)
                            LZStatusPill("Agrément N° 2024/09-DZ", LZ.colors.success, LZ.colors.successSoft)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    LZDivider()
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        LZText("Année académique : 2025 / 2026", style = LZ.type.body)
                        LZText("Adresse : 14 Boulevard Mohamed V, Chéraga, Alger", style = LZ.type.secondary, color = LZ.colors.textSecondary)
                        LZText("Contact administration : +213 23 45 67 89", style = LZ.type.secondary, color = LZ.colors.textSecondary)
                    }
                }
            }

            // System Security & Backup
            item {
                LZCard(modifier = Modifier.padding(horizontal = LZSize.ScreenPadding).fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LZText("Sauvegardes & Intégrité", style = LZ.type.cardTitle)
                        Icon(Icons.Rounded.CloudDone, null, tint = LZ.colors.success, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    LZText("Dernière sauvegarde cloud automatique effectuée aujourd'hui à 03:00.", style = LZ.type.secondary, color = LZ.colors.textSecondary)
                    Spacer(Modifier.height(14.dp))
                    LZSecondaryButton(
                        text = "Lancer une sauvegarde manuelle",
                        onClick = { onShowMessage("Sauvegarde chiffrée de la base de données terminée avec succès !") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = Icons.Rounded.Backup,
                    )
                }
            }

            // Access Control & Roles
            item {
                LZCard(modifier = Modifier.padding(horizontal = LZSize.ScreenPadding).fillMaxWidth()) {
                    LZText("Contrôle d'accès & Sécurité", style = LZ.type.cardTitle)
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Rounded.AdminPanelSettings, null, tint = LZ.colors.primary, modifier = Modifier.size(20.dp))
                                LZText("Comptes administrateurs (3)", style = LZ.type.body)
                            }
                            LZStatusPill("Actifs", LZ.colors.primary, LZ.colors.surfaceSoft)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Rounded.Lock, null, tint = LZ.colors.textSecondary, modifier = Modifier.size(20.dp))
                                LZText("Authentification à deux facteurs", style = LZ.type.body)
                            }
                            LZStatusPill("Obligatoire", LZ.colors.success, LZ.colors.successSoft)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Rounded.History, null, tint = LZ.colors.textSecondary, modifier = Modifier.size(20.dp))
                                LZText("Journaux d'audit", style = LZ.type.body)
                            }
                            LZStatusPill("Enregistré", LZ.colors.textSecondary, LZ.colors.surfaceSoft)
                        }
                    }
                }
            }

            // Packs & tarifs — moved out of Gestion
            item {
                LZCard(modifier = Modifier.padding(horizontal = LZSize.ScreenPadding).fillMaxWidth()) {
                    LZText("Packs & tarifs", style = LZ.type.cardTitle)
                    Spacer(Modifier.height(6.dp))
                    LZText(
                        "Formules, prix et séances : créez et ajustez ce qui est vendu à l'accueil de l'académie.",
                        style = LZ.type.secondary,
                        color = LZ.colors.textSecondary,
                    )
                    Spacer(Modifier.height(12.dp))
                    LZPrimaryButton(
                        text = "Gérer les packs",
                        onClick = onOpenPacks,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = Icons.Rounded.Sell,
                    )
                }
            }

            // Session renewal — administration only
            item {
                LZCard(modifier = Modifier.padding(horizontal = LZSize.ScreenPadding).fillMaxWidth()) {
                    LZText("Abonnements & séances", style = LZ.type.cardTitle)
                    Spacer(Modifier.height(6.dp))
                    LZText(
                        "L'administration seule crédite les séances : les élèves consultent leur compteur, sans jamais renouveler depuis leur espace.",
                        style = LZ.type.secondary,
                        color = LZ.colors.textSecondary,
                    )
                    Spacer(Modifier.height(12.dp))
                    LZPrimaryButton(
                        text = "Renouveler les abonnements",
                        onClick = onOpenSubscriptions,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = Icons.Rounded.Autorenew,
                    )
                }
            }

            // Account & preferences
            item {
                LZCard(modifier = Modifier.padding(horizontal = LZSize.ScreenPadding).fillMaxWidth()) {
                    LZText("Mon compte", style = LZ.type.cardTitle)
                    Spacer(Modifier.height(8.dp))
                    LZText(
                        "Profil de l'administrateur : photo et informations personnelles, stockées sur l'appareil.",
                        style = LZ.type.secondary,
                        color = LZ.colors.textSecondary,
                    )
                    Spacer(Modifier.height(12.dp))
                    LZPrimaryButton(
                        text = "Profil admin",
                        onClick = onOpenProfile,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = Icons.Rounded.Person,
                    )
                    Spacer(Modifier.height(8.dp))
                    LZSecondaryButton(
                        text = "Ouvrir mon compte",
                        onClick = onAvatarClick,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
