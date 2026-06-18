package uk.gov.justice.digital.hmpps.templatepackagename.service

import jakarta.validation.ValidationException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.templatepackagename.config.ForbiddenException
import uk.gov.justice.digital.hmpps.templatepackagename.config.LeaveRequestNotFoundException
import uk.gov.justice.digital.hmpps.templatepackagename.config.UserNotFoundException
import uk.gov.justice.digital.hmpps.templatepackagename.controller.request.CreateLeaveRequestBody
import uk.gov.justice.digital.hmpps.templatepackagename.controller.request.DecisionRequest
import uk.gov.justice.digital.hmpps.templatepackagename.controller.response.BalanceResponse
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

  fun getBalance(userId: UUID): BalanceResponse {
    val user = userRepository.findById(userId).orElseThrow { UserNotFoundException(userId) }

    val activeRequests = leaveRequestRepository.findAllByCreatorIdAndStatusIn(
      userId,
      listOf(Status.PENDING, Status.APPROVED),
    )

    val pendingDays = activeRequests.filter { it.status == Status.PENDING }.sumOf { it.duration }
    val approvedDays = activeRequests.filter { it.status == Status.APPROVED }.sumOf { it.duration }

    return BalanceResponse(
      annualEntitlement = user.annualEntitlement,
      availableBalance = user.annualEntitlement - (pendingDays + approvedDays),
      actualBalance = user.annualEntitlement - approvedDays,
      pendingDays = pendingDays,
      approvedDays = approvedDays,
    )
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

  fun deleteRequest(userId: UUID, requestId: UUID) {
    val request = leaveRequestRepository.findById(requestId)
      .orElseThrow { LeaveRequestNotFoundException(requestId) }

    if (request.creatorId != userId) {
      throw ForbiddenException("You can only delete your own leave requests")
    }

    if (request.status != Status.PENDING) {
      throw ValidationException("Only pending leave requests can be deleted")
    }

    leaveRequestRepository.delete(request)
  }

  fun getAssignedRequests(managerId: UUID): List<LeaveRequest> {
    if (!userRepository.existsById(managerId)) {
      throw UserNotFoundException(managerId)
    }

    return leaveRequestRepository.findAllByApproverId(managerId)
  }

  fun decideRequest(managerId: UUID, requestId: UUID, decision: DecisionRequest): LeaveRequest {
    if (decision.status == Status.PENDING) {
      throw ValidationException("Decision must be APPROVED or REJECTED")
    }

    val request = leaveRequestRepository.findById(requestId)
      .orElseThrow { LeaveRequestNotFoundException(requestId) }

    if (request.status != Status.PENDING) {
      throw ValidationException("Only pending leave requests can be approved or rejected")
    }

    if (request.approverId != managerId) {
      throw ForbiddenException("You are not the assigned approver for this request")
    }

    val creator = userRepository.findById(request.creatorId)
      .orElseThrow { UserNotFoundException(request.creatorId) }

    if (creator.managerId != managerId) {
      throw ForbiddenException("You are not the manager of the request creator")
    }

    val updatedRequest = request.copy(
      status = decision.status,
      decisionAt = LocalDateTime.now(),
      approverNote = decision.approverNote,
    )

    return leaveRequestRepository.save(updatedRequest)
  }
}
