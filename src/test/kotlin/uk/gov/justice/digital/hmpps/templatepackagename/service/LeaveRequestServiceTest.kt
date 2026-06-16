package uk.gov.justice.digital.hmpps.templatepackagename.service

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
import uk.gov.justice.digital.hmpps.templatepackagename.config.UserNotFoundException
import uk.gov.justice.digital.hmpps.templatepackagename.controller.request.CreateLeaveRequestBody
import uk.gov.justice.digital.hmpps.templatepackagename.model.LeaveRequest
import uk.gov.justice.digital.hmpps.templatepackagename.model.Status
import uk.gov.justice.digital.hmpps.templatepackagename.model.User
import uk.gov.justice.digital.hmpps.templatepackagename.repository.LeaveRequestRepository
import uk.gov.justice.digital.hmpps.templatepackagename.repository.UserRepository
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
    name = "Alice Johnson",
    email = "alice@example.com",
    password = "password",
    managerId = UUID.fromString("00000000-0000-0000-0000-000000000002"),
    annualEntitlement = 25,
  )

  private val bob = User(
    id = UUID.fromString("00000000-0000-0000-0000-000000000002"),
    name = "Bob Smith",
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
    endDate = LocalDate.of(2026, 6, 14),
    duration = 4.5,
    isFirstDayHalfDay = false,
    isLastDayHalfDay = true,
    status = Status.PENDING,
    creatorNote = "Family holiday",
  )

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
    fun `should throw ValidationException when dates fall entirely on a weekend`() {
      val weekendOnly = request.copy(
        startDate = LocalDate.of(2026, 8, 8),
        endDate = LocalDate.of(2026, 8, 9),
        isFirstDayHalfDay = false,
        isLastDayHalfDay = false,
      )

      assertThatThrownBy { service.createRequest(alice.id, weekendOnly) }
        .isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("duration must be greater than 0")
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
        endDate = LocalDate.of(2026, 6, 14),
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
}
