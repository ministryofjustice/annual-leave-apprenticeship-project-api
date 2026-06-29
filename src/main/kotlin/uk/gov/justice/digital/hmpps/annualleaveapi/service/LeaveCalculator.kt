package uk.gov.justice.digital.hmpps.annualleaveapi.service

import java.time.DayOfWeek
import java.time.LocalDate

object LeaveCalculator {

  private val weekends = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

  fun calculateDuration(
    startDate: LocalDate,
    endDate: LocalDate,
    isFirstDayHalfDay: Boolean,
    isLastDayHalfDay: Boolean,
  ): Double {
    val businessDays = startDate.datesUntil(endDate.plusDays(1))
      .filter { it.dayOfWeek !in weekends }
      .count()
      .toDouble()

    val halfDayDeductions = listOf(
      isFirstDayHalfDay && startDate.dayOfWeek !in weekends,
      isLastDayHalfDay && endDate.dayOfWeek !in weekends,
    ).count { it } * 0.5

    return businessDays - halfDayDeductions
  }
}
