package org.bhel.hrm.server.daos.impls;

import org.bhel.hrm.common.dtos.ApplicantDTO.ApplicantStatus;
import org.bhel.hrm.common.error.ExceptionMappingConfig;
import org.bhel.hrm.common.exceptions.DataAccessException;
import org.bhel.hrm.server.config.DatabaseManager;
import org.bhel.hrm.server.daos.AbstractDAO;
import org.bhel.hrm.server.daos.ApplicantDAO;
import org.bhel.hrm.server.domain.Applicant;

import java.sql.*;
import java.util.List;
import java.util.Optional;

public class ApplicantDAOImpl extends AbstractDAO<Applicant> implements ApplicantDAO {

    private final RowMapper<Applicant> rowMapper = rs -> {
        Applicant app = new Applicant();
        app.setId(rs.getInt("id"));
        app.setJobOpeningId(rs.getInt("job_opening_id"));
        app.setFullName(rs.getString("full_name"));
        app.setEmail(rs.getString("email"));
        app.setPhone(rs.getString("phone"));
        app.setResumeUrl(rs.getString("resume_url"));
        app.setStatus(mapStatus(rs.getInt("status_id")));
        return app;
    };

    public ApplicantDAOImpl(DatabaseManager dbManager, ExceptionMappingConfig exceptionMapper) {
        super(dbManager);
    }

    @Override
    public Optional<Applicant> findById(Integer id) {
        String sql = "SELECT * FROM applicants WHERE id = ?";
        return findOne(sql, stmt -> stmt.setInt(1, id), rowMapper);
    }

    @Override
    public List<Applicant> findAll() {
        String sql = "SELECT * FROM applicants ORDER BY full_name ASC";
        return findMany(sql, stmt -> {}, rowMapper);
    }

    @Override
    public List<Applicant> findByJobOpeningId(int jobOpeningId) {
        String sql = "SELECT * FROM applicants WHERE job_opening_id = ?";
        return findMany(sql, stmt -> stmt.setInt(1, jobOpeningId), rowMapper);
    }

    @Override
    public void save(Applicant applicant) {
        if (applicant.getId() == 0) {
            insert(applicant);
        } else {
            update(applicant);
        }
    }

    @Override
    protected void insert(Applicant app) {
        String sql = """
            INSERT INTO applicants 
            (job_opening_id, full_name, email, phone, resume_url, status_id) 
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        Connection conn = null;

        try {
            conn = dbManager.getConnection();

            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                setSaveParameters(stmt, app);
                stmt.executeUpdate();

                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) app.setId(generatedKeys.getInt(1));
                }
            }

        } catch (SQLException e) {
            throw new DataAccessException("Error inserting new users" + app.getFullName(), e);
        }
//        finally {
//            dbManager.releaseConnection(conn);
//        }
    }

    @Override
    protected void update(Applicant app) {
        String sql = """
            UPDATE applicants 
            SET job_opening_id=?, full_name=?, email=?, phone=?, resume_url=?, status_id=? 
            WHERE id=?
        """;
        executeUpdate(sql, stmt -> {
            setSaveParameters(stmt, app);
            stmt.setInt(7, app.getId());
        });
    }

    @Override
    public void deleteById(Integer id) {
        String sql = "DELETE FROM applicants WHERE id = ?";
        executeUpdate(sql, stmt -> stmt.setInt(1, id));
    }

    @Override
    public long count() { return 0; } // Implement if needed

    protected void setSaveParameters(PreparedStatement stmt, Applicant app) throws SQLException {
        stmt.setInt(1, app.getJobOpeningId());
        stmt.setString(2, app.getFullName());
        stmt.setString(3, app.getEmail());
        stmt.setString(4, app.getPhone());
        stmt.setString(5, app.getResumeUrl());
        stmt.setInt(6, mapStatusToId(app.getStatus()));
    }

    // --- Enum Mappers ---

    private ApplicantStatus mapStatus(int statusId) {
        return switch (statusId) {
            case 1 -> ApplicantStatus.NEW;
            case 2 -> ApplicantStatus.SCREENING;
            case 3 -> ApplicantStatus.INTERVIEWING;
            case 4 -> ApplicantStatus.OFFERED;
            case 5 -> ApplicantStatus.HIRED;
            case 6 -> ApplicantStatus.REJECTED;
            default -> ApplicantStatus.NEW;
        };
    }

    private int mapStatusToId(ApplicantStatus status) {
        return switch (status) {
            case NEW -> 1;
            case SCREENING -> 2;
            case INTERVIEWING -> 3;
            case OFFERED -> 4;
            case HIRED -> 5;
            case REJECTED -> 6;
        };
    }
}