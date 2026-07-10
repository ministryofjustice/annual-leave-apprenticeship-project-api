package uk.gov.justice.digital.hmpps.annualleaveapi.controller.response

import uk.gov.justice.digital.hmpps.annualleaveapi.model.LeaveRequest

data class LeaveRequestResponse(
  val userRequests: List<LeaveRequest>,
)
