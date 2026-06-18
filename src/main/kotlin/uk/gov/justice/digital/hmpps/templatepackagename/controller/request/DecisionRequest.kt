package uk.gov.justice.digital.hmpps.templatepackagename.controller.request

import uk.gov.justice.digital.hmpps.templatepackagename.model.Status

data class DecisionRequest(
  val status: Status,
  val approverNote: String? = null,
)
