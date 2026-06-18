package uk.gov.justice.digital.hmpps.annualleaveapi.config

import java.util.UUID

class LeaveRequestNotFoundException(requestId: UUID) : RuntimeException("Leave request not found: $requestId")
