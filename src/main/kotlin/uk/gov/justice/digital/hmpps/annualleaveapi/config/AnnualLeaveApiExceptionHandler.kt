package uk.gov.justice.digital.hmpps.annualleaveapi.config

import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException

data class ErrorResponse(
  val status: Int,
  val userMessage: String?,
  val developerMessage: String?,
)

@RestControllerAdvice
class AnnualLeaveApiExceptionHandler {
  @ExceptionHandler(ValidationException::class)
  fun handleValidationException(e: ValidationException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(BAD_REQUEST)
    .body(
      ErrorResponse(
        status = BAD_REQUEST.value(),
        userMessage = "Validation failure: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.info("Validation exception: {}", e.message) }

  @ExceptionHandler(NoResourceFoundException::class)
  fun handleNoResourceFoundException(e: NoResourceFoundException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(NOT_FOUND)
    .body(
      ErrorResponse(
        status = NOT_FOUND.value(),
        userMessage = "No resource found failure: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.info("No resource found exception: {}", e.message) }

  @ExceptionHandler(LeaveRequestNotFoundException::class)
  fun handleLeaveRequestNotFoundException(e: LeaveRequestNotFoundException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(NOT_FOUND)
    .body(
      ErrorResponse(
        status = NOT_FOUND.value(),
        userMessage = "${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.info("Leave request not found: {}", e.message) }

  @ExceptionHandler(ForbiddenException::class)
  fun handleForbiddenException(e: ForbiddenException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(FORBIDDEN)
    .body(
      ErrorResponse(
        status = FORBIDDEN.value(),
        userMessage = "${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.info("Forbidden: {}", e.message) }

  @ExceptionHandler(UserNotFoundException::class)
  fun handleUserNotFoundException(e: UserNotFoundException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(NOT_FOUND)
    .body(
      ErrorResponse(
        status = NOT_FOUND.value(),
        userMessage = "${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.info("User not found: {}", e.message) }

  @ExceptionHandler(InvalidCredentialsException::class)
  fun handleInvalidCredentialsException(e: InvalidCredentialsException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(UNAUTHORIZED)
    .body(
      ErrorResponse(
        status = UNAUTHORIZED.value(),
        userMessage = "${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.info("Invalid credentials: {}", e.message) }

  @ExceptionHandler(UserNotRegisteredException::class)
  fun handleUserNotRegisteredException(e: UserNotRegisteredException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(UNAUTHORIZED)
    .body(
      ErrorResponse(
        status = UNAUTHORIZED.value(),
        userMessage = "${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.info("User not registered: {}", e.message) }

  @ExceptionHandler(MissingRequestHeaderException::class)
  fun handleMissingHeaderException(e: MissingRequestHeaderException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(BAD_REQUEST)
    .body(
      ErrorResponse(
        status = BAD_REQUEST.value(),
        userMessage = "Missing required header: ${e.headerName}",
        developerMessage = e.message,
      ),
    ).also { log.info("Missing header: {}", e.headerName) }

  @ExceptionHandler(MethodArgumentTypeMismatchException::class)
  fun handleTypeMismatchException(e: MethodArgumentTypeMismatchException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(BAD_REQUEST)
    .body(
      ErrorResponse(
        status = BAD_REQUEST.value(),
        userMessage = "Invalid value for parameter: ${e.name}",
        developerMessage = e.message,
      ),
    ).also { log.info("Type mismatch: {}", e.message) }

  @ExceptionHandler(Exception::class)
  fun handleException(e: Exception): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(INTERNAL_SERVER_ERROR)
    .body(
      ErrorResponse(
        status = INTERNAL_SERVER_ERROR.value(),
        userMessage = "Unexpected error: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { log.error("Unexpected exception", e) }

  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
