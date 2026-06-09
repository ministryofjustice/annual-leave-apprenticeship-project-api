package uk.gov.justice.digital.hmpps.templatepackagename.controller.response

import uk.gov.justice.digital.hmpps.templatepackagename.model.LeaveRequest

data class LeaveRequestResponse(
  val userRequests: List<LeaveRequest>,
)
