package uk.gov.justice.digital.hmpps.templatepackagename.service

import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.templatepackagename.config.UserNotFoundException
import uk.gov.justice.digital.hmpps.templatepackagename.controller.request.CreateLeaveRequestBody
import uk.gov.justice.digital.hmpps.templatepackagename.data.SeedData
import uk.gov.justice.digital.hmpps.templatepackagename.model.Status
import java.time.LocalDate
import java.util.UUID

class LeaveRequestServiceTest {

  private val service = LeaveRequestService()

  private val seedRequestIds = SeedData.leaveRequests.map { it.id }.toSet()
  private val aliceRequestCount = SeedData.leaveRequests.count { it.creatorId == SeedData.userAlice.id }

  @AfterEach
  fun cleanUp() {
    SeedData.leaveRequests.removeIf { it.id !in seedRequestIds }
  }

  @Nested
  @DisplayName("getRequestsByUser()")
  inner class GetRequestsByUser {

    @Test
    fun `should return requests for a user who has them`() {
      val results = service.getRequestsByUser(SeedData.userAlice.id)

      assertThat(results).hasSize(aliceRequestCount)
      assertThat(results).allMatch { it.creatorId == SeedData.userAlice.id }
    }

    @Test
    fun `should return empty list when user has no requests`() {
      val results = service.getRequestsByUser(SeedData.userBob.id)

      assertThat(results).isEmpty()
    }

    @Test
    fun `should throw UserNotFoundException when user does not exist`() {
      val unknownId = UUID.randomUUID()

      assertThatThrownBy { service.getRequestsByUser(unknownId) }
        .isInstanceOf(UserNotFoundException::class.java)
        .hasMessageContaining(unknownId.toString())
    }
  }

  @Nested
  @DisplayName("createRequest()")
  inner class CreateRequest {

    // Uses dates that don't overlap with Alice's seed data (Jun 10-14, Jul 1-3)
    private val request = CreateLeaveRequestBody(
      startDate = LocalDate.of(2026, 8, 3),
      endDate = LocalDate.of(2026, 8, 7),
      isFirstDayHalfDay = false,
      isLastDayHalfDay = true,
      creatorNote = "Holiday",
    )

    @Test
    fun `should create a leave request with calculated duration`() {
      val result = service.createRequest(SeedData.userAlice.id, request)

      assertThat(result.creatorId).isEqualTo(SeedData.userAlice.id)
      assertThat(result.approverId).isEqualTo(SeedData.userAlice.managerId)
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
    }

    @Test
    fun `should throw UserNotFoundException when user does not exist`() {
      val unknownId = UUID.randomUUID()

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

      assertThatThrownBy { service.createRequest(SeedData.userAlice.id, badRequest) }
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

      assertThatThrownBy { service.createRequest(SeedData.userAlice.id, singleDayBothHalves) }
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

      assertThatThrownBy { service.createRequest(SeedData.userAlice.id, weekendOnly) }
        .isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("duration must be greater than 0")
    }

    @Test
    fun `should throw ValidationException when new request overlaps end of existing request`() {
      // Alice has a PENDING request for Jun 10-14
      // New: Jun 12-16 overlaps the tail end
      val overlapping = request.copy(
        startDate = LocalDate.of(2026, 6, 12),
        endDate = LocalDate.of(2026, 6, 16),
      )

      assertThatThrownBy { service.createRequest(SeedData.userAlice.id, overlapping) }
        .isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("overlap")
    }

    @Test
    fun `should throw ValidationException when new request overlaps start of existing request`() {
      // New: Jun 8-11 overlaps the beginning
      val overlapping = request.copy(
        startDate = LocalDate.of(2026, 6, 8),
        endDate = LocalDate.of(2026, 6, 11),
      )

      assertThatThrownBy { service.createRequest(SeedData.userAlice.id, overlapping) }
        .isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("overlap")
    }

    @Test
    fun `should throw ValidationException when new request is fully contained within existing request`() {
      // New: Jun 11-12 sits entirely inside Jun 10-14
      val overlapping = request.copy(
        startDate = LocalDate.of(2026, 6, 11),
        endDate = LocalDate.of(2026, 6, 12),
      )

      assertThatThrownBy { service.createRequest(SeedData.userAlice.id, overlapping) }
        .isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("overlap")
    }

    @Test
    fun `should throw ValidationException when new request fully contains existing request`() {
      // New: Jun 8-16 wraps around Jun 10-14
      val overlapping = request.copy(
        startDate = LocalDate.of(2026, 6, 8),
        endDate = LocalDate.of(2026, 6, 16),
      )

      assertThatThrownBy { service.createRequest(SeedData.userAlice.id, overlapping) }
        .isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("overlap")
    }

    @Test
    fun `should throw ValidationException when new request has exact same dates as existing request`() {
      // New: Jun 10-14, exactly matching the existing request
      val overlapping = request.copy(
        startDate = LocalDate.of(2026, 6, 10),
        endDate = LocalDate.of(2026, 6, 14),
      )

      assertThatThrownBy { service.createRequest(SeedData.userAlice.id, overlapping) }
        .isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("overlap")
    }

    @Test
    fun `should allow dates that don't overlap with existing requests`() {
      // Bob has no existing requests, so any dates are fine
      val bobRequest = request.copy(
        startDate = LocalDate.of(2026, 6, 10),
        endDate = LocalDate.of(2026, 6, 12),
      )

      val result = service.createRequest(SeedData.userBob.id, bobRequest)

      assertThat(result.creatorId).isEqualTo(SeedData.userBob.id)
      assertThat(result.duration).isEqualTo(2.5)
    }
  }
}
