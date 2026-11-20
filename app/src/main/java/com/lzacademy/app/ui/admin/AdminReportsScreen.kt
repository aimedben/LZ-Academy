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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lzacademy.app.components.LZAnalyticsCard
import com.lzacademy.app.components.LZBarChart
import com.lzacademy.app.components.LZCard
import com.lzacademy.app.components.LZChartCard
import com.lzacademy.app.components.LZDashboardSkeleton
import com.lzacademy.app.components.LZDonutChart
import com.lzacademy.app.components.LZEmptyState
import com.lzacademy.app.components.LZFilterChip
import com.lzacademy.app.components.LZGradientHeader
import com.lzacademy.app.components.LZGreetingRow
import com.lzacademy.app.components.LZLineChart
import com.lzacademy.app.components.LZPrimaryButton
import com.lzacademy.app.components.LZStatusPill
import com.lzacademy.app.components.LZText
import com.lzacademy.app.data.FrenchDates
import com.lzacademy.app.models.DonutSegmentUi
import com.lzacademy.app.theme.LZ
import com.lzacademy.app.theme.LZPalette
import com.lzacademy.app.theme.LZSize
import com.lzacademy.app.viewmodel.AdminReportsViewModel
import com.lzacademy.app.viewmodel.UiState
import java.text.NumberFormat
import java.util.Locale

@Composable
fun AdminReportsScreen(
    onNavigateBack: () -> Unit,
    onShowMessage: (String) -> Unit,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AdminReportsViewModel = viewModel(),
) {
    val periods = listOf("Mois en cours", "Ce Trimestre", "Année 2025/2026")
    var selectedPeriod by rememberSaveable { mutableStateOf("Mois en cours") }
    val state by viewModel.reports.collectAsStateWithLifecycle()

    when (val s = state) {
        UiState.Loading -> LZDashboardSkeleton(LZ.colors.adminHeaderBrush, modifier)
        is UiState.Error -> LZEmptyState(
            title = "Chargement impossible",
            message = s.message,
            actionLabel = "Réessayer",
            onAction = viewModel::load,
            modifier = modifier.fillMaxSize(),
        )
        is UiState.Success -> AdminReportsContent(
            data = s.data,
            periods = periods,
            selectedPeriod = selectedPeriod,
            onPeriodSelected = { selectedPeriod = it },
            onShowMessage = onShowMessage,
            onAvatarClick = onAvatarClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun AdminReportsContent(
    data: AdminReportsViewModel.ReportsData,
    periods: List<String>,
    selectedPeriod: String,
    onPeriodSelected: (String) -> Unit,
    onShowMessage: (String) -> Unit,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val money = remember(data.monthRevenue) {
        NumberFormat.getIntegerInstance(Locale.FRANCE).format(data.monthRevenue.toLong())
    }
    val delta = remember(data.monthRevenue, data.prevMonthRevenue) {
        when {
            data.prevMonthRevenue <= 0.0 && data.monthRevenue <= 0.0 -> "— ce mois"
            data.prevMonthRevenue <= 0.0 -> "Nouveau revenu ce mois"
            else -> {
                val pct = (data.monthRevenue - data.prevMonthRevenue) / data.prevMonthRevenue * 100.0
                (if (pct >= 0) "+" else "") + "%.1f %% ce mois".format(pct).replace(".", ",")
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LZ.colors.background)
            .testTag("admin_reports_screen"),
    ) {
        // Admin gradient header
        LZGradientHeader(brush = LZ.colors.adminHeaderBrush) {
            LZGreetingRow(
                initials = "LZ",
                title = "Rapports & Statistiques",
                subtitle = "Performances Académiques • LZ Chéraga",
                onAvatarClick = onAvatarClick,
                photo = rememberAdminLocalPhoto(),
                trailing = {
                    IconButton(
                        onClick = { onShowMessage("Rapport d'activité complet exporté au format PDF !") },
                        modifier = Modifier.testTag("export_report_button"),
                    ) {
                        Icon(Icons.Rounded.Download, "Exporter", tint = Color.White)
                    }
                },
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Period selector
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = LZSize.ScreenPadding),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(periods) { period ->
                        LZFilterChip(
                            label = period,
                            selected = selectedPeriod == period,
                            onClick = { onPeriodSelected(period) },
                        )
                    }
                }
            }

            // Financial summary card with sparkline (real paid revenue)
            item {
                Box(Modifier.padding(horizontal = LZSize.ScreenPadding)) {
                    LZAnalyticsCard(
                        title = "Chiffre d'Affaires Mensuel",
                        value = if (data.totalStudents == 0 && data.monthRevenue == 0.0) {
                            "Aucune donnée"
                        } else {
                            "$money DZD"
                        },
                        delta = delta,
                        deltaPositive = data.monthRevenue >= data.prevMonthRevenue,
                        caption = "Total encaissé (paiements réglés) • ${FrenchDates.monthLong[java.time.LocalDate.now().monthValue - 1]}",
                    ) {
                        if (data.revenueTrend.values.any { it > 0f }) {
                            LZLineChart(
                                values = data.revenueTrend.values,
                                labels = data.revenueTrend.labels,
                                lineColor = LZ.colors.primary,
                            )
                        } else {
                            LZText("Aucune donnée — aucun paiement réglé sur la période.", style = LZ.type.secondary)
                        }
                    }
                }
            }

            // KPI Grid (real mirror values only)
            item {
                Row(
                    modifier = Modifier.padding(horizontal = LZSize.ScreenPadding).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    LZCard(modifier = Modifier.weight(1f)) {
                        LZText("Assiduité globale", style = LZ.type.secondary, color = LZ.colors.textSecondary)
                        Spacer(Modifier.height(4.dp))
                        val rate = data.attendanceRate
                        if (rate != null) {
                            LZText(
                                "%.1f %%".format(rate * 100f).replace(".", ","),
                                style = LZ.type.stat.copy(fontSize = 24.sp),
                                color = LZ.colors.success,
                            )
                            Spacer(Modifier.height(4.dp))
                            LZStatusPill("${data.attendanceCalls} appels", LZ.colors.success, LZ.colors.successSoft)
                        } else {
                            LZText("Aucune donnée", style = LZ.type.stat.copy(fontSize = 18.sp), color = LZ.colors.textSecondary)
                            Spacer(Modifier.height(4.dp))
                            LZStatusPill("Aucun appel", LZ.colors.textSecondary, LZ.colors.surfaceSoft)
                        }
                    }
                    LZCard(modifier = Modifier.weight(1f)) {
                        LZText("Paiements en attente", style = LZ.type.secondary, color = LZ.colors.textSecondary)
                        Spacer(Modifier.height(4.dp))
                        LZText(
                            data.pendingPayments.toString(),
                            style = LZ.type.stat.copy(fontSize = 24.sp),
                            color = LZ.colors.primary,
                        )
                        Spacer(Modifier.height(4.dp))
                        LZStatusPill(
                            if (data.pendingPayments > 0) "À valider" else "À jour",
                            LZ.colors.primary,
                            LZ.colors.surfaceSoft,
                        )
                    }
                }
            }

            // Enrollment Bar Chart (real cumulative headcount)
            item {
                Box(Modifier.padding(horizontal = LZSize.ScreenPadding)) {
                    LZChartCard(
                        title = "Évolution des Inscriptions",
                        subtitle = "Effectif cumulé d'élèves sur les 6 derniers mois (${data.totalStudents} élèves)",
                    ) {
                        if (data.growth.values.isNotEmpty()) {
                            LZBarChart(
                                values = data.growth.values,
                                labels = data.growth.labels,
                                barColor = LZPalette.DeepPurple,
                            )
                        } else {
                            LZText("Aucune donnée — la base ne contient pas encore d'élèves.", style = LZ.type.secondary)
                        }
                    }
                }
            }

            // Level Distribution Donut (real students per level)
            item {
                Box(Modifier.padding(horizontal = LZSize.ScreenPadding)) {
                    LZChartCard(
                        title = "Répartition par Niveau",
                        subtitle = "Effectif d'élèves par niveau d'études",
                    ) {
                        val donutColors = listOf(
                            LZPalette.PrimaryGreen, LZPalette.Violet, LZPalette.Turquoise,
                            LZPalette.Orange, LZPalette.Gold,
                        )
                        if (data.studentsByLevel.isEmpty()) {
                            LZText("Aucune donnée — la base ne contient pas encore d'élèves.", style = LZ.type.secondary)
                        } else {
                            LZDonutChart(
                                segments = data.studentsByLevel.mapIndexed { i, (label, count) ->
                                    DonutSegmentUi(label, count.toFloat(), donutColors[i % donutColors.size])
                                },
                                diameter = 140.dp,
                            )
                        }
                    }
                }
            }

            // Export action
            item {
                Box(Modifier.padding(horizontal = LZSize.ScreenPadding)) {
                    LZPrimaryButton(
                        text = "Télécharger le bilan annuel (PDF + Excel)",
                        onClick = { onShowMessage("Génération du rapport annuel complet LZ Academy 2026...") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = Icons.Rounded.Download,
                    )
                }
            }
        }
    }
}
