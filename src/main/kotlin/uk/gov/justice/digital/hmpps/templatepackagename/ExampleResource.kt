package uk.gov.justice.digital.hmpps.templatepackagename

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/example", produces = ["application/json"])
class ExampleResource {

  @GetMapping("/time")
  fun getTime(): LocalDateTime = LocalDateTime.now()
}
