package org.bhel.hrm.server.services.impls;

import org.bhel.hrm.common.dtos.LeaveApplicationDTO;
import org.bhel.hrm.server.daos.EmployeeDAO;
import org.bhel.hrm.server.daos.LeaveApplicationDAO;
import org.bhel.hrm.server.domain.LeaveApplication;
import org.bhel.hrm.server.services.LeaveService;

import java.util.List;

public class LeaveServiceImpl implements LeaveService {

    private final LeaveApplicationDAO leaveDAO;
    private final EmployeeDAO employeeDAO;

    public LeaveServiceImpl(LeaveApplicationDAO leaveDAO, EmployeeDAO employeeDAO) {
        this.leaveDAO = leaveDAO;
        this.employeeDAO = employeeDAO;
    }

    @Override
    public void applyForLeave(LeaveApplicationDTO dto) {
        LeaveApplication leave = new LeaveApplication();
        leave.setEmployeeId(dto.employeeId());
        leave.setStartDateTime(dto.startDateTime());
        leave.setEndDateTime(dto.endDateTime());
        leave.setType(dto.type());
        leave.setStatus(LeaveApplicationDTO.LeaveStatus.PENDING);
        leave.setReason(dto.reason());

        // correct DAO name
        leaveDAO.save(leave);
    }

    @Override
    public List<LeaveApplicationDTO> getLeaveHistory(int employeeId) {
        return leaveDAO.findByEmployeeId(employeeId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public List<LeaveApplicationDTO> getPendingLeaves() {
        return leaveDAO.findPending()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public void decideLeave(int leaveId, boolean approve, int hrUserId, String decisionReason) {

        if (leaveId <= 0) {
            throw new IllegalArgumentException("Invalid leave ID");
        }
        if (hrUserId <= 0) {
            throw new IllegalArgumentException("Invalid HR user ID");
        }

        Integer ownerUserId = leaveDAO.findOwnerUserIdByLeaveId(leaveId);
        if (ownerUserId == null) {
            throw new IllegalArgumentException("Leave not found: " + leaveId);
        }

        // Block HR approving/rejecting their own leave
        if (ownerUserId == hrUserId) {
            throw new IllegalArgumentException("You cannot approve/reject your own leave request.");
        }

        LeaveApplicationDTO.LeaveStatus newStatus =
                approve ? LeaveApplicationDTO.LeaveStatus.APPROVED : LeaveApplicationDTO.LeaveStatus.REJECTED;

        int affected = leaveDAO.decideIfPending(leaveId, newStatus, hrUserId, decisionReason);

        if (affected == 0) {
            // Either: leave already decided (not pending) OR was deleted between checks
            throw new IllegalStateException("Leave decision failed: request is no longer pending (or does not exist).");
        }
    }


    private LeaveApplicationDTO toDTO(LeaveApplication leave) {

        return new LeaveApplicationDTO(
                leave.getId(),
                leave.getEmployeeId(),
                leave.getStartDateTime(),
                leave.getEndDateTime(),
                leave.getType(),
                leave.getStatus(),
                leave.getReason()
        );
    }
}
