package uk.gov.justice.digital.hmpps.annualleaveapi.integration

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.annualleaveapi.controller.response.UserResponse
import java.util.UUID

class AuthControllerTest : IntegrationTestBase() {

  private val seedPassword = "password"
  private val userAlice = UserResponse(
    id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
    firstName = "Alice",
    lastName = "Johnson",
    email = "alice@example.com",
    annualEntitlement = 25,
    managerId = UUID.fromString("00000000-0000-0000-0000-000000000002"),
    managerName = "Bob Smith",
    isManager = false,
  )

  @Nested
  @DisplayName("POST /auth/login")
  inner class Login {

    @Test
    fun `should return user details for valid credentials`() {
      webTestClient.post()
        .uri("/auth/login")
        .header("Content-Type", "application/json")
        .bodyValue("""{"email": "${userAlice.email}", "password": "$seedPassword"}""")
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.id").isEqualTo(userAlice.id)
        .jsonPath("$.firstName").isEqualTo(userAlice.firstName)
        .jsonPath("$.lastName").isEqualTo(userAlice.lastName)
        .jsonPath("$.email").isEqualTo(userAlice.email)
        .jsonPath("$.annualEntitlement").isEqualTo(userAlice.annualEntitlement)
        .jsonPath("$.managerName").isEqualTo(userAlice.managerName)
    }

    @Test
    fun `should not expose the password field in login response`() {
      webTestClient.post()
        .uri("/auth/login")
        .header("Content-Type", "application/json")
        .bodyValue("""{"email": "${userAlice.email}", "password": "$seedPassword"}""")
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.password").doesNotExist()
    }

    @Test
    fun `should return 401 for wrong password`() {
      webTestClient.post()
        .uri("/auth/login")
        .header("Content-Type", "application/json")
        .bodyValue("""{"email": "${userAlice.email}", "password": "wrongPassword"}""")
        .exchange()
        .expectStatus().isUnauthorized
        .expectBody()
        .jsonPath("$.status").isEqualTo(401)
        .jsonPath("$.userMessage").isEqualTo("Invalid password")
    }

    @Test
    fun `should return 401 for non-existent email`() {
      webTestClient.post()
        .uri("/auth/login")
        .header("Content-Type", "application/json")
        .bodyValue("""{"email": "nobody@example.com", "password": "$seedPassword"}""")
        .exchange()
        .expectStatus().isUnauthorized
        .expectBody()
        .jsonPath("$.status").isEqualTo(401)
        .jsonPath("$.userMessage").isEqualTo("You are not registered for this service")
    }
  }
}
