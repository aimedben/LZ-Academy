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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lzacademy.app.components.LZAnalyticsCard
import com.lzacademy.app.components.LZBarChart
import com.lzacademy.app.components.LZCard
import com.lzacademy.app.components.LZChartCard
import com.lzacademy.app.components.LZCompactDropdown
import com.lzacademy.app.components.LZDashboardSkeleton
import com.lzacademy.app.components.LZDonutChart
import com.lzacademy.app.components.LZEmptyState
import com.lzacademy.app.components.LZGradientHeader
import com.lzacademy.app.components.LZHeaderIconButton
import com.lzacademy.app.components.LZKpiTile
import com.lzacademy.app.components.LZLineChart
import com.lzacademy.app.components.LZModernDialog
import com.lzacademy.app.components.LZPaymentCard
import com.lzacademy.app.components.LZSecondaryButton
import com.lzacademy.app.components.LZSectionHeader
import com.lzacademy.app.components.LZStatCard
import com.lzacademy.app.components.LZText
import com.lzacademy.app.components.LZAvatar
import com.lzacademy.app.components.color
import com.lzacademy.app.components.pressable
import com.lzacademy.app.components.softColor
import com.lzacademy.app.ui.previews.PreviewSamples
import com.lzacademy.app.models.AdminDashboardData
import com.lzacademy.app.models.AlertUi
import com.lzacademy.app.models.PaymentUi
import com.lzacademy.app.models.QuickActionUi
import com.lzacademy.app.navigation.LZRoutes
import com.lzacademy.app.theme.LZ
import com.lzacademy.app.theme.LZShapes
import com.lzacademy.app.theme.LZStatusBarIcons
import com.lzacademy.app.theme.LZTheme
import com.lzacademy.app.viewmodel.AdminDashboardViewModel
import com.lzacademy.app.viewmodel.UiState
import androidx.compose.foundation.layout.fillMaxHeight
import java.time.LocalDate

class AdminDashboardActions(
    val onAvatarClick: () -> Unit = {},
    val onBellClick: () -> Unit = {},
    val onHeaderStatClick: (String) -> Unit = {},
    val onQuickAction: (QuickActionUi) -> Unit = {},
    val onRevenueClick: () -> Unit = {},
    val onKpiClick: (String) -> Unit = {},
    val onAlertConfirmed: (AlertUi) -> Unit = {},
    val onSelectPeriod: (Int) -> Unit = {},
    val onSeeAllPayments: () -> Unit = {},
    val onPaymentClick: (PaymentUi) -> Unit = {},
)

@Composable
fun AdminDashboardRoute(
    onNavigate: (String) -> Unit,
    onShowMessage: (String) -> Unit,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AdminDashboardViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val periodIndex by viewModel.periodIndex.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val preferences = remember { com.lzacademy.app.data.preferences.AppPreferences(context) }
    // Reactive: turning "Alertes de gestion" off in Settings hides the section immediately.
    val alertsEnabled by remember {
        preferences.notificationFlow(com.lzacademy.app.data.settings.ADMIN_ALERTS, true)
    }.collectAsStateWithLifecycle(initialValue = true)
    when (val s = state) {
        UiState.Loading -> {
            LZStatusBarIcons(lightIcons = true)
            LZDashboardSkeleton(LZ.colors.adminHeaderBrush, modifier)
        }
        is UiState.Error -> LZEmptyState(
            title = "Chargement impossible",
            message = s.message,
            actionLabel = "Réessayer",
            onAction = viewModel::load,
            modifier = modifier.fillMaxSize(),
        )
        is UiState.Success -> AdminDashboardScreen(
            data = s.data,
            periodIndex = periodIndex,
            modifier = modifier,
            showAlerts = alertsEnabled,
            actions = AdminDashboardActions(
                onAvatarClick = onAvatarClick,
                onBellClick = { onShowMessage("Aucune nouvelle notification d'administration.") },
                onHeaderStatClick = { onNavigate(LZRoutes.AdminManagement) },
                onQuickAction = {
                    when (it.id) {
                        "reports" -> onNavigate(LZRoutes.AdminReports)
                        "send_sms" -> onNavigate(LZRoutes.AdminCommunication)
                        else -> onShowMessage("« ${it.label} » arrive dans la livraison Administration.")
                    }
                },
                onRevenueClick = { onShowMessage("Les analyses financières arrivent dans la livraison Administration.") },
                onKpiClick = { onNavigate(LZRoutes.AdminManagement) },
                onAlertConfirmed = {
                    onShowMessage("Démo : « ${it.title} » enregistré, aucun SMS n'a été envoyé.")
                },
                onSelectPeriod = viewModel::selectPeriod,
                onSeeAllPayments = { onShowMessage("Le module Paiements arrive dans la livraison Administration.") },
                onPaymentClick = { onShowMessage("${it.student} • ${it.amount} • ${it.status.label}") },
            ),
        )
    }
}

@Composable
fun AdminDashboardScreen(
    data: AdminDashboardData,
    periodIndex: Int,
    modifier: Modifier = Modifier,
    showAlerts: Boolean = true,
    actions: AdminDashboardActions = AdminDashboardActions(),
) {
    val c = LZ.colors
    var pendingAlert by remember { mutableStateOf<AlertUi?>(null) }
    var selectedBar by remember(periodIndex) { mutableStateOf<Int?>(null) }
    val period = data.periods[periodIndex]
    val series = data.enrollment.getValue(period)
    val periodLabels = remember(data.periods) { data.periods.map { "$it mois" } }

    Column(
        modifier
            .fillMaxSize()
            .background(c.background)
            .verticalScroll(rememberScrollState()),
    ) {
        LZStatusBarIcons(lightIcons = true)

        // Distinct purple header with the four headline tiles inside
        LZGradientHeader(brush = c.adminHeaderBrush) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Rounded.WorkspacePremium, null, tint = LZ.colors.warning, modifier = Modifier.size(26.dp))
                LZText(
                    "Espace Administration",
                    style = LZ.type.sectionTitle,
                    color = LZ.colors.onHeader,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
                LZHeaderIconButton(Icons.Rounded.Notifications, "Notifications", actions.onBellClick)
            }
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.pressable(androidx.compose.foundation.shape.CircleShape, onClick = actions.onAvatarClick)) {
                    LZAvatar(
                        "AD",
                        size = 52.dp,
                        ring = true,
                        photo = rememberAdminLocalPhoto(),
                        background = androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(c.primary, c.primaryDark),
                        ),
                    )
                }
                Column(Modifier.weight(1f)) {
                    LZText("Bonjour, ${data.adminName} 👋", style = LZ.type.pageTitle, color = c.onHeader, maxLines = 1)
                    LZText(
                        "Vue d'ensemble de l'académie",
                        style = LZ.type.secondary,
                        color = c.onHeader.copy(alpha = 0.72f),
                        maxLines = 1,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                data.headerStats.forEach { stat ->
                    LZKpiTile(
                        icon = stat.icon,
                        value = stat.value,
                        label = stat.label,
                        container = stat.container,
                        accent = stat.accent,
                        modifier = Modifier.weight(1f),
                        onClick = { actions.onHeaderStatClick(stat.label) },
                    )
                }
            }
        }

        Column(
            Modifier.padding(horizontal = 20.dp).padding(top = 22.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(26.dp),
        ) {
            // Quick actions
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                data.quickActions.forEach { action ->
                    QuickActionButton(action, Modifier.weight(1f), onClick = { actions.onQuickAction(action) })
                }
            }

            // Revenue
            LZAnalyticsCard(
                title = data.revenue.caption,
                value = data.revenue.amount,
                delta = data.revenue.delta,
                caption = "par rapport au mois dernier",
                onClick = actions.onRevenueClick,
            ) {
                LZLineChart(
                    values = data.revenue.trend.values,
                    labels = data.revenue.trend.labels,
                    chartHeight = 72.dp,
                    compact = true,
                )
            }

            // Indicators
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LZSectionHeader("Indicateurs du jour")
                data.kpis.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        pair.forEach { stat ->
                            LZStatCard(
                                icon = stat.icon,
                                value = stat.value,
                                label = stat.label,
                                accent = stat.accent,
                                modifier = Modifier.weight(1f),
                                onClick = { actions.onKpiClick(stat.label) },
                            )
                        }
                    }
                }
            }

            // Alerts — shown only while "Alertes de gestion" is on in Settings
            if (showAlerts) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LZSectionHeader("Alertes")
                    LZCard(contentPadding = PaddingValues(vertical = 6.dp, horizontal = 14.dp)) {
                        if (data.alerts.isEmpty()) {
                            LZText(
                                "Aucune donnée — rien à signaler.",
                                style = LZ.type.secondary,
                                modifier = Modifier.padding(vertical = 12.dp),
                            )
                        }
                        data.alerts.forEachIndexed { i, alert ->
                            AlertRow(alert, onAction = { pendingAlert = alert })
                            if (i < data.alerts.lastIndex) com.lzacademy.app.components.LZDivider()
                        }
                    }
                }
            }

            // Enrollment chart
            LZChartCard(
                title = "Évolution des inscriptions",
                subtitle = selectedBar?.let { "${series.labels[it]} : effectif ${series.values[it].toInt()} élèves" }
                    ?: "Effectif total d'élèves par mois — touchez une barre pour le détail",
                trailing = {
                    LZCompactDropdown(periodLabels, periodIndex, actions.onSelectPeriod)
                },
            ) {
                LZBarChart(
                    values = series.values,
                    labels = series.labels,
                    selectedIndex = selectedBar,
                    onSelect = { selectedBar = if (selectedBar == it) null else it },
                    chartHeight = 150.dp,
                )
            }

            // Subject distribution
            LZChartCard(title = "Répartition par niveau", subtitle = "Effectif d'élèves par niveau") {
                if (data.subjectDistribution.isEmpty()) {
                    LZText("Aucune donnée — la base ne contient pas encore d'élèves.", style = LZ.type.secondary)
                } else {
                    LZDonutChart(segments = data.subjectDistribution)
                }
            }

            // Recent payments (real data from the Supabase mirror)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LZSectionHeader("Derniers paiements", actionLabel = "Voir tout", onAction = actions.onSeeAllPayments)
                if (data.recentPayments.isEmpty()) {
                    LZCard { LZText("Aucune donnée — aucun paiement enregistré.", style = LZ.type.secondary) }
                }
                data.recentPayments.forEach { p ->
                    LZPaymentCard(
                        title = p.student,
                        subtitle = "${p.title} • ${p.date}",
                        amount = p.amount,
                        status = p.status,
                        onClick = { actions.onPaymentClick(p) },
                    )
                }
                LZText(
                    "Source : base de données de l'académie (synchronisée).",
                    style = LZ.type.label,
                    color = c.textSecondary,
                )
            }
        }
    }

    pendingAlert?.let { alert ->
        LZModernDialog(
            title = alert.dialogTitle,
            message = alert.dialogMessage,
            confirmLabel = "Confirmer",
            icon = alert.icon,
            footnote = "Mode démo : aucun SMS n'est réellement envoyé.",
            onConfirm = {
                actions.onAlertConfirmed(alert)
                pendingAlert = null
            },
            onDismiss = { pendingAlert = null },
        )
    }
}

@Composable
private fun QuickActionButton(action: QuickActionUi, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .pressable(LZShapes.standard, onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier.size(56.dp).background(action.accent.softColor(), LZShapes.standard),
            contentAlignment = Alignment.Center,
        ) { Icon(action.icon, null, tint = action.accent.color(), modifier = Modifier.size(26.dp)) }
        LZText(action.label, style = LZ.type.label, maxLines = 1)
    }
}

@Composable
private fun AlertRow(alert: AlertUi, onAction: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(42.dp).background(alert.accent.softColor(), LZShapes.small),
            contentAlignment = Alignment.Center,
        ) { Icon(alert.icon, null, tint = alert.accent.color(), modifier = Modifier.size(22.dp)) }
        Column(Modifier.weight(1f)) {
            LZText(alert.title, style = LZ.type.bodyStrong, maxLines = 1)
            LZText(alert.subtitle, style = LZ.type.label, color = LZ.colors.textSecondary, maxLines = 1)
        }
        LZSecondaryButton(alert.actionLabel, onAction, height = 36.dp)
    }
}

@Preview(name = "Admin – clair", showBackground = true, widthDp = 393, heightDp = 2300)
@Composable
private fun AdminDashboardPreviewLight() {
    LZTheme(darkTheme = false) {
        AdminDashboardScreen(PreviewSamples.admin(LocalDate.of(2026, 11, 6)), periodIndex = 2)
    }
}

@Preview(
    name = "Admin – sombre", showBackground = true, widthDp = 393, heightDp = 2300,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AdminDashboardPreviewDark() {
    LZTheme(darkTheme = true) {
        AdminDashboardScreen(PreviewSamples.admin(LocalDate.of(2026, 11, 6)), periodIndex = 1)
    }
}
