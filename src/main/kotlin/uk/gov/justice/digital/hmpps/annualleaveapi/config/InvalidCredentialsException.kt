package uk.gov.justice.digital.hmpps.annualleaveapi.config

class InvalidCredentialsException : RuntimeException("Invalid password")

class UserNotRegisteredException : RuntimeException("You are not registered for this service")
