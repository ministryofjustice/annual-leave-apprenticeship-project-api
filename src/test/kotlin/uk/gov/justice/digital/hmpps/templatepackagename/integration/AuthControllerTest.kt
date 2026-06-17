package uk.gov.justice.digital.hmpps.templatepackagename.integration

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.templatepackagename.controller.response.UserResponse
import java.util.UUID

class AuthControllerTest : IntegrationTestBase() {

  private val aliceIdString = "00000000-0000-0000-0000-000000000001"
  private val seedPassword = "password"
  private val nonExistentUserIdString = "00000000-0000-0000-0000-999999999999"
  private val userAlice = UserResponse(
    id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
    name = "Alice Johnson",
    email = "alice@example.com",
    annualEntitlement = 25,
    managerId = UUID.fromString("00000000-0000-0000-0000-000000000002"),
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
        .jsonPath("$.name").isEqualTo(userAlice.name)
        .jsonPath("$.email").isEqualTo(userAlice.email)
        .jsonPath("$.annualEntitlement").isEqualTo(userAlice.annualEntitlement)
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

  @Nested
  @DisplayName("GET /auth/me")
  inner class Me {

    @Test
    fun `should return the user's details`() {
      webTestClient.get()
        .uri("/auth/me")
        .header("X-User-Id", aliceIdString)
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.id").isEqualTo(userAlice.id)
        .jsonPath("$.name").isEqualTo(userAlice.name)
        .jsonPath("$.email").isEqualTo(userAlice.email)
        .jsonPath("$.annualEntitlement").isEqualTo(userAlice.annualEntitlement)
    }

    @Test
    fun `should not expose the password field`() {
      webTestClient.get()
        .uri("/auth/me")
        .header("X-User-Id", aliceIdString)
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.password").doesNotExist()
    }

    @Test
    fun `should return 400 when X-User-Id header is missing`() {
      webTestClient.get()
        .uri("/auth/me")
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    fun `should return 404 when user does not exist`() {
      webTestClient.get()
        .uri("/auth/me")
        .header("X-User-Id", nonExistentUserIdString)
        .exchange()
        .expectStatus().isNotFound
        .expectBody()
        .jsonPath("$.status").isEqualTo(404)
    }
  }
}
