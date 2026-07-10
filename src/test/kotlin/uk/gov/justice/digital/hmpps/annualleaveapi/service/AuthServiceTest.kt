package uk.gov.justice.digital.hmpps.annualleaveapi.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.annualleaveapi.config.InvalidCredentialsException
import uk.gov.justice.digital.hmpps.annualleaveapi.config.UserNotFoundException
import uk.gov.justice.digital.hmpps.annualleaveapi.config.UserNotRegisteredException
import uk.gov.justice.digital.hmpps.annualleaveapi.model.User
import uk.gov.justice.digital.hmpps.annualleaveapi.repository.UserRepository
import java.util.Optional
import java.util.UUID

class AuthServiceTest {

  private val userRepository: UserRepository = mock()
  private val service = AuthService(userRepository)

  private val alice = User(
    id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
    firstName = "Alice",
    lastName = "Johnson",
    email = "alice@example.com",
    password = "password",
    managerId = UUID.fromString("00000000-0000-0000-0000-000000000002"),
    annualEntitlement = 25,
  )

  private val bob = User(
    id = UUID.fromString("00000000-0000-0000-0000-000000000002"),
    firstName = "Bob",
    lastName = "Smith",
    email = "bob@example.com",
    password = "password",
    managerId = null,
    annualEntitlement = 55,
  )

  @Nested
  @DisplayName("login()")
  inner class Login {

    @Test
    fun `should return user details for valid credentials`() {
      whenever(userRepository.findByEmail(alice.email)).thenReturn(alice)
      whenever(userRepository.findById(bob.id)).thenReturn(Optional.of(bob))

      val result = service.login(alice.email, alice.password)

      assertThat(result.id).isEqualTo(alice.id)
      assertThat(result.firstName).isEqualTo(alice.firstName)
      assertThat(result.lastName).isEqualTo(alice.lastName)
      assertThat(result.email).isEqualTo(alice.email)
      assertThat(result.managerId).isEqualTo(alice.managerId)
      assertThat(result.managerName).isEqualTo("Bob Smith")
      assertThat(result.annualEntitlement).isEqualTo(alice.annualEntitlement)
    }

    @Test
    fun `should return empty manager name when user has no manager`() {
      whenever(userRepository.findByEmail(bob.email)).thenReturn(bob)

      val result = service.login(bob.email, bob.password)

      assertThat(result.managerName).isEqualTo("")
    }

    @Test
    fun `should throw UserNotRegisteredException when email does not exist`() {
      whenever(userRepository.findByEmail("nobody@example.com")).thenReturn(null)

      assertThatThrownBy { service.login("nobody@example.com", "password") }
        .isInstanceOf(UserNotRegisteredException::class.java)
        .hasMessage("You are not registered for this service")
    }

    @Test
    fun `should throw InvalidCredentialsException when password is wrong`() {
      whenever(userRepository.findByEmail(alice.email)).thenReturn(alice)

      assertThatThrownBy { service.login(alice.email, "wrongpassword") }
        .isInstanceOf(InvalidCredentialsException::class.java)
        .hasMessage("Invalid password")
    }
  }

  @Nested
  @DisplayName("getUserById()")
  inner class GetUserById {

    @Test
    fun `should return user details when user exists`() {
      whenever(userRepository.findById(alice.id)).thenReturn(Optional.of(alice))
      whenever(userRepository.findById(bob.id)).thenReturn(Optional.of(bob))

      val result = service.getUserById(alice.id)

      assertThat(result.id).isEqualTo(alice.id)
      assertThat(result.firstName).isEqualTo(alice.firstName)
      assertThat(result.lastName).isEqualTo(alice.lastName)
      assertThat(result.email).isEqualTo(alice.email)
      assertThat(result.managerId).isEqualTo(alice.managerId)
      assertThat(result.managerName).isEqualTo("Bob Smith")
      assertThat(result.annualEntitlement).isEqualTo(alice.annualEntitlement)
    }

    @Test
    fun `should throw UserNotFoundException when user does not exist`() {
      val unknownId = UUID.fromString("00000000-0000-0000-0000-999999999999")
      whenever(userRepository.findById(unknownId)).thenReturn(Optional.empty())

      assertThatThrownBy { service.getUserById(unknownId) }
        .isInstanceOf(UserNotFoundException::class.java)
        .hasMessage("User not found: 00000000-0000-0000-0000-999999999999")
    }
  }
}
