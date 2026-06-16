package uk.gov.justice.digital.hmpps.templatepackagename.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

// TODO: once auth is added, password will need to be hashed
@Entity
@Table(name = "users")
data class User(
  @Id
  val id: UUID,

  @Column(nullable = false)
  val name: String,

  @Column(nullable = false, unique = true)
  val email: String,

  @Column(nullable = false)
  val password: String,

  @Column(name = "manager_id")
  val managerId: UUID? = null,

  @Column(name = "annual_entitlement", nullable = false)
  val annualEntitlement: Int,
)
