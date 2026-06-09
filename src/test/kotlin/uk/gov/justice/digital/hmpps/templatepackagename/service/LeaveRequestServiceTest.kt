package uk.gov.justice.digital.hmpps.templatepackagename.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.templatepackagename.config.UserNotFoundException
import uk.gov.justice.digital.hmpps.templatepackagename.data.SeedData
import java.util.UUID

class LeaveRequestServiceTest {

  private val service = LeaveRequestService()

  private val aliceRequestCount = SeedData.leaveRequests.count { it.creatorId == SeedData.userAlice.id }

  @Nested
  @DisplayName("getRequestsByUser()")
  inner class GetRequestsByUser {

    @Test
    fun `should return requests for a user who has them`() {
      val results = service.getRequestsByUser(SeedData.userAlice.id)

      assertThat(results).hasSize(aliceRequestCount)
      assertThat(results).allMatch { it.creatorId == SeedData.userAlice.id }
    }

    @Test
    fun `should return empty list when user has no requests`() {
      val results = service.getRequestsByUser(SeedData.userBob.id)

      assertThat(results).isEmpty()
    }

    @Test
    fun `should throw UserNotFoundException when user does not exist`() {
      val unknownId = UUID.randomUUID()

      assertThatThrownBy { service.getRequestsByUser(unknownId) }
        .isInstanceOf(UserNotFoundException::class.java)
        .hasMessageContaining(unknownId.toString())
    }
  }
}
