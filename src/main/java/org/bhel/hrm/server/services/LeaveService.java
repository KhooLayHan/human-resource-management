package org.bhel.hrm.server.services;

import org.bhel.hrm.common.dtos.LeaveApplicationDTO;

import java.util.List;

public interface LeaveService {
    void applyForLeave(LeaveApplicationDTO dto);
    List<LeaveApplicationDTO> getLeaveHistory(int employeeId);
    List<LeaveApplicationDTO> getPendingLeaves();
    void decideLeave(int leaveId, boolean approve, int hrUserId, String decisionReason);

}
