package uk.gov.justice.digital.hmpps.templatepackagename.controller.response

data class BalanceResponse(
  val annualEntitlement: Int,
  val availableBalance: Double,
  val actualBalance: Double,
  val pendingDays: Double,
  val approvedDays: Double,
)
