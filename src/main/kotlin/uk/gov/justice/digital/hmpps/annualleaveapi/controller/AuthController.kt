package uk.gov.justice.digital.hmpps.annualleaveapi.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.annualleaveapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.annualleaveapi.controller.request.LoginRequest
import uk.gov.justice.digital.hmpps.annualleaveapi.controller.response.UserResponse
import uk.gov.justice.digital.hmpps.annualleaveapi.service.AuthService
import java.util.UUID

@RestController
@Tag(name = "Auth")
@RequestMapping("/auth", produces = ["application/json"])
class AuthController(
  private val authService: AuthService,
) {

  @PostMapping("/login")
  @Operation(description = "Validate email and password, returns user details if valid")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Login successful",
        content = [Content(schema = Schema(implementation = UserResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Invalid credentials",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun login(@RequestBody request: LoginRequest): UserResponse = authService.login(request.email, request.password)

  @GetMapping("/me")
  @Operation(description = "Get the current user's details")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "User details returned",
        content = [Content(schema = Schema(implementation = UserResponse::class))],
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
    ],
  )
  fun me(@RequestHeader("X-User-Id") userId: UUID): UserResponse = authService.getUserById(userId)
}
