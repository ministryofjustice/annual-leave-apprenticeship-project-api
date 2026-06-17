package uk.gov.justice.digital.hmpps.templatepackagename.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.templatepackagename.config.InvalidCredentialsException
import uk.gov.justice.digital.hmpps.templatepackagename.config.UserNotFoundException
import uk.gov.justice.digital.hmpps.templatepackagename.config.UserNotRegisteredException
import uk.gov.justice.digital.hmpps.templatepackagename.controller.response.UserResponse
import uk.gov.justice.digital.hmpps.templatepackagename.model.User
import uk.gov.justice.digital.hmpps.templatepackagename.repository.UserRepository
import java.util.UUID

@Service
class AuthService(
  private val userRepository: UserRepository,
) {

  fun login(email: String, password: String): UserResponse {
    val user = userRepository.findByEmail(email)
      ?: throw UserNotRegisteredException()

    if (user.password != password) {
      throw InvalidCredentialsException()
    }

    return toUserResponse(user)
  }

  fun getUserById(userId: UUID): UserResponse {
    val user = userRepository.findById(userId)
      .orElseThrow { UserNotFoundException(userId) }

    return toUserResponse(user)
  }

  private fun toUserResponse(user: User): UserResponse = UserResponse(
    id = user.id,
    name = user.name,
    email = user.email,
    managerId = user.managerId,
    annualEntitlement = user.annualEntitlement,
  )
}
