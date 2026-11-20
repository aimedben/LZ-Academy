package com.lzacademy.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lzacademy.app.components.LZLogo
import com.lzacademy.app.components.LZPasswordInput
import com.lzacademy.app.components.LZPrimaryButton
import com.lzacademy.app.components.LZText
import com.lzacademy.app.components.LZTextInput
import com.lzacademy.app.components.pressable
import com.lzacademy.app.data.auth.AuthSeed
import com.lzacademy.app.models.UserRole
import com.lzacademy.app.theme.LZ
import com.lzacademy.app.theme.LZShapes
import com.lzacademy.app.viewmodel.AuthViewModel

/**
 * Local sign-in screen. Each role has its own account, so the experience behind the screen is
 * chosen by the authenticated identity, never by a debug switch.
 */
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.loginState.collectAsStateWithLifecycle()
    val c = LZ.colors
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(c.background)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        LZLogo(size = 76.dp)
        Spacer(Modifier.height(18.dp))
        LZText("LZ Academy", style = LZ.type.pageTitle, color = c.textPrimary)
        Spacer(Modifier.height(6.dp))
        LZText(
            "Connectez-vous à votre espace",
            style = LZ.type.secondary,
            color = c.textSecondary,
        )
        Spacer(Modifier.height(28.dp))

        Column(
            Modifier
                .fillMaxWidth()
                .background(c.surface, LZShapes.standard)
                .border(1.dp, c.border, LZShapes.standard)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            LZText("Connexion", style = LZ.type.cardTitle)
            LZTextInput(
                value = email,
                onValueChange = {
                    email = it
                    viewModel.clearError()
                },
                placeholder = "Adresse e-mail",
                leadingIcon = Icons.Rounded.Email,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                modifier = Modifier.fillMaxWidth(),
            )
            LZPasswordInput(
                value = password,
                onValueChange = {
                    password = it
                    viewModel.clearError()
                },
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.error != null) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(c.errorSoft, LZShapes.small)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Lock, null, tint = c.error, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    LZText(state.error.orEmpty(), style = LZ.type.secondary, color = c.error)
                }
            }
            LZPrimaryButton(
                text = "Se connecter",
                onClick = { viewModel.login(email, password) },
                modifier = Modifier.fillMaxWidth(),
                enabled = email.isNotBlank() && password.isNotBlank(),
                loading = state.loading,
            )
        }

        Spacer(Modifier.height(24.dp))
        LZText(
            "Comptes de l'académie",
            style = LZ.type.bodyStrong,
            color = c.textSecondary,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .background(c.surface, LZShapes.standard)
                .border(1.dp, c.border, LZShapes.standard)
                .padding(vertical = 6.dp),
        ) {
            DemoAccountRow(
                icon = Icons.Rounded.Person,
                role = "Élève",
                email = AuthSeed.STUDENT_EMAIL,
                onClick = {
                    email = AuthSeed.STUDENT_EMAIL
                    password = AuthSeed.DEMO_PASSWORD
                    viewModel.clearError()
                },
            )
            DemoAccountRow(
                icon = Icons.Rounded.School,
                role = "Enseignant",
                email = AuthSeed.TEACHER_EMAIL,
                onClick = {
                    email = AuthSeed.TEACHER_EMAIL
                    password = AuthSeed.DEMO_PASSWORD
                    viewModel.clearError()
                },
            )
            DemoAccountRow(
                icon = Icons.Rounded.AdminPanelSettings,
                role = "Administrateur",
                email = AuthSeed.ADMIN_EMAIL,
                onClick = {
                    email = AuthSeed.ADMIN_EMAIL
                    password = AuthSeed.DEMO_PASSWORD
                    viewModel.clearError()
                },
            )
        }
        Spacer(Modifier.height(10.dp))
        LZText(
            "Mot de passe commun : ${AuthSeed.DEMO_PASSWORD} — appuyez sur un compte pour préremplir.",
            style = LZ.type.label,
            color = c.textSecondary,
        )
        Spacer(Modifier.height(20.dp))
        LZText(
            "Données locales : aucun serveur n'est contacté dans cette version.",
            style = LZ.type.label,
            color = c.textSecondary,
        )
    }
}

@Composable
private fun DemoAccountRow(
    icon: ImageVector,
    role: String,
    email: String,
    onClick: () -> Unit,
) {
    val c = LZ.colors
    Row(
        Modifier
            .fillMaxWidth()
            .pressable(LZShapes.small, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(38.dp)
                .background(c.surfaceSoft, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = c.primary, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            LZText(role, style = LZ.type.bodyStrong)
            LZText(email, style = LZ.type.label, color = c.textSecondary)
        }
    }
}

