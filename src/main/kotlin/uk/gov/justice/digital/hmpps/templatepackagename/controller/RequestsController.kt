package uk.gov.justice.digital.hmpps.templatepackagename.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.templatepackagename.config.ErrorResponse
import uk.gov.justice.digital.hmpps.templatepackagename.controller.request.CreateLeaveRequestBody
import uk.gov.justice.digital.hmpps.templatepackagename.controller.request.DecisionRequest
import uk.gov.justice.digital.hmpps.templatepackagename.controller.response.LeaveRequestResponse
import uk.gov.justice.digital.hmpps.templatepackagename.model.LeaveRequest
import uk.gov.justice.digital.hmpps.templatepackagename.service.LeaveRequestService
import java.util.UUID

@RestController
@Tag(name = "Requests")
@RequestMapping("/requests", produces = ["application/json"])
class RequestsController(
  private val leaveRequestService: LeaveRequestService,
) {

  @GetMapping
  @Operation(description = "Get all the leave requests for user")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Leave requests found",
        content = [Content(schema = Schema(implementation = LeaveRequestResponse::class))],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "User not found",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "Unexpected error",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getMyRequests(@RequestHeader("X-User-Id") userId: UUID): LeaveRequestResponse = LeaveRequestResponse(userRequests = leaveRequestService.getRequestsByUser(userId))

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(description = "Submit a new leave request")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Leave request created",
        content = [Content(schema = Schema(implementation = LeaveRequest::class))],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "User not found",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "Unexpected error",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun createRequest(
    @RequestHeader("X-User-Id") userId: UUID,
    @RequestBody request: CreateLeaveRequestBody,
  ): LeaveRequest = leaveRequestService.createRequest(userId, request)

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(description = "Delete a pending leave request")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "204", description = "Leave request deleted"),
      ApiResponse(
        responseCode = "400",
        description = "Bad request",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Leave request not found",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "Unexpected error",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun deleteRequest(
    @RequestHeader("X-User-Id") userId: UUID,
    @PathVariable id: UUID,
  ) = leaveRequestService.deleteRequest(userId, id)

  @GetMapping("/assigned")
  @Operation(description = "Get all leave requests assigned to the user as approver")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Assigned requests found",
        content = [Content(schema = Schema(implementation = LeaveRequestResponse::class))],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "User not found",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "Unexpected error",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getAssignedRequests(@RequestHeader("X-User-Id") userId: UUID): LeaveRequestResponse = LeaveRequestResponse(userRequests = leaveRequestService.getAssignedRequests(userId))

  @PatchMapping("/assigned/{id}")
  @Operation(description = "Approve or reject an assigned leave request")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Decision recorded",
        content = [Content(schema = Schema(implementation = LeaveRequest::class))],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Leave request not found",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "Unexpected error",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun decideRequest(
    @RequestHeader("X-User-Id") userId: UUID,
    @PathVariable id: UUID,
    @RequestBody decision: DecisionRequest,
  ): LeaveRequest = leaveRequestService.decideRequest(userId, id, decision)
}
