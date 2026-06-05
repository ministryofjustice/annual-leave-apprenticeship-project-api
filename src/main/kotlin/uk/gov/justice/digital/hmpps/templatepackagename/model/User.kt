package uk.gov.justice.digital.hmpps.templatepackagename.model

import java.util.UUID

// TODO: once auth is added, password will need to be hashed
data class User(
  val id: UUID,
  val name: String,
  val email: String,
  val password: String,
  val managerId: UUID? = null,
  val annualEntitlement: Int,
)
