package uk.gov.justice.digital.hmpps.templatepackagename.config

import java.util.UUID

class UserNotFoundException(userId: UUID) : RuntimeException("User not found: $userId")
