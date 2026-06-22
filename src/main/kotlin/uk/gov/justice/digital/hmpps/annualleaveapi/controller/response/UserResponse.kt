package uk.gov.justice.digital.hmpps.annualleaveapi.controller.response

import java.util.UUID

data class UserResponse(
  val id: UUID,
  val firstName: String,
  val lastName: String,
  val email: String,
  val managerId: UUID?,
  val annualEntitlement: Int,
  val isManager: Boolean,
)
