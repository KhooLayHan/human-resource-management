package org.bhel.hrm.server.services.impls;

import org.bhel.hrm.common.dtos.LeaveApplicationDTO;
import org.bhel.hrm.server.daos.EmployeeDAO;
import org.bhel.hrm.server.daos.LeaveApplicationDAO;
import org.bhel.hrm.server.domain.LeaveApplication;
import org.bhel.hrm.server.services.LeaveService;

import java.time.LocalDateTime;
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
        if (dto == null) {
            throw new IllegalArgumentException("LeaveApplicationDTO must not be null");
        }

        if (dto.employeeId() <= 0) {
            throw new IllegalArgumentException("Invalid employeeId: " + dto.employeeId());
        }

        employeeDAO.findById(dto.employeeId())
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + dto.employeeId()));

        LocalDateTime start = dto.startDateTime();
        LocalDateTime end = dto.endDateTime();

        if (start == null || end == null) {
            throw new IllegalArgumentException("Start and End date/time must not be null");
        }
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("End date/time cannot be before start date/time");
        }

        LeaveApplication leave = new LeaveApplication();
        leave.setEmployeeId(dto.employeeId());
        leave.setStartDateTime(start);
        leave.setEndDateTime(end);
        leave.setType(dto.type());
        leave.setStatus(LeaveApplicationDTO.LeaveStatus.PENDING);
        leave.setReason(dto.reason());

        leaveDAO.save(leave);
    }

    @Override
    public List<LeaveApplicationDTO> getLeaveHistory(int employeeId) {
        if (employeeId <= 0) {
            throw new IllegalArgumentException("Invalid employeeId: " + employeeId);
        }

        return leaveDAO.findByEmployeeId(employeeId).stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public List<LeaveApplicationDTO> getPendingLeaves() {
        return leaveDAO.findPending().stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public void decideLeave(int leaveId, boolean approve, int hrUserId, String decisionReason) {
        if (leaveId <= 0) {
            throw new IllegalArgumentException("Invalid leaveId: " + leaveId);
        }
        if (hrUserId <= 0) {
            throw new IllegalArgumentException("Invalid HR user ID: " + hrUserId);
        }

        Integer ownerUserId = leaveDAO.findOwnerUserIdByLeaveId(leaveId);
        if (ownerUserId == null) {
            throw new IllegalArgumentException("Leave not found: " + leaveId);
        }
        if (ownerUserId == hrUserId) {
            throw new IllegalArgumentException("You cannot approve/reject your own leave request.");
        }

        int newStatusId = approve ? 2 : 3; // approved=2, rejected=3 (your seed)
        leaveDAO.updateStatus(leaveId, newStatusId, hrUserId, decisionReason);
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
