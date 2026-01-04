package org.bhel.hrm.server.services;

import org.bhel.hrm.common.dtos.LeaveApplicationDTO;
import org.bhel.hrm.common.error.ErrorCode;
import org.bhel.hrm.common.exceptions.HRMException;
import org.bhel.hrm.common.exceptions.InvalidInputException;
import org.bhel.hrm.common.exceptions.ResourceNotFoundException;
import org.bhel.hrm.server.config.DatabaseManager;
import org.bhel.hrm.server.daos.EmployeeDAO;
import org.bhel.hrm.server.daos.LeaveApplicationDAO;
import org.bhel.hrm.server.domain.LeaveApplication;
import org.bhel.hrm.server.mapper.LeaveApplicationMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

public class LeaveService {
    private static final Logger logger = LoggerFactory.getLogger(LeaveService.class);

    private final DatabaseManager dbManager;
    private final LeaveApplicationDAO leaveDAO;
    private final EmployeeDAO employeeDAO;

    public LeaveService(DatabaseManager dbManager, LeaveApplicationDAO leaveDAO, EmployeeDAO employeeDAO) {
        this.dbManager = dbManager;
        this.leaveDAO = leaveDAO;
        this.employeeDAO = employeeDAO;
    }

    public List<LeaveApplicationDTO> getLeaveHistory(int employeeId) {
        List<LeaveApplication> leaves = leaveDAO.findByEmployeeId(employeeId);
        return leaves.stream().map(LeaveApplicationMapper::mapToDto).toList();
    }

    public void applyForLeave(LeaveApplicationDTO leaveDTO) throws SQLException, HRMException {
        validateLeaveApplication(leaveDTO);

        dbManager.executeInTransaction(() -> {
            // Verify employee exists
            if (employeeDAO.findById(leaveDTO.employeeId()).isEmpty()) {
                throw new ResourceNotFoundException(ErrorCode.EMPLOYEE_NOT_FOUND, "Employee ID");
            }

            LeaveApplication leave = LeaveApplicationMapper.mapToDomain(leaveDTO);
            leave.setId(0); // Ensure new insert
            leave.setStatus(LeaveApplicationDTO.LeaveStatus.PENDING); // Default status

            leaveDAO.save(leave);
        });
        logger.info("Leave application submitted for Employee ID: {}", leaveDTO.employeeId());
    }

    private void validateLeaveApplication(LeaveApplicationDTO dto) throws InvalidInputException {
        if (dto.startDateTime() == null || dto.endDateTime() == null) {
            throw new InvalidInputException("Start and End dates are required");
        }
        if (dto.endDateTime().isBefore(dto.startDateTime())) {
            throw new InvalidInputException("End date cannot be before start date");
        }
        if (dto.type() == null) {
            throw new InvalidInputException("Leave Type is required");
        }
    }
}