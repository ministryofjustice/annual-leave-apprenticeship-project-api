package uk.gov.justice.digital.hmpps.annualleaveapi.integration

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.test.context.bean.override.mockito.MockitoBean
import uk.gov.justice.digital.hmpps.annualleaveapi.service.LeaveRequestService

class BalanceControllerTest : IntegrationTestBase() {

  private val aliceId = "00000000-0000-0000-0000-000000000001"
  private val bobId = "00000000-0000-0000-0000-000000000002"

  @Nested
  @DisplayName("GET /balance")
  inner class GetBalance {

    @Test
    fun `should return balance for user with pending and approved requests`() {
      webTestClient.get()
        .uri("/balance")
        .header("X-User-Id", aliceId)
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.annualEntitlement").isEqualTo(25)
        .jsonPath("$.pendingDays").isEqualTo(4.5)
        .jsonPath("$.approvedDays").isEqualTo(3.0)
        .jsonPath("$.availableBalance").isEqualTo(17.5)
        .jsonPath("$.actualBalance").isEqualTo(22.0)
    }

    @Test
    fun `should return full entitlement for user with no requests`() {
      webTestClient.get()
        .uri("/balance")
        .header("X-User-Id", bobId)
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.annualEntitlement").isEqualTo(30)
        .jsonPath("$.pendingDays").isEqualTo(0.0)
        .jsonPath("$.approvedDays").isEqualTo(0.0)
        .jsonPath("$.availableBalance").isEqualTo(30.0)
        .jsonPath("$.actualBalance").isEqualTo(30.0)
    }

    @Test
    fun `should return 400 when X-User-Id header is missing`() {
      webTestClient.get()
        .uri("/balance")
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    fun `should return 400 when X-User-Id header is not a valid UUID`() {
      webTestClient.get()
        .uri("/balance")
        .header("X-User-Id", "not-a-uuid")
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    fun `should return 404 when user does not exist`() {
      webTestClient.get()
        .uri("/balance")
        .header("X-User-Id", "00000000-0000-0000-0000-999999999999")
        .exchange()
        .expectStatus().isNotFound
        .expectBody()
        .jsonPath("$.status").isEqualTo(404)
        .jsonPath("$.userMessage").isEqualTo("User not found: 00000000-0000-0000-0000-999999999999")
    }
  }

  @Nested
  @DisplayName("GET /balance - service error")
  inner class GetBalanceServiceError {

    @MockitoBean
    private lateinit var leaveRequestService: LeaveRequestService

    @Test
    fun `should return 500 when service throws unexpected exception`() {
      whenever(leaveRequestService.getBalance(any()))
        .thenThrow(RuntimeException("Something went wrong"))

      webTestClient.get()
        .uri("/balance")
        .header("X-User-Id", aliceId)
        .exchange()
        .expectStatus().is5xxServerError
        .expectBody()
        .jsonPath("$.status").isEqualTo(500)
        .jsonPath("$.userMessage").isEqualTo("Unexpected error: Something went wrong")
    }
  }
}
