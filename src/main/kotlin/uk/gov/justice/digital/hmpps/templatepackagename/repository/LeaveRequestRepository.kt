package uk.gov.justice.digital.hmpps.templatepackagename.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.templatepackagename.model.LeaveRequest
import uk.gov.justice.digital.hmpps.templatepackagename.model.Status
import java.util.UUID

@Repository
interface LeaveRequestRepository : JpaRepository<LeaveRequest, UUID> {

  fun findAllByCreatorId(creatorId: UUID): List<LeaveRequest>

  fun findAllByCreatorIdAndStatusIn(creatorId: UUID, statuses: Collection<Status>): List<LeaveRequest>

  fun findAllByApproverId(approverId: UUID): List<LeaveRequest>
}
