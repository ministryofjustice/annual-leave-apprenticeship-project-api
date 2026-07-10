package uk.gov.justice.digital.hmpps.annualleaveapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class AnnualLeaveApi

fun main(args: Array<String>) {
  runApplication<AnnualLeaveApi>(*args)
}
