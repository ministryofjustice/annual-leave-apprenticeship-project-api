package uk.gov.justice.digital.hmpps.templatepackagename.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class LeaveCalculatorTest {

  @Nested
  @DisplayName("calculateDuration()")
  inner class CalculateDuration {

    @Test
    fun `should count business days for a full week`() {
      // Mon 1 Jun - Fri 5 Jun 2026 = 5 business days
      val result = LeaveCalculator.calculateDuration(
        startDate = LocalDate.of(2026, 6, 1),
        endDate = LocalDate.of(2026, 6, 5),
        isFirstDayHalfDay = false,
        isLastDayHalfDay = false,
      )

      assertThat(result).isEqualTo(5.0)
    }

    @Test
    fun `should exclude weekends`() {
      // Mon 1 Jun - Mon 8 Jun 2026 = 6 business days (skips Sat 6, Sun 7)
      val result = LeaveCalculator.calculateDuration(
        startDate = LocalDate.of(2026, 6, 1),
        endDate = LocalDate.of(2026, 6, 8),
        isFirstDayHalfDay = false,
        isLastDayHalfDay = false,
      )

      assertThat(result).isEqualTo(6.0)
    }

    @Test
    fun `should deduct half day for first day`() {
      val result = LeaveCalculator.calculateDuration(
        startDate = LocalDate.of(2026, 6, 1),
        endDate = LocalDate.of(2026, 6, 5),
        isFirstDayHalfDay = true,
        isLastDayHalfDay = false,
      )

      assertThat(result).isEqualTo(4.5)
    }

    @Test
    fun `should deduct half day for last day`() {
      val result = LeaveCalculator.calculateDuration(
        startDate = LocalDate.of(2026, 6, 1),
        endDate = LocalDate.of(2026, 6, 5),
        isFirstDayHalfDay = false,
        isLastDayHalfDay = true,
      )

      assertThat(result).isEqualTo(4.5)
    }

    @Test
    fun `should deduct full day when both half-day flags are set`() {
      val result = LeaveCalculator.calculateDuration(
        startDate = LocalDate.of(2026, 6, 1),
        endDate = LocalDate.of(2026, 6, 5),
        isFirstDayHalfDay = true,
        isLastDayHalfDay = true,
      )

      assertThat(result).isEqualTo(4.0)
    }

    @Test
    fun `should return 1 for a single business day`() {
      val result = LeaveCalculator.calculateDuration(
        startDate = LocalDate.of(2026, 6, 1),
        endDate = LocalDate.of(2026, 6, 1),
        isFirstDayHalfDay = false,
        isLastDayHalfDay = false,
      )

      assertThat(result).isEqualTo(1.0)
    }

    @Test
    fun `should return 0 for a single day with both half-day flags`() {
      val result = LeaveCalculator.calculateDuration(
        startDate = LocalDate.of(2026, 6, 1),
        endDate = LocalDate.of(2026, 6, 1),
        isFirstDayHalfDay = true,
        isLastDayHalfDay = true,
      )

      assertThat(result).isEqualTo(0.0)
    }

    @Test
    fun `should return 0 when range falls entirely on a weekend`() {
      // Sat 6 Jun - Sun 7 Jun 2026
      val result = LeaveCalculator.calculateDuration(
        startDate = LocalDate.of(2026, 6, 6),
        endDate = LocalDate.of(2026, 6, 7),
        isFirstDayHalfDay = false,
        isLastDayHalfDay = false,
      )

      assertThat(result).isEqualTo(0.0)
    }
  }
}
