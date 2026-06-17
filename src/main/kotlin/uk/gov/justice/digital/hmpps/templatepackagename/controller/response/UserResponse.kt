package uk.gov.justice.digital.hmpps.templatepackagename.controller.response

import java.util.UUID

data class UserResponse(
  val id: UUID,
  val name: String,
  val email: String,
  val managerId: UUID?,
  val annualEntitlement: Int,
)
