package uk.gov.justice.digital.hmpps.annualleaveapi.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "leave_requests")
data class LeaveRequest(
  @Id
  val id: UUID,

  @Column(name = "created_at", nullable = false)
  val createdAt: LocalDateTime,

  @Column(name = "decision_at")
  val decisionAt: LocalDateTime? = null,

  @Column(name = "creator_id", nullable = false)
  val creatorId: UUID,

  @Column(name = "approver_id")
  val approverId: UUID? = null,

  @Column(name = "start_date", nullable = false)
  val startDate: LocalDate,

  @Column(name = "end_date", nullable = false)
  val endDate: LocalDate,

  @Column(name = "duration", nullable = false)
  val duration: Double,

  @Column(name = "is_first_day_half_day", nullable = false)
  val isFirstDayHalfDay: Boolean,

  @Column(name = "is_last_day_half_day", nullable = false)
  val isLastDayHalfDay: Boolean,

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  val status: Status = Status.PENDING,

  @Column(name = "creator_note")
  val creatorNote: String? = null,

  @Column(name = "approver_note")
  val approverNote: String? = null,
)
