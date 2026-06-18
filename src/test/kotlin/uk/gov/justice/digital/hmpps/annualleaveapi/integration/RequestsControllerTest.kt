package uk.gov.justice.digital.hmpps.annualleaveapi.integration

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean
import uk.gov.justice.digital.hmpps.annualleaveapi.repository.LeaveRequestRepository
import uk.gov.justice.digital.hmpps.annualleaveapi.service.LeaveRequestService
import java.util.UUID

class RequestsControllerTest : IntegrationTestBase() {

  @Autowired
  private lateinit var leaveRequestRepository: LeaveRequestRepository

  private val aliceId = "00000000-0000-0000-0000-000000000001"
  private val bobId = "00000000-0000-0000-0000-000000000002"
  private val seedRequestIds = setOf(
    UUID.fromString("00000000-0000-0000-0000-000000000101"),
    UUID.fromString("00000000-0000-0000-0000-000000000102"),
  )

  @AfterEach
  fun cleanUp() {
    leaveRequestRepository.findAll()
      .filter { it.id !in seedRequestIds }
      .forEach { leaveRequestRepository.deleteById(it.id) }
  }

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
        .jsonPath("$.userRequests.length()").isEqualTo(2)
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
            "startDate": "2026-08-03",
            "endDate": "2026-08-07",
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
        .jsonPath("$.startDate").isEqualTo("2026-08-03")
        .jsonPath("$.endDate").isEqualTo("2026-08-07")
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
            "startDate": "2026-08-03",
            "endDate": "2026-08-07",
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
            "startDate": "2026-08-03",
            "endDate": "2026-08-07",
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

  @Nested
  @DisplayName("DELETE /requests/{id}")
  inner class DeleteRequest {

    private val approvedRequestId = "00000000-0000-0000-0000-000000000102"

    private fun createPendingRequest(): String {
      val result = webTestClient.post()
        .uri("/requests")
        .header("X-User-Id", aliceId)
        .header("Content-Type", "application/json")
        .bodyValue(
          """
          {
            "startDate": "2026-11-02",
            "endDate": "2026-11-06",
            "isFirstDayHalfDay": false,
            "isLastDayHalfDay": false
          }
          """.trimIndent(),
        )
        .exchange()
        .expectStatus().isCreated
        .expectBody()
        .returnResult()

      val body = String(result.responseBody!!)
      return body.substringAfter("\"id\":\"").substringBefore("\"")
    }

    @Test
    fun `should delete a pending request and return 204`() {
      val newRequestId = createPendingRequest()

      webTestClient.delete()
        .uri("/requests/$newRequestId")
        .header("X-User-Id", aliceId)
        .exchange()
        .expectStatus().isNoContent
    }

    @Test
    fun `should return 400 when request is not pending`() {
      webTestClient.delete()
        .uri("/requests/$approvedRequestId")
        .header("X-User-Id", aliceId)
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    fun `should return 403 when user is not the creator`() {
      val newRequestId = createPendingRequest()

      webTestClient.delete()
        .uri("/requests/$newRequestId")
        .header("X-User-Id", bobId)
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return 404 when request does not exist`() {
      webTestClient.delete()
        .uri("/requests/00000000-0000-0000-0000-999999999999")
        .header("X-User-Id", aliceId)
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `should return 400 when X-User-Id header is missing`() {
      val newRequestId = createPendingRequest()

      webTestClient.delete()
        .uri("/requests/$newRequestId")
        .exchange()
        .expectStatus().isBadRequest
    }
  }

  @Nested
  @DisplayName("GET /requests/assigned")
  inner class GetAssignedRequests {

    @Test
    fun `should return assigned requests for a manager`() {
      webTestClient.get()
        .uri("/requests/assigned")
        .header("X-User-Id", bobId)
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.userRequests.length()").isEqualTo(2)
        .jsonPath("$.userRequests[0].approverId").isEqualTo(bobId)
    }

    @Test
    fun `should return empty list when user has no assigned requests`() {
      webTestClient.get()
        .uri("/requests/assigned")
        .header("X-User-Id", aliceId)
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.userRequests.length()").isEqualTo(0)
    }

    @Test
    fun `should return 404 when user does not exist`() {
      webTestClient.get()
        .uri("/requests/assigned")
        .header("X-User-Id", "00000000-0000-0000-0000-999999999999")
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `should return 400 when X-User-Id header is missing`() {
      webTestClient.get()
        .uri("/requests/assigned")
        .exchange()
        .expectStatus().isBadRequest
    }
  }

  @Nested
  @DisplayName("PATCH /requests/assigned/{id}")
  inner class DecideRequest {

    private val approvedRequestId = "00000000-0000-0000-0000-000000000102"

    private fun createPendingRequest(): String {
      val result = webTestClient.post()
        .uri("/requests")
        .header("X-User-Id", aliceId)
        .header("Content-Type", "application/json")
        .bodyValue(
          """
          {
            "startDate": "2026-12-01",
            "endDate": "2026-12-05",
            "isFirstDayHalfDay": false,
            "isLastDayHalfDay": false
          }
          """.trimIndent(),
        )
        .exchange()
        .expectStatus().isCreated
        .expectBody()
        .returnResult()

      val body = String(result.responseBody!!)
      return body.substringAfter("\"id\":\"").substringBefore("\"")
    }

    @Test
    fun `should approve a pending request and return 200`() {
      val newRequestId = createPendingRequest()

      webTestClient.patch()
        .uri("/requests/assigned/$newRequestId")
        .header("X-User-Id", bobId)
        .header("Content-Type", "application/json")
        .bodyValue("""{"status": "APPROVED", "approverNote": "Enjoy!"}""")
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.status").isEqualTo("APPROVED")
        .jsonPath("$.approverNote").isEqualTo("Enjoy!")
        .jsonPath("$.decisionAt").isNotEmpty
    }

    @Test
    fun `should reject a pending request and return 200`() {
      val newRequestId = createPendingRequest()

      webTestClient.patch()
        .uri("/requests/assigned/$newRequestId")
        .header("X-User-Id", bobId)
        .header("Content-Type", "application/json")
        .bodyValue("""{"status": "REJECTED", "approverNote": "Team is busy"}""")
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.status").isEqualTo("REJECTED")
        .jsonPath("$.approverNote").isEqualTo("Team is busy")
        .jsonPath("$.decisionAt").isNotEmpty
    }

    @Test
    fun `should return 400 when decision status is PENDING`() {
      val newRequestId = createPendingRequest()

      webTestClient.patch()
        .uri("/requests/assigned/$newRequestId")
        .header("X-User-Id", bobId)
        .header("Content-Type", "application/json")
        .bodyValue("""{"status": "PENDING"}""")
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    fun `should return 400 when request is already decided`() {
      webTestClient.patch()
        .uri("/requests/assigned/$approvedRequestId")
        .header("X-User-Id", bobId)
        .header("Content-Type", "application/json")
        .bodyValue("""{"status": "REJECTED"}""")
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    fun `should return 403 when user is not the assigned approver`() {
      val newRequestId = createPendingRequest()

      webTestClient.patch()
        .uri("/requests/assigned/$newRequestId")
        .header("X-User-Id", aliceId)
        .header("Content-Type", "application/json")
        .bodyValue("""{"status": "APPROVED"}""")
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return 404 when request does not exist`() {
      webTestClient.patch()
        .uri("/requests/assigned/00000000-0000-0000-0000-999999999999")
        .header("X-User-Id", bobId)
        .header("Content-Type", "application/json")
        .bodyValue("""{"status": "APPROVED"}""")
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `should return 400 when X-User-Id header is missing`() {
      val newRequestId = createPendingRequest()

      webTestClient.patch()
        .uri("/requests/assigned/$newRequestId")
        .header("Content-Type", "application/json")
        .bodyValue("""{"status": "APPROVED"}""")
        .exchange()
        .expectStatus().isBadRequest
    }
  }
}
