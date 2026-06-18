package uk.gov.justice.digital.hmpps.annualleaveapi.config

import java.util.UUID

class UserNotFoundException(userId: UUID) : RuntimeException("User not found: $userId")
