package uk.gov.justice.digital.hmpps.templatepackagename.service

import jakarta.validation.ValidationException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.templatepackagename.config.UserNotFoundException
import uk.gov.justice.digital.hmpps.templatepackagename.controller.request.CreateLeaveRequestBody
import uk.gov.justice.digital.hmpps.templatepackagename.data.SeedData
import uk.gov.justice.digital.hmpps.templatepackagename.model.LeaveRequest
import uk.gov.justice.digital.hmpps.templatepackagename.model.Status
import java.time.LocalDateTime
import java.util.UUID

@Service
class LeaveRequestService {

  // TODO: temp in-memory storage, will be replaced with a repository later
  private val requests = SeedData.leaveRequests
  private val users = SeedData.users

  fun getRequestsByUser(userId: UUID): List<LeaveRequest> {
    if (users.none { it.id == userId }) {
      throw UserNotFoundException(userId)
    }

    return requests.filter { it.creatorId == userId }
  }

  fun createRequest(userId: UUID, request: CreateLeaveRequestBody): LeaveRequest {
    val user = users.find { it.id == userId } ?: throw UserNotFoundException(userId)

    if (request.endDate.isBefore(request.startDate)) {
      throw ValidationException("End date must not be before start date")
    }

    val duration = LeaveCalculator.calculateDuration(
      startDate = request.startDate,
      endDate = request.endDate,
      isFirstDayHalfDay = request.isFirstDayHalfDay,
      isLastDayHalfDay = request.isLastDayHalfDay,
    )

    if (duration <= 0) {
      throw ValidationException("Leave duration must be greater than 0")
    }

    val activeRequests = requests.filter {
      it.creatorId == userId && it.status in listOf(Status.PENDING, Status.APPROVED)
    }
    val hasOverlap = activeRequests.any {
      !request.startDate.isAfter(it.endDate) && !request.endDate.isBefore(it.startDate)
    }

    if (hasOverlap) {
      throw ValidationException("Leave dates overlap with an existing request")
    }

    val leaveRequest = LeaveRequest(
      id = UUID.randomUUID(),
      createdAt = LocalDateTime.now(),
      creatorId = user.id,
      approverId = user.managerId,
      startDate = request.startDate,
      endDate = request.endDate,
      duration = duration,
      isFirstDayHalfDay = request.isFirstDayHalfDay,
      isLastDayHalfDay = request.isLastDayHalfDay,
      status = Status.PENDING,
      creatorNote = request.creatorNote,
    )

    requests.add(leaveRequest)

    return leaveRequest
  }
}
