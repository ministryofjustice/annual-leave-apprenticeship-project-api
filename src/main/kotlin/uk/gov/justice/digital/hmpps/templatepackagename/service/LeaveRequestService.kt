package uk.gov.justice.digital.hmpps.templatepackagename.service

import jakarta.validation.ValidationException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.templatepackagename.config.UserNotFoundException
import uk.gov.justice.digital.hmpps.templatepackagename.controller.request.CreateLeaveRequestBody
import uk.gov.justice.digital.hmpps.templatepackagename.model.LeaveRequest
import uk.gov.justice.digital.hmpps.templatepackagename.model.Status
import uk.gov.justice.digital.hmpps.templatepackagename.repository.LeaveRequestRepository
import uk.gov.justice.digital.hmpps.templatepackagename.repository.UserRepository
import java.time.LocalDateTime
import java.util.UUID

@Service
class LeaveRequestService(
  private val leaveRequestRepository: LeaveRequestRepository,
  private val userRepository: UserRepository,
) {

  fun getRequestsByUser(userId: UUID): List<LeaveRequest> {
    if (!userRepository.existsById(userId)) {
      throw UserNotFoundException(userId)
    }

    return leaveRequestRepository.findAllByCreatorId(userId)
  }

  fun createRequest(userId: UUID, request: CreateLeaveRequestBody): LeaveRequest {
    val user = userRepository.findById(userId).orElseThrow { UserNotFoundException(userId) }

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

    val activeRequests = leaveRequestRepository.findAllByCreatorIdAndStatusIn(
      userId,
      listOf(Status.PENDING, Status.APPROVED),
    )
    val hasOverlap = activeRequests.any {
      !request.startDate.isAfter(it.endDate) && !request.endDate.isBefore(it.startDate)
    }

    if (hasOverlap) {
      throw ValidationException("Leave dates overlap with an existing request")
    }

    val usedEntitlement = activeRequests.sumOf { it.duration }
    if (usedEntitlement + duration > user.annualEntitlement) {
      throw ValidationException(
        "Insufficient annual entitlement: ${user.annualEntitlement - usedEntitlement} days remaining, but $duration days requested",
      )
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

    return leaveRequestRepository.save(leaveRequest)
  }
}
