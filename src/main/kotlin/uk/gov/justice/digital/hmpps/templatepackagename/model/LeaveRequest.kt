package uk.gov.justice.digital.hmpps.templatepackagename.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class LeaveRequest(
  val id: UUID,
  val createdAt: LocalDateTime,
  val decisionAt: LocalDateTime? = null,

  val creatorId: UUID,
  val approverId: UUID? = null,

  val startDate: LocalDate,
  val endDate: LocalDate,
  val duration: Int,
  val isFirstDayHalfDay: Boolean,
  val isLastDayHalfDay: Boolean,

  val status: Status = Status.PENDING,
  val creatorNote: String? = null,
  val approverNote: String? = null,
)
