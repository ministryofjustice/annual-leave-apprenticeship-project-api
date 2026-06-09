package uk.gov.justice.digital.hmpps.templatepackagename.data

import uk.gov.justice.digital.hmpps.templatepackagename.model.LeaveRequest
import uk.gov.justice.digital.hmpps.templatepackagename.model.Status
import uk.gov.justice.digital.hmpps.templatepackagename.model.User
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

object SeedData {

  val userAlice = User(
    id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
    name = "Alice Johnson",
    email = "alice@example.com",
    password = "password",
    managerId = UUID.fromString("00000000-0000-0000-0000-000000000002"),
    annualEntitlement = 25,
  )

  val userBob = User(
    id = UUID.fromString("00000000-0000-0000-0000-000000000002"),
    name = "Bob Smith",
    email = "bob@example.com",
    password = "password",
    managerId = null,
    annualEntitlement = 30,
  )

  val users = listOf(userAlice, userBob)

  val leaveRequests = mutableListOf(
    LeaveRequest(
      id = UUID.fromString("00000000-0000-0000-0000-000000000101"),
      createdAt = LocalDateTime.of(2026, 5, 20, 10, 0),
      creatorId = userAlice.id,
      approverId = userBob.id,
      startDate = LocalDate.of(2026, 6, 10),
      endDate = LocalDate.of(2026, 6, 14),
      duration = 4.5,
      isFirstDayHalfDay = false,
      isLastDayHalfDay = true,
      status = Status.PENDING,
      creatorNote = "Family holiday",
    ),
    LeaveRequest(
      id = UUID.fromString("00000000-0000-0000-0000-000000000102"),
      createdAt = LocalDateTime.of(2026, 5, 22, 14, 30),
      creatorId = userAlice.id,
      approverId = userBob.id,
      startDate = LocalDate.of(2026, 7, 1),
      endDate = LocalDate.of(2026, 7, 3),
      duration = 3.0,
      isFirstDayHalfDay = false,
      isLastDayHalfDay = false,
      status = Status.APPROVED,
      creatorNote = "Short break",
      approverNote = "Looks good",
      decisionAt = LocalDateTime.of(2026, 5, 23, 9, 0),
    ),
  )
}
