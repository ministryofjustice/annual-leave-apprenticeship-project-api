package uk.gov.justice.digital.hmpps.annualleaveapi.controller

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
import uk.gov.justice.digital.hmpps.annualleaveapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.annualleaveapi.controller.response.BalanceResponse
import uk.gov.justice.digital.hmpps.annualleaveapi.service.LeaveRequestService
import java.util.UUID

@RestController
@Tag(name = "Balance")
@RequestMapping("/balance", produces = ["application/json"])
class BalanceController(
  private val leaveRequestService: LeaveRequestService,
) {

  @GetMapping
  @Operation(description = "Get the leave balance for user")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Balance retrieved",
        content = [Content(schema = Schema(implementation = BalanceResponse::class))],
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
  fun getBalance(@RequestHeader("X-User-Id") userId: UUID): BalanceResponse = leaveRequestService.getBalance(userId)
}
