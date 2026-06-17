package uk.gov.justice.digital.hmpps.templatepackagename.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.templatepackagename.model.User
import java.util.UUID

@Repository
interface UserRepository : JpaRepository<User, UUID> {

  fun findByEmail(email: String): User?
}
