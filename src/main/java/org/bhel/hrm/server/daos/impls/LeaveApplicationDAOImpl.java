package org.bhel.hrm.server.daos.impls;

import org.bhel.hrm.common.dtos.LeaveApplicationDTO;
import org.bhel.hrm.common.exceptions.DataAccessException;
import org.bhel.hrm.server.config.DatabaseManager;
import org.bhel.hrm.server.daos.AbstractDAO;
import org.bhel.hrm.server.daos.LeaveApplicationDAO;
import org.bhel.hrm.server.domain.LeaveApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.List;
import java.util.Optional;

public class LeaveApplicationDAOImpl extends AbstractDAO<LeaveApplication> implements LeaveApplicationDAO {
    private static final Logger logger = LoggerFactory.getLogger(LeaveApplicationDAOImpl.class);

    private static final String TABLE_NAME = "leave_applications";

    private static final String SELECT_BASE = """
        SELECT
            la.id,
            la.employee_id,
            la.start_date_time,
            la.end_date_time,
            la.type_id,
            la.status_id,
            la.reason
        FROM leave_applications la
        """;

    public LeaveApplicationDAOImpl(DatabaseManager dbManager) {
        super(dbManager);
    }



    // ---------------- DAO<LeaveApplication, Integer> ----------------


    @Override
    public Integer findOwnerUserIdByLeaveId(int leaveId) {
        String sql = """
        SELECT e.user_id
        FROM leave_applications la
        JOIN employees e ON e.id = la.employee_id
        WHERE la.id = ?
    """;

        try (var conn = dbManager.getConnection();
             var ps = conn.prepareStatement(sql)) {

            ps.setInt(1, leaveId);

            try (var rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("user_id") : null;
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to find owner user_id for leaveId=" + leaveId, e);
        }
    }




    @Override
    public Optional<LeaveApplication> findById(Integer id) {
        String sql = SELECT_BASE + " WHERE la.id = ?";
        return findOne(sql,
                stmt -> stmt.setInt(1, id),
                this::mapRow);
    }

    @Override
    public List<LeaveApplication> findAll() {
        String sql = SELECT_BASE + " ORDER BY la.start_date_time DESC";
        return findMany(sql,
                stmt -> { /* no params */ },
                this::mapRow);
    }

    @Override
    public void save(LeaveApplication entity) {
        // Option A: delegate to insert/update based on id
        if (entity.getId() == 0) {
            insert(entity);
        } else {
            update(entity);
        }
    }

    @Override
    public void deleteById(Integer id) {
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE id = ?";
        executeUpdate(sql, stmt -> stmt.setInt(1, id));
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) AS total FROM " + TABLE_NAME;
        Connection conn = null;

        try {
            conn = dbManager.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {
                    return rs.getLong("total");
                }
                return 0L;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error counting leave applications", e);
        } finally {
            dbManager.releaseConnection(conn);
        }
    }

    // ---------------- LeaveApplicationDAO custom methods ----------------

    @Override
    public List<LeaveApplication> findByEmployeeId(int employeeId) {
        String sql = SELECT_BASE + " WHERE la.employee_id = ? ORDER BY la.start_date_time DESC";

        return findMany(sql,
                stmt -> stmt.setInt(1, employeeId),
                this::mapRow);
    }

    // ---------------- AbstractDAO hooks ----------------

    @Override
    protected void insert(LeaveApplication entity) {
        String sql = """
            INSERT INTO leave_applications
                (employee_id, start_date_time, end_date_time, type_id, status_id, reason)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        Connection conn = null;
        try {
            conn = dbManager.getConnection();

            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                setSaveParameters(stmt, entity);

                int affectedRows = stmt.executeUpdate();
                logger.info("Executed insert: {}", stmt);

                if (affectedRows == 0) {
                    throw new DataAccessException("Inserting leave application failed, no ID obtained.", null);
                }

                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        entity.setId(keys.getInt(1));
                    } else {
                        throw new DataAccessException("Inserting leave application failed, no ID obtained.", null);

                    }
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error inserting leave application", e);
        } finally {
            dbManager.releaseConnection(conn);
        }
    }

    @Override
    protected void update(LeaveApplication entity) {
        String sql = """
            UPDATE leave_applications
            SET employee_id = ?, start_date_time = ?, end_date_time = ?, type_id = ?, status_id = ?, reason = ?
            WHERE id = ?
            """;

        executeUpdate(sql, stmt -> {
            setSaveParameters(stmt, entity);
            stmt.setInt(7, entity.getId());
        });
    }

    @Override
    protected void setSaveParameters(PreparedStatement stmt, LeaveApplication entity) throws SQLException {
        stmt.setInt(1, entity.getEmployeeId());
        stmt.setTimestamp(2, Timestamp.valueOf(entity.getStartDateTime()));
        stmt.setTimestamp(3, Timestamp.valueOf(entity.getEndDateTime()));
        stmt.setInt(4, mapTypeToId(entity.getType()));
        stmt.setInt(5, mapStatusToId(entity.getStatus()));
        stmt.setString(6, entity.getReason());
    }

    // ---------------- Row mapping ----------------

    private LeaveApplication mapRow(ResultSet rs) throws SQLException {
        LeaveApplication leave = new LeaveApplication();
        leave.setId(rs.getInt("id"));
        leave.setEmployeeId(rs.getInt("employee_id"));

        Timestamp startTs = rs.getTimestamp("start_date_time");
        if (startTs != null) {
            leave.setStartDateTime(startTs.toLocalDateTime());
        }

        Timestamp endTs = rs.getTimestamp("end_date_time");
        if (endTs != null) {
            leave.setEndDateTime(endTs.toLocalDateTime());
        }

        int typeId = rs.getInt("type_id");
        leave.setType(mapIdToType(typeId));

        int statusId = rs.getInt("status_id");
        leave.setStatus(mapIdToStatus(statusId));

        leave.setReason(rs.getString("reason"));

        return leave;
    }

    // ---------------- Enum <-> DB id helpers ----------------

    private int mapTypeToId(LeaveApplicationDTO.LeaveType type) {
        // Based on your seed data:
        // (1, 'annual'), (2, 'sick'), (3, 'unpaid')
        return switch (type) {
            case ANNUAL -> 1;
            case SICK -> 2;
            case UNPAID -> 3;
        };
    }

    private LeaveApplicationDTO.LeaveType mapIdToType(int id) {
        return switch (id) {
            case 1 -> LeaveApplicationDTO.LeaveType.ANNUAL;
            case 2 -> LeaveApplicationDTO.LeaveType.SICK;
            case 3 -> LeaveApplicationDTO.LeaveType.UNPAID;
            default -> throw new IllegalArgumentException("Unknown leave type id: " + id);
        };
    }

    private int mapStatusToId(LeaveApplicationDTO.LeaveStatus status) {
        // Based on your seed data:
        // (1, 'pending'), (2, 'approved'), (3, 'rejected')
        return switch (status) {
            case PENDING -> 1;
            case APPROVED -> 2;
            case REJECTED -> 3;
        };
    }

    private LeaveApplicationDTO.LeaveStatus mapIdToStatus(int id) {
        return switch (id) {
            case 1 -> LeaveApplicationDTO.LeaveStatus.PENDING;
            case 2 -> LeaveApplicationDTO.LeaveStatus.APPROVED;
            case 3 -> LeaveApplicationDTO.LeaveStatus.REJECTED;
            default -> throw new IllegalArgumentException("Unknown leave status id: " + id);
        };
    }

    @Override
    public List<LeaveApplication> findPending() {
        String sql = SELECT_BASE + " WHERE status_id = ? ORDER BY created_at DESC";
        return findMany(sql, stmt -> stmt.setInt(1, 1), this::mapRow); // 1 = pending
    }

    @Override
    public void updateStatus(int leaveId, int statusId, Integer decidedByUserId, String decisionReason) {
        String sql = """
        UPDATE leave_applications
        SET status_id = ?,
            decided_by_user_id = ?,
            decision_reason = ?,
            decided_at = CURRENT_TIMESTAMP
        WHERE id = ?
    """;

        executeUpdate(sql, stmt -> {
            stmt.setInt(1, statusId);

            if (decidedByUserId != null) stmt.setInt(2, decidedByUserId);
            else stmt.setNull(2, java.sql.Types.INTEGER);

            if (decisionReason != null && !decisionReason.isBlank()) stmt.setString(3, decisionReason);
            else stmt.setNull(3, java.sql.Types.LONGVARCHAR);

            stmt.setInt(4, leaveId);
        });
    }





}
