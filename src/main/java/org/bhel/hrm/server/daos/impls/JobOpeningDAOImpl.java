package org.bhel.hrm.server.daos.impls;

import org.bhel.hrm.common.dtos.JobOpeningDTO.JobStatus;
import org.bhel.hrm.common.error.ExceptionMappingConfig;
import org.bhel.hrm.common.exceptions.DataAccessException;
import org.bhel.hrm.server.config.DatabaseManager;
import org.bhel.hrm.server.daos.AbstractDAO;
import org.bhel.hrm.server.daos.JobOpeningDAO;
import org.bhel.hrm.server.domain.JobOpening;

import java.sql.*;
import java.util.List;
import java.util.Optional;

public class JobOpeningDAOImpl extends AbstractDAO<JobOpening> implements JobOpeningDAO {

    private final ExceptionMappingConfig exceptionMapper;

    // Mapper to convert SQL ResultSet to JobOpening Object
    private final RowMapper<JobOpening> rowMapper = rs -> {
        JobOpening job = new JobOpening();
        job.setId(rs.getInt("id"));
        job.setTitle(rs.getString("title"));
        job.setDescription(rs.getString("description"));
        job.setDepartment(rs.getString("department"));
        job.setStatus(mapStatus(rs.getInt("status_id")));

        // Handle potentially null dates
        Date posted = rs.getDate("posted_date");
        if (posted != null) job.setPostedDate(posted.toLocalDate());

        Date closing = rs.getDate("closing_date");
        if (closing != null) job.setClosingDate(closing.toLocalDate());

        return job;
    };

    public JobOpeningDAOImpl(DatabaseManager dbManager, ExceptionMappingConfig exceptionMapper) {
        super(dbManager);
        this.exceptionMapper = exceptionMapper;
    }

    @Override
    public Optional<JobOpening> findById(Integer id) {
        String sql = "SELECT * FROM job_openings WHERE id = ?";
        return findOne(sql, stmt -> stmt.setInt(1, id), rowMapper);
    }

    @Override
    public List<JobOpening> findAll() {
        String sql = "SELECT * FROM job_openings ORDER BY created_at DESC";
        return findMany(sql, stmt -> {}, rowMapper);
    }

    @Override
    public List<JobOpening> findAllByStatus(JobStatus status) {
        String sql = "SELECT * FROM job_openings WHERE status_id = ?";
        return findMany(sql, stmt -> stmt.setInt(1, mapStatusToId(status)), rowMapper);
    }

    @Override
    public void save(JobOpening job) {
        if (job.getId() == 0) {
            insert(job);
        } else {
            update(job);
        }
    }

    @Override
    protected void insert(JobOpening job) {
        String sql = """
            INSERT INTO job_openings
            (title, description, department, status_id, posted_date, closing_date)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        Connection conn = null;

        try {
            conn = dbManager.getConnection();

            System.out.println("JERE");

            // Error here actually...
//            PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            System.out.println("DAY");

            // Special handling for INSERT to get Generated Keys (Not using AbstractDAO template here)
//            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
//
//                setSaveParameters(stmt, job);
//                stmt.executeUpdate();
//
//                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
//                    if (generatedKeys.next()) {
//                        job.setId(generatedKeys.getInt(1));
//                    }
//                }
//            }
        } catch (SQLException e) {
            throw new DataAccessException("Error inserting new job openings " + job.getId(), e);
        }
//        finally {
//            dbManager.releaseConnection(conn);
//        }
    }

    @Override
    protected void update(JobOpening job) {
        String sql = """
            UPDATE job_openings
            SET title=?, description=?, department=?, status_id=?, posted_date=?, closing_date=? 
            WHERE id=?
        """;
        executeUpdate(sql, stmt -> {
            setSaveParameters(stmt, job);
            stmt.setInt(7, job.getId());
        });
    }

    @Override
    public void deleteById(Integer id) {
        String sql = "DELETE FROM job_openings WHERE id = ?";
        executeUpdate(sql, stmt -> stmt.setInt(1, id));
    }

    @Override
    public long count() {
        // Implementation similar to UserDAO count...
        return 0; // Placeholder
    }

    // Helper to map object fields to SQL statement
    protected void setSaveParameters(PreparedStatement stmt, JobOpening job) throws SQLException {
        stmt.setString(1, job.getTitle());
        stmt.setString(2, job.getDescription());
        stmt.setString(3, job.getDepartment());
        stmt.setInt(4, mapStatusToId(job.getStatus()));
        stmt.setDate(5, job.getPostedDate() != null ? Date.valueOf(job.getPostedDate()) : null);
        stmt.setDate(6, job.getClosingDate() != null ? Date.valueOf(job.getClosingDate()) : null);
    }

    // --- Enum Mappers ---

    private JobStatus mapStatus(int statusId) {
        return switch (statusId) {
            case 1 -> JobStatus.OPEN;
            case 2 -> JobStatus.CLOSED;
            case 3 -> JobStatus.ON_HOLD;
            default -> JobStatus.OPEN; // Default fallback
        };
    }

    private int mapStatusToId(JobStatus status) {
        return switch (status) {
            case OPEN -> 1;
            case CLOSED -> 2;
            case ON_HOLD -> 3;
        };
    }
}