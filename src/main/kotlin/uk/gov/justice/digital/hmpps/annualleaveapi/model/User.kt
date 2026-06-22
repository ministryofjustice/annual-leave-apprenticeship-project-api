package uk.gov.justice.digital.hmpps.annualleaveapi.model

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

  @Column(name = "first_name", nullable = false)
  val firstName: String,

  @Column(name = "last_name", nullable = false)
  val lastName: String,

  @Column(nullable = false, unique = true)
  val email: String,

  @Column(nullable = false)
  val password: String,

  @Column(name = "manager_id")
  val managerId: UUID? = null,

  @Column(name = "annual_entitlement", nullable = false)
  val annualEntitlement: Int,

  @Column(name = "is_manager", nullable = false)
  val isManager: Boolean = false,
)
