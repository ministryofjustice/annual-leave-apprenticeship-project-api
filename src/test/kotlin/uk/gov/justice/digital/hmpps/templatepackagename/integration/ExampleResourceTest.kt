package uk.gov.justice.digital.hmpps.templatepackagename.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

// TODO This test exists to support the HMPPS Typescript template and should be removed by the bootstrap process
class ExampleResourceTest : IntegrationTestBase() {

  @Nested
  @DisplayName("GET /example/time")
  inner class TimeEndpoint {

    @Test
    fun `should return OK`() {
      webTestClient.get()
        .uri("/example/time")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$").value<String> {
          assertThat(it).startsWith("${LocalDate.now()}")
        }
    }
  }
}
