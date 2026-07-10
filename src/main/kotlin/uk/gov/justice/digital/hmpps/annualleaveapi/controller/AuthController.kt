package uk.gov.justice.digital.hmpps.annualleaveapi.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.annualleaveapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.annualleaveapi.controller.request.LoginRequest
import uk.gov.justice.digital.hmpps.annualleaveapi.controller.response.UserResponse
import uk.gov.justice.digital.hmpps.annualleaveapi.service.AuthService

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
}
