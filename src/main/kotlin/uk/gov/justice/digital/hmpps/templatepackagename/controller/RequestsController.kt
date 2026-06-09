package uk.gov.justice.digital.hmpps.templatepackagename.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.templatepackagename.config.ErrorResponse
import uk.gov.justice.digital.hmpps.templatepackagename.controller.response.LeaveRequestResponse
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
  fun getMyRequests(@RequestHeader("X-User-Id") userId: UUID): LeaveRequestResponse =
    LeaveRequestResponse(userRequests = leaveRequestService.getRequestsByUser(userId))
}
