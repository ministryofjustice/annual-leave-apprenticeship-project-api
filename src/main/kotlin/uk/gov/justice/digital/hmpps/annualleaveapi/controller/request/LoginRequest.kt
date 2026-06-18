package uk.gov.justice.digital.hmpps.annualleaveapi.controller.request

data class LoginRequest(
  val email: String,
  val password: String,
)
