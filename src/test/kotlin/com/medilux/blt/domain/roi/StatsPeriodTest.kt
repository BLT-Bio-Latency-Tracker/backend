package com.medilux.blt.domain.roi

import com.medilux.blt.domain.roi.service.StatsPeriod
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class StatsPeriodTest {
    @Test
    fun `from normalizes case-insensitively`() {
        assertThat(StatsPeriod.from("week")).isEqualTo(StatsPeriod.WEEK)
        assertThat(StatsPeriod.from("MONTH")).isEqualTo(StatsPeriod.MONTH)
        assertThat(StatsPeriod.from("Year")).isEqualTo(StatsPeriod.YEAR)
    }

    @Test
    fun `from falls back to MONTH for unknown values`() {
        assertThat(StatsPeriod.from("bogus")).isEqualTo(StatsPeriod.MONTH)
        assertThat(StatsPeriod.from("")).isEqualTo(StatsPeriod.MONTH)
    }

    @Test
    fun `defaultFrom computes range start per period`() {
        val today = LocalDate.of(2026, 5, 20)
        assertThat(StatsPeriod.WEEK.defaultFrom(today)).isEqualTo(LocalDate.of(2026, 5, 14)) // 최근 7일(today-6)
        assertThat(StatsPeriod.MONTH.defaultFrom(today)).isEqualTo(LocalDate.of(2026, 5, 1)) // 당월 1일
        assertThat(StatsPeriod.YEAR.defaultFrom(today)).isEqualTo(LocalDate.of(2026, 1, 1)) // 올해 1/1
    }

    @Test
    fun `label is lowercase`() {
        assertThat(StatsPeriod.WEEK.label).isEqualTo("week")
        assertThat(StatsPeriod.MONTH.label).isEqualTo("month")
        assertThat(StatsPeriod.YEAR.label).isEqualTo("year")
    }
}
