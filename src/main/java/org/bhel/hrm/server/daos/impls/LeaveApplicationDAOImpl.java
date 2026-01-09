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

public class LeaveApplicationDAOImpl
        extends AbstractDAO<LeaveApplication>
        implements LeaveApplicationDAO {

    private static final Logger logger =
            LoggerFactory.getLogger(LeaveApplicationDAOImpl.class);

    private static final String TABLE_NAME = "leave_applications";

    // ---------------- SQL ----------------

    private static final String SELECT_ALL = """
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

    private static final String ORDER_BY_START_DESC =
            " ORDER BY la.start_date_time DESC";

    private static final String WHERE_ID =
            " WHERE la.id = ?";

    private static final String WHERE_EMPLOYEE_ID =
            " WHERE la.employee_id = ?";

    private static final String WHERE_STATUS_ID =
            " WHERE la.status_id = ?";

    private static final String INSERT_SQL = """
        INSERT INTO leave_applications
            (employee_id, start_date_time, end_date_time, type_id, status_id, reason)
        VALUES (?, ?, ?, ?, ?, ?)
        """;

    private static final String UPDATE_SQL = """
        UPDATE leave_applications
        SET employee_id = ?,
            start_date_time = ?,
            end_date_time = ?,
            type_id = ?,
            status_id = ?,
            reason = ?
        WHERE id = ?
        """;

    private static final String DELETE_BY_ID_SQL =
            "DELETE FROM leave_applications WHERE id = ?";

    private static final String COUNT_SQL =
            "SELECT COUNT(*) AS total FROM leave_applications";

    private static final String OWNER_USER_ID_BY_LEAVE_ID_SQL = """
        SELECT e.user_id
        FROM leave_applications la
        JOIN employees e ON e.id = la.employee_id
        WHERE la.id = ?
        """;

    private static final String DECIDE_IF_PENDING_SQL = """
        UPDATE leave_applications
        SET status_id = ?,
            decided_by_user_id = ?,
            decision_reason = ?,
            decided_at = CURRENT_TIMESTAMP
        WHERE id = ?
          AND status_id = ?
        """;

    // ---------------- ctor ----------------

    public LeaveApplicationDAOImpl(DatabaseManager dbManager) {
        super(dbManager);
    }

    // ---------------- DAO basics ----------------

    @Override
    public Optional<LeaveApplication> findById(Integer id) {
        return findOne(
                SELECT_ALL + WHERE_ID,
                stmt -> stmt.setInt(1, id),
                this::mapRow
        );
    }

    @Override
    public List<LeaveApplication> findAll() {
        return findMany(
                SELECT_ALL + ORDER_BY_START_DESC,
                stmt -> {},
                this::mapRow
        );
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

    // ---------------- Custom queries ----------------

    @Override
    public Integer findOwnerUserIdByLeaveId(int leaveId) {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            try (PreparedStatement stmt =
                         conn.prepareStatement(OWNER_USER_ID_BY_LEAVE_ID_SQL)) {

                stmt.setInt(1, leaveId);
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next() ? rs.getInt("user_id") : null;
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Failed to find owner for leaveId=" + leaveId, e);
        } finally {
            dbManager.releaseConnection(conn);
        }
    }

    @Override
    public List<LeaveApplication> findByEmployeeId(int employeeId) {
        return findMany(
                SELECT_ALL + WHERE_EMPLOYEE_ID + ORDER_BY_START_DESC,
                stmt -> stmt.setInt(1, employeeId),
                this::mapRow
        );
    }

    @Override
    public List<LeaveApplication> findPending() {
        return findMany(
                SELECT_ALL + WHERE_STATUS_ID + ORDER_BY_START_DESC,
                stmt -> stmt.setInt(
                        1,
                        mapStatusToId(LeaveApplicationDTO.LeaveStatus.PENDING)
                ),
                this::mapRow
        );
    }

    // ---------------- Concurrency-safe decision ----------------

    @Override
    public int decideIfPending(int leaveId,
                               LeaveApplicationDTO.LeaveStatus newStatus,
                               int decidedByUserId,
                               String decisionReason) {

        int newStatusId = mapStatusToId(newStatus);
        int pendingStatusId =
                mapStatusToId(LeaveApplicationDTO.LeaveStatus.PENDING);

        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            try (PreparedStatement stmt =
                         conn.prepareStatement(DECIDE_IF_PENDING_SQL)) {

                stmt.setInt(1, newStatusId);
                stmt.setInt(2, decidedByUserId);

                if (decisionReason != null && !decisionReason.isBlank()) {
                    stmt.setString(3, decisionReason);
                } else {
                    stmt.setNull(3, Types.LONGVARCHAR);
                }

                stmt.setInt(4, leaveId);
                stmt.setInt(5, pendingStatusId);

                return stmt.executeUpdate(); // 0 or 1
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Failed to decide leaveId=" + leaveId, e);
        } finally {
            dbManager.releaseConnection(conn);
        }
    }

    // ---------------- AbstractDAO hooks ----------------

    @Override
    protected void insert(LeaveApplication entity) {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            try (PreparedStatement stmt =
                         conn.prepareStatement(
                                 INSERT_SQL,
                                 Statement.RETURN_GENERATED_KEYS)) {

                setSaveParameters(stmt, entity);
                int affected = stmt.executeUpdate();

                if (affected == 0) {
                    throw new DataAccessException(
                            "Insert leave failed, no rows affected", null);
                }

                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        entity.setId(keys.getInt(1));
                    } else {
                        throw new DataAccessException(
                                "Insert leave failed, no ID returned", null);
                    }
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error inserting leave", e);
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
    protected void setSaveParameters(
            PreparedStatement stmt,
            LeaveApplication entity) throws SQLException {

        stmt.setInt(1, entity.getEmployeeId());
        stmt.setTimestamp(2,
                Timestamp.valueOf(entity.getStartDateTime()));
        stmt.setTimestamp(3,
                Timestamp.valueOf(entity.getEndDateTime()));
        stmt.setInt(4, mapTypeToId(entity.getType()));
        stmt.setInt(5, mapStatusToId(entity.getStatus()));
        stmt.setString(6, entity.getReason());
    }

    // ---------------- Helpers ----------------

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

        leave.setType(mapIdToType(rs.getInt("type_id")));
        leave.setStatus(mapIdToStatus(rs.getInt("status_id")));
        leave.setReason(rs.getString("reason"));

        return leave;
    }

    // ---------------- Enum ↔ DB ID ----------------

    private int mapTypeToId(LeaveApplicationDTO.LeaveType type) {
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
            default -> throw new IllegalArgumentException(
                    "Unknown leave type id: " + id);
        };
    }

    private int mapStatusToId(LeaveApplicationDTO.LeaveStatus status) {
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
            default -> throw new IllegalArgumentException(
                    "Unknown leave status id: " + id);
        };
    }


    @Override
    public void updateStatus(int leaveId,
                             int statusId,
                             Integer decidedByUserId,
                             String decisionReason) {

        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement("""
            UPDATE leave_applications
            SET status_id = ?,
                decided_by_user_id = ?,
                decision_reason = ?,
                decided_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """)) {

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

                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Failed to update status for leaveId=" + leaveId, e);
        } finally {
            dbManager.releaseConnection(conn);
        }
    }


}
