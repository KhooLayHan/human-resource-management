package org.bhel.hrm.server.daos.impls;

import org.bhel.hrm.common.dtos.TrainingEnrollmentDTO;
import org.bhel.hrm.common.error.ExceptionMappingConfig;
import org.bhel.hrm.common.exceptions.DataAccessException;
import org.bhel.hrm.server.config.DatabaseManager;
import org.bhel.hrm.server.daos.AbstractDAO;
import org.bhel.hrm.server.daos.TrainingEnrollmentDAO;
import org.bhel.hrm.server.domain.TrainingEnrollment;

import java.sql.*;
import java.util.List;
import java.util.Optional;
public class TrainingEnrollmentDAOImpl extends AbstractDAO<TrainingEnrollment> implements TrainingEnrollmentDAO {

    private ExceptionMappingConfig config;

    private final RowMapper<TrainingEnrollment> rowMapper = rs -> {
        Timestamp enrollmentTs = rs.getTimestamp("enrollment_date");

        return new TrainingEnrollment(
            rs.getInt("id"),
            rs.getInt("employee_id"),
            rs.getInt("course_id"),
            enrollmentTs != null ? enrollmentTs.toLocalDateTime() : null,
            mapStatus(rs.getObject("status_id", Integer.class))
        );
    };

    public TrainingEnrollmentDAOImpl(DatabaseManager dbManager) {
        super(dbManager);
    }

    @Override
    public List<TrainingEnrollment> findByEmployeeId(int employeeId) {
        String sql = "SELECT * FROM training_enrollments WHERE employee_id = ?";
        return findMany(sql, stmt -> stmt.setInt(1, employeeId), rowMapper);
    }

    @Override
    public List<TrainingEnrollment> findByCourseId(int courseId) {
        String sql = "SELECT * FROM training_enrollments WHERE course_id = ?";
        return findMany(sql, stmt -> stmt.setInt(1, courseId), rowMapper);
    }

    @Override
    public Optional<TrainingEnrollment> findById(Integer id) {
        if (id == null)
            throw new IllegalArgumentException("id must not be null");

        String sql = "SELECT * FROM training_enrollments WHERE id = ?";
        return findOne(sql, stmt -> stmt.setInt(1, id), rowMapper);
    }

    @Override
    public List<TrainingEnrollment> findAll() {
        String sql = "SELECT * FROM training_enrollments";
        return findMany(sql, stmt -> {}, rowMapper);
    }

    @Override
    public void deleteById(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }

        String sql = "DELETE FROM training_enrollments WHERE id = ?";
        executeUpdate(sql, stmt -> stmt.setInt(1, id));
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM training_enrollments";
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error counting enrollments", e);
        } finally {
            dbManager.releaseConnection(conn);
        }
        return 0;
    }

    @Override
    protected void setSaveParameters(PreparedStatement stmt, TrainingEnrollment enrollment) throws SQLException {
        stmt.setInt(1, enrollment.getEmployeeId());
        stmt.setInt(2, enrollment.getCourseId());

        if (enrollment.getStatus() == null) {
            stmt.setNull(3, Types.INTEGER);
        } else {
            +            int statusId = switch (enrollment.getStatus()) {
                +                case ENROLLED -> 1;
                +                case COMPLETED -> 2;
                +                case CANCELLED -> 3;
                +                case FAILED -> 4;
                +            };
            +            stmt.setInt(3, statusId);
            +        }
        stmt.setInt(3, statusId);

        if (enrollment.getEnrollmentDate() != null) {
            stmt.setTimestamp(4, Timestamp.valueOf(enrollment.getEnrollmentDate()));
        } else {
            stmt.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
        }
    }

    private static TrainingEnrollmentDTO.Status mapStatus(Integer statusId) {
        if (statusId == null)
            throw new IllegalStateException("training_enrollments.status_id is NULL");

        return switch (statusId) {
            case 1 -> TrainingEnrollmentDTO.Status.ENROLLED;
            case 2 -> TrainingEnrollmentDTO.Status.COMPLETED;
            case 3 -> TrainingEnrollmentDTO.Status.CANCELLED;
            case 4 -> TrainingEnrollmentDTO.Status.FAILED;
            default -> throw new IllegalArgumentException("Unknown training_enrollments.status_id=" + statusId);
        };
    }

    // Update your save() method to utilize it
    @Override
    public void save(TrainingEnrollment enrollment) {
        if (enrollment.getId() == 0) {
            insert(enrollment);
        } else {
            update(enrollment);
        }
    }

    @Override
    protected void insert(TrainingEnrollment enrollment) {
        String sql = """
                INSERT INTO training_enrollments (
                                employee_id,
                                course_id,
                                status_id,
                                enrollment_date
                            ) VALUES (?, ?, ?, ?)
                """;
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                setSaveParameters(stmt, enrollment);
                stmt.executeUpdate();

                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) enrollment.setId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error inserting enrollment", e);
        } finally {
            dbManager.releaseConnection(conn);
        }
    }

    @Override
    protected void update(TrainingEnrollment enrollment) {
        String sql = """
        UPDATE training_enrollments SET
                            employee_id = ?,
                            course_id = ?,
                            status_id = ?,
                            enrollment_date = ?
                            where id = ?
        """;
        executeUpdate(sql, stmt -> {
            setSaveParameters(stmt, enrollment);
            stmt.setInt(5, enrollment.getId());
        });
    }
}