package com.lzacademy.app

import com.lzacademy.app.viewmodel.lastSixMonths
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/**
 * The revenue and growth charts take the LAST six values of a 12-month window that runs
 * oldest→newest, so their labels must be the six months ending with the current one.
 */
class ReportsMonthsTest {

    @Test
    fun `les libelles finissent par le mois courant`() {
        val months = lastSixMonths(LocalDate.of(2026, 11, 14))
        assertEquals(6, months.size)
        assertEquals(YearMonth.of(2026, 11), months.last())
        assertEquals(YearMonth.of(2026, 6), months.first())
    }

    @Test
    fun `le passage d annee est gere`() {
        val months = lastSixMonths(LocalDate.of(2026, 2, 3))
        assertEquals(
            listOf(
                YearMonth.of(2025, 9),
                YearMonth.of(2025, 10),
                YearMonth.of(2025, 11),
                YearMonth.of(2025, 12),
                YearMonth.of(2026, 1),
                YearMonth.of(2026, 2),
            ),
            months,
        )
    }
}
