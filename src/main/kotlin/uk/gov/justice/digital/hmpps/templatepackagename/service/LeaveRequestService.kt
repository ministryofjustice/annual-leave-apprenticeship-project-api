package uk.gov.justice.digital.hmpps.templatepackagename.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.templatepackagename.config.UserNotFoundException
import uk.gov.justice.digital.hmpps.templatepackagename.data.SeedData
import uk.gov.justice.digital.hmpps.templatepackagename.model.LeaveRequest
import java.util.UUID

@Service
class LeaveRequestService {

  // TODO: temp in-memory storage, will be replaced with a repository later
  private val requests = SeedData.leaveRequests
  private val users = SeedData.users

  fun getRequestsByUser(userId: UUID): List<LeaveRequest> {
    if (users.none { it.id == userId }) {
      throw UserNotFoundException(userId)
    }

    return requests.filter { it.creatorId == userId }
  }
}
