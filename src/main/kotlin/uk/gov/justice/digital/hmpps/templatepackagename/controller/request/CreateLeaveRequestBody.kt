package uk.gov.justice.digital.hmpps.templatepackagename.controller.request

import java.time.LocalDate

data class CreateLeaveRequestBody(
  val startDate: LocalDate,
  val endDate: LocalDate,
  val isFirstDayHalfDay: Boolean,
  val isLastDayHalfDay: Boolean,
  val creatorNote: String? = null,
)
