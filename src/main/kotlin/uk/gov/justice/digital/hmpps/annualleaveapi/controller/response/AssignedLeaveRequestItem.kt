package uk.gov.justice.digital.hmpps.annualleaveapi.controller.response

import uk.gov.justice.digital.hmpps.annualleaveapi.model.Status
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class AssignedLeaveRequestItem(
  val id: UUID,
  val createdAt: LocalDateTime,
  val decisionAt: LocalDateTime?,
  val creatorId: UUID,
  val creatorName: String,
  val approverId: UUID?,
  val startDate: LocalDate,
  val endDate: LocalDate,
  val duration: Double,
  val isFirstDayHalfDay: Boolean,
  val isLastDayHalfDay: Boolean,
  val status: Status,
  val creatorNote: String?,
  val approverNote: String?,
)
