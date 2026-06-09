package uk.gov.justice.digital.hmpps.templatepackagename.integration

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.test.context.bean.override.mockito.MockitoBean
import uk.gov.justice.digital.hmpps.templatepackagename.data.SeedData
import uk.gov.justice.digital.hmpps.templatepackagename.service.LeaveRequestService
import java.util.UUID

class RequestsControllerTest : IntegrationTestBase() {

  private val aliceId = SeedData.userAlice.id.toString()
  private val bobId = SeedData.userBob.id.toString()
  private val aliceRequestCount = SeedData.leaveRequests.count { it.creatorId == SeedData.userAlice.id }

  @Nested
  @DisplayName("GET /requests")
  inner class GetMyRequests {

    @Test
    fun `should return leave requests for a valid user`() {
      webTestClient.get()
        .uri("/requests")
        .header("X-User-Id", aliceId)
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.userRequests.length()").isEqualTo(aliceRequestCount)
        .jsonPath("$.userRequests[0].creatorId").isEqualTo(aliceId)
    }

    @Test
    fun `should return empty list when user has no requests`() {
      webTestClient.get()
        .uri("/requests")
        .header("X-User-Id", bobId)
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.userRequests.length()").isEqualTo(0)
    }

    @Test
    fun `should return 400 when X-User-Id header is missing`() {
      webTestClient.get()
        .uri("/requests")
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    fun `should return 400 when X-User-Id header is not a valid UUID`() {
      webTestClient.get()
        .uri("/requests")
        .header("X-User-Id", "not-a-uuid")
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    fun `should return 404 when user does not exist`() {
      webTestClient.get()
        .uri("/requests")
        .header("X-User-Id", "00000000-0000-0000-0000-999999999999")
        .exchange()
        .expectStatus().isNotFound
        .expectBody()
        .jsonPath("$.status").isEqualTo(404)
        .jsonPath("$.userMessage").isEqualTo("User not found: 00000000-0000-0000-0000-999999999999")
    }
  }

  @Nested
  @DisplayName("GET /requests - service error")
  inner class GetMyRequestsServiceError {

    // note: this bean on a nested class creates a separate application context for that nested group, isolating the mock
    // from the rest of the tests
    @MockitoBean
    private lateinit var leaveRequestService: LeaveRequestService

    @Test
    fun `should return 500 when service throws unexpected exception`() {
      whenever(leaveRequestService.getRequestsByUser(any()))
        .thenThrow(RuntimeException("Something went wrong"))

      webTestClient.get()
        .uri("/requests")
        .header("X-User-Id", UUID.randomUUID().toString())
        .exchange()
        .expectStatus().is5xxServerError
        .expectBody()
        .jsonPath("$.status").isEqualTo(500)
        .jsonPath("$.userMessage").isEqualTo("Unexpected error: Something went wrong")
    }
  }

  @Nested
  @DisplayName("POST /requests")
  inner class CreateRequest {

    @Test
    fun `should create a leave request and return 201`() {
      webTestClient.post()
        .uri("/requests")
        .header("X-User-Id", aliceId)
        .header("Content-Type", "application/json")
        .bodyValue(
          """
          {
            "startDate": "2026-06-01",
            "endDate": "2026-06-05",

            "isFirstDayHalfDay": false,
            "isLastDayHalfDay": true,
            "creatorNote": "Holiday"
          }
          """.trimIndent(),
        )
        .exchange()
        .expectStatus().isCreated
        .expectBody()
        .jsonPath("$.creatorId").isEqualTo(aliceId)
        .jsonPath("$.approverId").isEqualTo(bobId)
        .jsonPath("$.startDate").isEqualTo("2026-06-01")
        .jsonPath("$.endDate").isEqualTo("2026-06-05")
        .jsonPath("$.duration").isEqualTo(4.5)
        .jsonPath("$.isFirstDayHalfDay").isEqualTo(false)
        .jsonPath("$.isLastDayHalfDay").isEqualTo(true)
        .jsonPath("$.status").isEqualTo("PENDING")
        .jsonPath("$.creatorNote").isEqualTo("Holiday")
        .jsonPath("$.approverNote").doesNotExist()
        .jsonPath("$.id").isNotEmpty
        .jsonPath("$.createdAt").isNotEmpty
    }

    @Test
    fun `should return 400 when X-User-Id header is missing`() {
      webTestClient.post()
        .uri("/requests")
        .header("Content-Type", "application/json")
        .bodyValue(
          """
          {
            "startDate": "2026-06-01",
            "endDate": "2026-06-05",

            "isFirstDayHalfDay": false,
            "isLastDayHalfDay": true
          }
          """.trimIndent(),
        )
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    fun `should return 404 when user does not exist`() {
      webTestClient.post()
        .uri("/requests")
        .header("X-User-Id", "00000000-0000-0000-0000-999999999999")
        .header("Content-Type", "application/json")
        .bodyValue(
          """
          {
            "startDate": "2026-06-01",
            "endDate": "2026-06-05",

            "isFirstDayHalfDay": false,
            "isLastDayHalfDay": true
          }
          """.trimIndent(),
        )
        .exchange()
        .expectStatus().isNotFound
        .expectBody()
        .jsonPath("$.status").isEqualTo(404)
    }
  }
}
