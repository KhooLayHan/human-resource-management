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

    // ---------------- SQL fragments ----------------

    private static final String SELECT_ALL = """
        SELECT
            la.id,
            la.employee_id,
            la.start_date_time,
            la.end_date_time,
            la.type_id,
            la.status_id,
            la.reason
        FROM %s la
        """.formatted(TABLE_NAME);

    private static final String ORDER_BY_START_DESC = " ORDER BY la.start_date_time DESC";

    private static final String WHERE_ID = " WHERE la.id = ?";
    private static final String WHERE_EMPLOYEE_ID = " WHERE la.employee_id = ?";
    private static final String WHERE_STATUS_ID = " WHERE la.status_id = ?";

    private static final String DELETE_BY_ID_SQL =
            "DELETE FROM " + TABLE_NAME + " WHERE id = ?";

    private static final String COUNT_SQL =
            "SELECT COUNT(*) AS total FROM " + TABLE_NAME;

    private static final String INSERT_SQL = """
        INSERT INTO %s
            (employee_id, start_date_time, end_date_time, type_id, status_id, reason)
        VALUES (?, ?, ?, ?, ?, ?)
        """.formatted(TABLE_NAME);

    private static final String UPDATE_SQL = """
        UPDATE %s
        SET employee_id = ?, start_date_time = ?, end_date_time = ?, type_id = ?, status_id = ?, reason = ?
        WHERE id = ?
        """.formatted(TABLE_NAME);

    private static final String OWNER_USER_ID_BY_LEAVE_ID_SQL = """
        SELECT e.user_id
        FROM leave_applications la
        JOIN employees e ON e.id = la.employee_id
        WHERE la.id = ?
        """;

    private static final String UPDATE_STATUS_SQL = """
        UPDATE leave_applications
        SET status_id = ?,
            decided_by_user_id = ?,
            decision_reason = ?,
            decided_at = CURRENT_TIMESTAMP
        WHERE id = ?
        """;

    public LeaveApplicationDAOImpl(DatabaseManager dbManager) {
        super(dbManager);
    }

    // ---------------- DAO<LeaveApplication, Integer> ----------------

    @Override
    public Optional<LeaveApplication> findById(Integer id) {
        return findOne(SELECT_ALL + WHERE_ID,
                stmt -> stmt.setInt(1, id),
                this::mapRow);
    }

    @Override
    public List<LeaveApplication> findAll() {
        return findMany(SELECT_ALL + ORDER_BY_START_DESC,
                stmt -> { /* no params */ },
                this::mapRow);
    }

    @Override
    public void save(LeaveApplication entity) {
        if (entity.getId() == 0) {
            insert(entity);
        } else {
            update(entity);
        }
    }

    @Override
    public void deleteById(Integer id) {
        executeUpdate(DELETE_BY_ID_SQL, stmt -> stmt.setInt(1, id));
    }

    @Override
    public long count() {
        return countWithSql(COUNT_SQL, "Error counting leave applications");
    }

    // ---------------- LeaveApplicationDAO custom methods ----------------

    @Override
    public Integer findOwnerUserIdByLeaveId(int leaveId) {
        try (var conn = dbManager.getConnection();
             var ps = conn.prepareStatement(OWNER_USER_ID_BY_LEAVE_ID_SQL)) {

            ps.setInt(1, leaveId);

            try (var rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("user_id") : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find owner user_id for leaveId=" + leaveId, e);
        }
    }

    @Override
    public List<LeaveApplication> findByEmployeeId(int employeeId) {
        return findMany(SELECT_ALL + WHERE_EMPLOYEE_ID + ORDER_BY_START_DESC,
                stmt -> stmt.setInt(1, employeeId),
                this::mapRow);
    }

    @Override
    public List<LeaveApplication> findPending() {
        // pending = 1 (per your seed)
        // Note: previously you had ORDER BY created_at DESC (may not exist). Using start_date_time instead.
        return findMany(SELECT_ALL + WHERE_STATUS_ID + ORDER_BY_START_DESC,
                stmt -> stmt.setInt(1, 1),
                this::mapRow);
    }

    @Override
    public void updateStatus(int leaveId, int statusId, Integer decidedByUserId, String decisionReason) {
        executeUpdate(UPDATE_STATUS_SQL, stmt -> {
            stmt.setInt(1, statusId);

            if (decidedByUserId != null) {
                stmt.setInt(2, decidedByUserId);
            } else {
                stmt.setNull(2, Types.INTEGER);
            }

            if (decisionReason != null && !decisionReason.isBlank()) {
                stmt.setString(3, decisionReason);
            } else {
                stmt.setNull(3, Types.LONGVARCHAR);
            }

            stmt.setInt(4, leaveId);
        });
    }

    // ---------------- AbstractDAO hooks ----------------

    @Override
    protected void insert(LeaveApplication entity) {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();

            try (PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
                setSaveParameters(stmt, entity);

                int affectedRows = stmt.executeUpdate();
                logger.info("Executed insert: {}", stmt);

                if (affectedRows == 0) {
                    throw new DataAccessException("Inserting leave application failed, no rows affected.", null);
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
        executeUpdate(UPDATE_SQL, stmt -> {
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

    // ---------------- Helpers (duplication reducers) ----------------

    private long countWithSql(String sql, String errorMessage) {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                return rs.next() ? rs.getLong("total") : 0L;
            }
        } catch (SQLException e) {
            throw new DataAccessException(errorMessage, e);
        } finally {
            dbManager.releaseConnection(conn);
        }
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
}
