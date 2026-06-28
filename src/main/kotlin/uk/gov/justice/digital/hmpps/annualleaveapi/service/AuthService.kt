package uk.gov.justice.digital.hmpps.annualleaveapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.annualleaveapi.config.InvalidCredentialsException
import uk.gov.justice.digital.hmpps.annualleaveapi.config.UserNotFoundException
import uk.gov.justice.digital.hmpps.annualleaveapi.config.UserNotRegisteredException
import uk.gov.justice.digital.hmpps.annualleaveapi.controller.response.UserResponse
import uk.gov.justice.digital.hmpps.annualleaveapi.model.User
import uk.gov.justice.digital.hmpps.annualleaveapi.repository.UserRepository
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

  private fun getManagerName(user: User): String {
    val managerId = user.managerId ?: return ""
    val manager = userRepository.findById(managerId)
      .orElseThrow { (UserNotFoundException(user.managerId)) }
    return manager.firstName + " " + manager.lastName
  }

  fun getUserById(userId: UUID?): UserResponse {
    if (userId == null) {
      throw UserNotRegisteredException()
    }
    val user = userRepository.findById(userId)
      .orElseThrow { UserNotFoundException(userId) }

    return toUserResponse(user)
  }

  private fun toUserResponse(user: User): UserResponse = UserResponse(
    id = user.id,
    firstName = user.firstName,
    lastName = user.lastName,
    email = user.email,
    managerId = user.managerId,
    managerName = getManagerName(user),
    annualEntitlement = user.annualEntitlement,
    isManager = user.isManager,
  )
}
