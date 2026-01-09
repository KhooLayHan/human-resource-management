package org.bhel.hrm.server.daos;

import org.bhel.hrm.common.dtos.LeaveApplicationDTO;
import org.bhel.hrm.server.domain.LeaveApplication;

import java.util.List;

/**
 * Data Access Object interface for LeaveApplication entities.
 */
public interface LeaveApplicationDAO extends DAO<LeaveApplication, Integer> {

    /**
     * Finds all leave applications submitted by a specific employee.
     *
     * @param employeeId The ID of the employee.
     * @return a list of leave applications for that employee (may be empty).
     */
    List<LeaveApplication> findByEmployeeId(int employeeId);

    List<LeaveApplication> findPending();
    void updateStatus(int leaveId, int statusId, Integer decidedByUserId, String decisionReason);
    Integer findOwnerUserIdByLeaveId(int leaveId);

    int decideIfPending(int leaveId,
                        LeaveApplicationDTO.LeaveStatus newStatus,
                        int decidedByUserId,
                        String decisionReason);

    private int statusId(LeaveApplicationDTO.LeaveStatus s) {
        return switch (s) {
            case PENDING -> 1;
            case APPROVED -> 2;
            case REJECTED -> 3;
        };
    }


}
