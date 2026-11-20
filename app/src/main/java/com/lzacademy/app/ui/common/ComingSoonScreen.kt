package com.lzacademy.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lzacademy.app.components.LZEmptyState
import com.lzacademy.app.components.LZTopBar
import com.lzacademy.app.theme.LZ
import com.lzacademy.app.theme.LZStatusBarIcons
import com.lzacademy.app.theme.LZTheme

/**
 * Honest placeholder for tabs that belong to a later delivery (planning, courses, groups...).
 * Delivery 1 only ships the three dashboards; every other tab lands here so navigation never dead-ends.
 */
@Composable
fun ComingSoonScreen(
    title: String,
    delivery: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.Construction,
    onBackHome: (() -> Unit)? = null,
) {
    Column(modifier.fillMaxSize().background(LZ.colors.background)) {
        LZStatusBarIcons(lightIcons = false)
        LZTopBar(title = title)
        Box(Modifier.fillMaxSize().padding(bottom = 48.dp), contentAlignment = Alignment.Center) {
            LZEmptyState(
                icon = icon,
                title = "$title arrive bientôt",
                message = "Cet écran fait partie de la $delivery. Le design system et les tableaux de bord sont déjà prêts.",
                actionLabel = if (onBackHome != null) "Retour à l'accueil" else null,
                onAction = { onBackHome?.invoke() },
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 800)
@Composable
private fun ComingSoonPreview() {
    LZTheme(darkTheme = false) { ComingSoonScreen("Planning", "livraison Élèves", onBackHome = {}) }
}
