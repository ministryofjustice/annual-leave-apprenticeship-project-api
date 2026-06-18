package uk.gov.justice.digital.hmpps.annualleaveapi.service

import java.time.DayOfWeek
import java.time.LocalDate

object LeaveCalculator {

  fun calculateDuration(
    startDate: LocalDate,
    endDate: LocalDate,
    isFirstDayHalfDay: Boolean,
    isLastDayHalfDay: Boolean,
  ): Double {
    val businessDays = startDate.datesUntil(endDate.plusDays(1))
      .filter { it.dayOfWeek != DayOfWeek.SATURDAY && it.dayOfWeek != DayOfWeek.SUNDAY }
      .count()
      .toDouble()

    val halfDayDeductions = listOf(isFirstDayHalfDay, isLastDayHalfDay).count { it } * 0.5

    return businessDays - halfDayDeductions
  }
}
