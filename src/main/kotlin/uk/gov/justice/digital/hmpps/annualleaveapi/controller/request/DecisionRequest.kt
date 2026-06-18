package uk.gov.justice.digital.hmpps.annualleaveapi.controller.request

import uk.gov.justice.digital.hmpps.annualleaveapi.model.Status

data class DecisionRequest(
  val status: Status,
  val approverNote: String? = null,
)
