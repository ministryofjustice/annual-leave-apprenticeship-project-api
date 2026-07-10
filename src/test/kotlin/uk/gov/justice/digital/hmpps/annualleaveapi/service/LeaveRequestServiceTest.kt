package uk.gov.justice.digital.hmpps.annualleaveapi.service

import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.annualleaveapi.config.ForbiddenException
import uk.gov.justice.digital.hmpps.annualleaveapi.config.LeaveRequestNotFoundException
import uk.gov.justice.digital.hmpps.annualleaveapi.config.UserNotFoundException
import uk.gov.justice.digital.hmpps.annualleaveapi.controller.request.CreateLeaveRequestBody
import uk.gov.justice.digital.hmpps.annualleaveapi.controller.request.DecisionRequest
import uk.gov.justice.digital.hmpps.annualleaveapi.model.LeaveRequest
import uk.gov.justice.digital.hmpps.annualleaveapi.model.Status
import uk.gov.justice.digital.hmpps.annualleaveapi.model.User
import uk.gov.justice.digital.hmpps.annualleaveapi.repository.LeaveRequestRepository
import uk.gov.justice.digital.hmpps.annualleaveapi.repository.UserRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

class LeaveRequestServiceTest {

  private val leaveRequestRepository: LeaveRequestRepository = mock()
  private val userRepository: UserRepository = mock()
  private val service = LeaveRequestService(leaveRequestRepository, userRepository)

  private val alice = User(
    id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
    firstName = "Alice",
    lastName = "Johnson",
    email = "alice@example.com",
    password = "password",
    managerId = UUID.fromString("00000000-0000-0000-0000-000000000002"),
    annualEntitlement = 25,
  )

  private val bob = User(
    id = UUID.fromString("00000000-0000-0000-0000-000000000002"),
    firstName = "Bob",
    lastName = "Smith",
    email = "bob@example.com",
    password = "password",
    managerId = null,
    annualEntitlement = 30,
  )

  private val alicePendingRequest = LeaveRequest(
    id = UUID.fromString("00000000-0000-0000-0000-000000000101"),
    createdAt = LocalDateTime.of(2026, 5, 20, 10, 0),
    creatorId = alice.id,
    approverId = bob.id,
    startDate = LocalDate.of(2026, 6, 10),
    endDate = LocalDate.of(2026, 6, 12),
    duration = 2.5,
    isFirstDayHalfDay = false,
    isLastDayHalfDay = true,
    status = Status.PENDING,
    creatorNote = "Family holiday",
  )

  private val aliceApprovedRequest = LeaveRequest(
    id = UUID.fromString("00000000-0000-0000-0000-000000000102"),
    createdAt = LocalDateTime.of(2026, 5, 15, 10, 0),
    decisionAt = LocalDateTime.of(2026, 5, 16, 9, 0),
    creatorId = alice.id,
    approverId = bob.id,
    startDate = LocalDate.of(2026, 7, 1),
    endDate = LocalDate.of(2026, 7, 3),
    duration = 3.0,
    isFirstDayHalfDay = false,
    isLastDayHalfDay = false,
    status = Status.APPROVED,
    creatorNote = "Summer trip",
  )

  @Nested
  @DisplayName("getBalance()")
  inner class GetBalance {

    @Test
    fun `should return balance with pending and approved days`() {
      whenever(userRepository.findById(alice.id)).thenReturn(Optional.of(alice))
      whenever(leaveRequestRepository.findAllByCreatorIdAndStatusIn(alice.id, listOf(Status.PENDING, Status.APPROVED)))
        .thenReturn(listOf(alicePendingRequest, aliceApprovedRequest))

      val result = service.getBalance(alice.id)

      assertThat(result.annualEntitlement).isEqualTo(25)
      assertThat(result.pendingDays).isEqualTo(2.5)
      assertThat(result.approvedDays).isEqualTo(3.0)
      assertThat(result.availableBalance).isEqualTo(19.5)
      assertThat(result.actualBalance).isEqualTo(22.0)
    }

    @Test
    fun `should return full entitlement when user has no requests`() {
      whenever(userRepository.findById(bob.id)).thenReturn(Optional.of(bob))
      whenever(leaveRequestRepository.findAllByCreatorIdAndStatusIn(bob.id, listOf(Status.PENDING, Status.APPROVED)))
        .thenReturn(emptyList())

      val result = service.getBalance(bob.id)

      assertThat(result.annualEntitlement).isEqualTo(30)
      assertThat(result.pendingDays).isEqualTo(0.0)
      assertThat(result.approvedDays).isEqualTo(0.0)
      assertThat(result.availableBalance).isEqualTo(30.0)
      assertThat(result.actualBalance).isEqualTo(30.0)
    }

    @Test
    fun `should throw UserNotFoundException when user does not exist`() {
      val unknownId = UUID.randomUUID()
      whenever(userRepository.findById(unknownId)).thenReturn(Optional.empty())

      assertThatThrownBy { service.getBalance(unknownId) }
        .isInstanceOf(UserNotFoundException::class.java)
        .hasMessageContaining(unknownId.toString())
    }
  }

  @Nested
  @DisplayName("getRequestsByUser()")
  inner class GetRequestsByUser {

    @Test
    fun `should return requests for a user who has them`() {
      whenever(userRepository.existsById(alice.id)).thenReturn(true)
      whenever(leaveRequestRepository.findAllByCreatorId(alice.id)).thenReturn(listOf(alicePendingRequest))

      val results = service.getRequestsByUser(alice.id)

      assertThat(results).hasSize(1)
      assertThat(results).allMatch { it.creatorId == alice.id }
    }

    @Test
    fun `should return empty list when user has no requests`() {
      whenever(userRepository.existsById(bob.id)).thenReturn(true)
      whenever(leaveRequestRepository.findAllByCreatorId(bob.id)).thenReturn(emptyList())

      val results = service.getRequestsByUser(bob.id)

      assertThat(results).isEmpty()
    }

    @Test
    fun `should throw UserNotFoundException when user does not exist`() {
      val unknownId = UUID.randomUUID()
      whenever(userRepository.existsById(unknownId)).thenReturn(false)

      assertThatThrownBy { service.getRequestsByUser(unknownId) }
        .isInstanceOf(UserNotFoundException::class.java)
        .hasMessageContaining(unknownId.toString())
    }
  }

  @Nested
  @DisplayName("createRequest()")
  inner class CreateRequest {

    private val request = CreateLeaveRequestBody(
      startDate = LocalDate.of(2026, 8, 3),
      endDate = LocalDate.of(2026, 8, 7),
      isFirstDayHalfDay = false,
      isLastDayHalfDay = true,
      creatorNote = "Holiday",
    )

    @BeforeEach
    fun setUp() {
      whenever(userRepository.findById(alice.id)).thenReturn(Optional.of(alice))
      whenever(userRepository.findById(bob.id)).thenReturn(Optional.of(bob))
      whenever(leaveRequestRepository.findAllByCreatorIdAndStatusIn(any(), any())).thenReturn(emptyList())
      whenever(leaveRequestRepository.save(any<LeaveRequest>())).thenAnswer { it.arguments[0] }
    }

    @Test
    fun `should create a leave request with calculated duration`() {
      val result = service.createRequest(alice.id, request)

      assertThat(result.creatorId).isEqualTo(alice.id)
      assertThat(result.approverId).isEqualTo(alice.managerId)
      assertThat(result.startDate).isEqualTo(request.startDate)
      assertThat(result.endDate).isEqualTo(request.endDate)
      assertThat(result.duration).isEqualTo(4.5)
      assertThat(result.isFirstDayHalfDay).isFalse()
      assertThat(result.isLastDayHalfDay).isTrue()
      assertThat(result.status).isEqualTo(Status.PENDING)
      assertThat(result.creatorNote).isEqualTo("Holiday")
      assertThat(result.approverNote).isNull()
      assertThat(result.decisionAt).isNull()
      assertThat(result.id).isNotNull()
      assertThat(result.createdAt).isNotNull()
      verify(leaveRequestRepository).save(any<LeaveRequest>())
    }

    @Test
    fun `should throw UserNotFoundException when user does not exist`() {
      val unknownId = UUID.randomUUID()
      whenever(userRepository.findById(unknownId)).thenReturn(Optional.empty())

      assertThatThrownBy { service.createRequest(unknownId, request) }
        .isInstanceOf(UserNotFoundException::class.java)
        .hasMessageContaining(unknownId.toString())
    }

    @Test
    fun `should throw ValidationException when end date is before start date`() {
      val badRequest = request.copy(
        startDate = LocalDate.of(2026, 8, 7),
        endDate = LocalDate.of(2026, 8, 3),
      )

      assertThatThrownBy { service.createRequest(alice.id, badRequest) }
        .isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("End date must not be before start date")
    }

    @Test
    fun `should throw ValidationException when duration is zero`() {
      val singleDayBothHalves = request.copy(
        startDate = LocalDate.of(2026, 8, 3),
        endDate = LocalDate.of(2026, 8, 3),
        isFirstDayHalfDay = true,
        isLastDayHalfDay = true,
      )

      assertThatThrownBy { service.createRequest(alice.id, singleDayBothHalves) }
        .isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("duration must be greater than 0")
    }

    @Test
    fun `should throw ValidationException when start date falls on a Saturday`() {
      val saturdayStart = request.copy(
        startDate = LocalDate.of(2026, 8, 8),
        endDate = LocalDate.of(2026, 8, 10),
      )

      assertThatThrownBy { service.createRequest(alice.id, saturdayStart) }
        .isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("Start date must not fall on a weekend")
    }

    @Test
    fun `should throw ValidationException when start date falls on a Sunday`() {
      val sundayStart = request.copy(
        startDate = LocalDate.of(2026, 8, 9),
        endDate = LocalDate.of(2026, 8, 10),
      )

      assertThatThrownBy { service.createRequest(alice.id, sundayStart) }
        .isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("Start date must not fall on a weekend")
    }

    @Test
    fun `should throw ValidationException when end date falls on a Saturday`() {
      val saturdayEnd = request.copy(
        startDate = LocalDate.of(2026, 8, 3),
        endDate = LocalDate.of(2026, 8, 8),
      )

      assertThatThrownBy { service.createRequest(alice.id, saturdayEnd) }
        .isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("End date must not fall on a weekend")
    }

    @Test
    fun `should throw ValidationException when end date falls on a Sunday`() {
      val sundayEnd = request.copy(
        startDate = LocalDate.of(2026, 8, 3),
        endDate = LocalDate.of(2026, 8, 9),
      )

      assertThatThrownBy { service.createRequest(alice.id, sundayEnd) }
        .isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("End date must not fall on a weekend")
    }

    @Test
    fun `should throw ValidationException when new request overlaps end of existing request`() {
      whenever(leaveRequestRepository.findAllByCreatorIdAndStatusIn(any(), any()))
        .thenReturn(listOf(alicePendingRequest))

      val overlapping = request.copy(
        startDate = LocalDate.of(2026, 6, 12),
        endDate = LocalDate.of(2026, 6, 16),
      )

      assertThatThrownBy { service.createRequest(alice.id, overlapping) }
        .isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("overlap")
    }

    @Test
    fun `should throw ValidationException when new request overlaps start of existing request`() {
      whenever(leaveRequestRepository.findAllByCreatorIdAndStatusIn(any(), any()))
        .thenReturn(listOf(alicePendingRequest))

      val overlapping = request.copy(
        startDate = LocalDate.of(2026, 6, 8),
        endDate = LocalDate.of(2026, 6, 11),
      )

      assertThatThrownBy { service.createRequest(alice.id, overlapping) }
        .isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("overlap")
    }

    @Test
    fun `should throw ValidationException when new request is fully contained within existing request`() {
      whenever(leaveRequestRepository.findAllByCreatorIdAndStatusIn(any(), any()))
        .thenReturn(listOf(alicePendingRequest))

      val overlapping = request.copy(
        startDate = LocalDate.of(2026, 6, 11),
        endDate = LocalDate.of(2026, 6, 12),
      )

      assertThatThrownBy { service.createRequest(alice.id, overlapping) }
        .isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("overlap")
    }

    @Test
    fun `should throw ValidationException when new request fully contains existing request`() {
      whenever(leaveRequestRepository.findAllByCreatorIdAndStatusIn(any(), any()))
        .thenReturn(listOf(alicePendingRequest))

      val overlapping = request.copy(
        startDate = LocalDate.of(2026, 6, 8),
        endDate = LocalDate.of(2026, 6, 16),
      )

      assertThatThrownBy { service.createRequest(alice.id, overlapping) }
        .isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("overlap")
    }

    @Test
    fun `should throw ValidationException when new request has exact same dates as existing request`() {
      whenever(leaveRequestRepository.findAllByCreatorIdAndStatusIn(any(), any()))
        .thenReturn(listOf(alicePendingRequest))

      val overlapping = request.copy(
        startDate = LocalDate.of(2026, 6, 10),
        endDate = LocalDate.of(2026, 6, 12),
      )

      assertThatThrownBy { service.createRequest(alice.id, overlapping) }
        .isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("overlap")
    }

    @Test
    fun `should allow dates that don't overlap with existing requests`() {
      val bobRequest = request.copy(
        startDate = LocalDate.of(2026, 6, 10),
        endDate = LocalDate.of(2026, 6, 12),
      )

      val result = service.createRequest(bob.id, bobRequest)

      assertThat(result.creatorId).isEqualTo(bob.id)
      assertThat(result.duration).isEqualTo(2.5)
    }

    @Test
    fun `should throw ValidationException when request exceeds annual entitlement`() {
      // Alice has 25 days:
      // Give her an existing 22-day approved request,
      // then try to book 4.5 more
      val existingLargeRequest = alicePendingRequest.copy(
        id = UUID.randomUUID(),
        startDate = LocalDate.of(2026, 9, 1),
        endDate = LocalDate.of(2026, 9, 30),
        duration = 22.0,
        status = Status.APPROVED,
      )
      whenever(leaveRequestRepository.findAllByCreatorIdAndStatusIn(any(), any()))
        .thenReturn(listOf(existingLargeRequest))

      assertThatThrownBy { service.createRequest(alice.id, request) }
        .isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("Insufficient annual entitlement")
        .hasMessageContaining("3.0 days remaining")
        .hasMessageContaining("4.5 days requested")
    }

    @Test
    fun `should allow request when it exactly uses remaining entitlement`() {
      // Alice has 25 days. Give her 20.5 used, then request 4.5 = exactly 25
      val existingRequest = alicePendingRequest.copy(
        id = UUID.randomUUID(),
        startDate = LocalDate.of(2026, 9, 1),
        endDate = LocalDate.of(2026, 9, 30),
        duration = 20.5,
        status = Status.APPROVED,
      )
      whenever(leaveRequestRepository.findAllByCreatorIdAndStatusIn(any(), any()))
        .thenReturn(listOf(existingRequest))

      val result = service.createRequest(alice.id, request)

      assertThat(result.creatorId).isEqualTo(alice.id)
      assertThat(result.status).isEqualTo(Status.PENDING)
      verify(leaveRequestRepository).save(any<LeaveRequest>())
    }
  }

  @Nested
  @DisplayName("deleteRequest()")
  inner class DeleteRequest {

    @Test
    fun `should delete a pending request owned by the user`() {
      whenever(leaveRequestRepository.findById(alicePendingRequest.id)).thenReturn(Optional.of(alicePendingRequest))

      service.deleteRequest(alice.id, alicePendingRequest.id)

      verify(leaveRequestRepository).delete(alicePendingRequest)
    }

    @Test
    fun `should throw LeaveRequestNotFoundException when request does not exist`() {
      val unknownId = UUID.randomUUID()
      whenever(leaveRequestRepository.findById(unknownId)).thenReturn(Optional.empty())

      assertThatThrownBy { service.deleteRequest(alice.id, unknownId) }
        .isInstanceOf(LeaveRequestNotFoundException::class.java)
        .hasMessageContaining(unknownId.toString())
    }

    @Test
    fun `should throw ForbiddenException when user is not the creator`() {
      whenever(leaveRequestRepository.findById(alicePendingRequest.id)).thenReturn(Optional.of(alicePendingRequest))

      assertThatThrownBy { service.deleteRequest(bob.id, alicePendingRequest.id) }
        .isInstanceOf(ForbiddenException::class.java)
        .hasMessageContaining("your own")
    }

    @Test
    fun `should throw ValidationException when request is not pending`() {
      whenever(leaveRequestRepository.findById(aliceApprovedRequest.id)).thenReturn(Optional.of(aliceApprovedRequest))

      assertThatThrownBy { service.deleteRequest(alice.id, aliceApprovedRequest.id) }
        .isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("pending")
    }
  }

  @Nested
  @DisplayName("getAssignedRequests()")
  inner class GetAssignedRequests {

    @Test
    fun `should return requests assigned to the manager`() {
      whenever(userRepository.existsById(bob.id)).thenReturn(true)
      whenever(leaveRequestRepository.findAllByApproverId(bob.id))
        .thenReturn(listOf(alicePendingRequest, aliceApprovedRequest))
      whenever(userRepository.findById(alice.id)).thenReturn(Optional.of(alice))

      val results = service.getAssignedRequests(bob.id)

      assertThat(results).hasSize(2)
      assertThat(results).allMatch { it.approverId == bob.id }
      assertThat(results).allMatch { it.creatorName == "Alice Johnson" }
    }

    @Test
    fun `should return empty list when manager has no assigned requests`() {
      whenever(userRepository.existsById(alice.id)).thenReturn(true)
      whenever(leaveRequestRepository.findAllByApproverId(alice.id)).thenReturn(emptyList())

      val results = service.getAssignedRequests(alice.id)

      assertThat(results).isEmpty()
    }

    @Test
    fun `should throw UserNotFoundException when user does not exist`() {
      val unknownId = UUID.randomUUID()
      whenever(userRepository.existsById(unknownId)).thenReturn(false)

      assertThatThrownBy { service.getAssignedRequests(unknownId) }
        .isInstanceOf(UserNotFoundException::class.java)
        .hasMessageContaining(unknownId.toString())
    }
  }

  @Nested
  @DisplayName("decideRequest()")
  inner class DecideRequest {

    @BeforeEach
    fun setUp() {
      whenever(leaveRequestRepository.findById(alicePendingRequest.id)).thenReturn(Optional.of(alicePendingRequest))
      whenever(userRepository.findById(alice.id)).thenReturn(Optional.of(alice))
      whenever(leaveRequestRepository.save(any<LeaveRequest>())).thenAnswer { it.arguments[0] }
    }

    @Test
    fun `should approve a pending request when manager is the approver`() {
      val decision = DecisionRequest(status = Status.APPROVED, approverNote = "Enjoy!")

      val result = service.decideRequest(bob.id, alicePendingRequest.id, decision)

      assertThat(result.status).isEqualTo(Status.APPROVED)
      assertThat(result.approverNote).isEqualTo("Enjoy!")
      assertThat(result.decisionAt).isNotNull()
      verify(leaveRequestRepository).save(any<LeaveRequest>())
    }

    @Test
    fun `should reject a pending request when manager is the approver`() {
      val decision = DecisionRequest(status = Status.REJECTED, approverNote = "Team is busy")

      val result = service.decideRequest(bob.id, alicePendingRequest.id, decision)

      assertThat(result.status).isEqualTo(Status.REJECTED)
      assertThat(result.approverNote).isEqualTo("Team is busy")
      assertThat(result.decisionAt).isNotNull()
    }

    @Test
    fun `should throw ValidationException when decision status is PENDING`() {
      val decision = DecisionRequest(status = Status.PENDING)

      assertThatThrownBy { service.decideRequest(bob.id, alicePendingRequest.id, decision) }
        .isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("APPROVED or REJECTED")
    }

    @Test
    fun `should throw LeaveRequestNotFoundException when request does not exist`() {
      val unknownId = UUID.randomUUID()
      whenever(leaveRequestRepository.findById(unknownId)).thenReturn(Optional.empty())

      val decision = DecisionRequest(status = Status.APPROVED)

      assertThatThrownBy { service.decideRequest(bob.id, unknownId, decision) }
        .isInstanceOf(LeaveRequestNotFoundException::class.java)
        .hasMessageContaining(unknownId.toString())
    }

    @Test
    fun `should throw ValidationException when request is already decided`() {
      whenever(leaveRequestRepository.findById(aliceApprovedRequest.id)).thenReturn(Optional.of(aliceApprovedRequest))

      val decision = DecisionRequest(status = Status.REJECTED)

      assertThatThrownBy { service.decideRequest(bob.id, aliceApprovedRequest.id, decision) }
        .isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("pending")
    }

    @Test
    fun `should throw ForbiddenException when user is not the assigned approver`() {
      val randomUser = UUID.randomUUID()
      val decision = DecisionRequest(status = Status.APPROVED)

      assertThatThrownBy { service.decideRequest(randomUser, alicePendingRequest.id, decision) }
        .isInstanceOf(ForbiddenException::class.java)
        .hasMessageContaining("not the assigned approver")
    }

    @Test
    fun `should throw ForbiddenException when user is approver but no longer manager of creator`() {
      val reassignedAlice = alice.copy(managerId = UUID.randomUUID())
      whenever(userRepository.findById(alice.id)).thenReturn(Optional.of(reassignedAlice))

      val decision = DecisionRequest(status = Status.APPROVED)

      assertThatThrownBy { service.decideRequest(bob.id, alicePendingRequest.id, decision) }
        .isInstanceOf(ForbiddenException::class.java)
        .hasMessageContaining("not the manager")
    }
  }

  @Nested
  @DisplayName("markDecisionSeen()")
  inner class MarkDecisionSeen {

    @BeforeEach
    fun setUp() {
      whenever(leaveRequestRepository.save(any<LeaveRequest>())).thenAnswer { it.arguments[0] }
    }

    @Test
    fun `should set decisionSeenAt when request has a decision`() {
      whenever(leaveRequestRepository.findById(aliceApprovedRequest.id)).thenReturn(Optional.of(aliceApprovedRequest))

      val result = service.markDecisionSeen(alice.id, aliceApprovedRequest.id)

      assertThat(result.decisionSeenAt).isNotNull()
      verify(leaveRequestRepository).save(any<LeaveRequest>())
    }

    @Test
    fun `should throw ValidationException when decision was already seen`() {
      val alreadySeen = aliceApprovedRequest.copy(
        decisionSeenAt = LocalDateTime.of(2026, 5, 17, 8, 0),
      )
      whenever(leaveRequestRepository.findById(alreadySeen.id)).thenReturn(Optional.of(alreadySeen))

      assertThatThrownBy { service.markDecisionSeen(alice.id, alreadySeen.id) }
        .isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("Decision already seen on")
    }

    @Test
    fun `should throw ValidationException when request has no decision`() {
      whenever(leaveRequestRepository.findById(alicePendingRequest.id)).thenReturn(Optional.of(alicePendingRequest))

      assertThatThrownBy { service.markDecisionSeen(alice.id, alicePendingRequest.id) }
        .isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("No decision has been made")
    }

    @Test
    fun `should throw ForbiddenException when user is not the creator`() {
      whenever(leaveRequestRepository.findById(aliceApprovedRequest.id)).thenReturn(Optional.of(aliceApprovedRequest))

      assertThatThrownBy { service.markDecisionSeen(bob.id, aliceApprovedRequest.id) }
        .isInstanceOf(ForbiddenException::class.java)
        .hasMessageContaining("your own")
    }

    @Test
    fun `should throw LeaveRequestNotFoundException when request does not exist`() {
      val unknownId = UUID.randomUUID()
      whenever(leaveRequestRepository.findById(unknownId)).thenReturn(Optional.empty())

      assertThatThrownBy { service.markDecisionSeen(alice.id, unknownId) }
        .isInstanceOf(LeaveRequestNotFoundException::class.java)
        .hasMessageContaining(unknownId.toString())
    }
  }
}
