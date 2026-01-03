package org.bhel.hrm.server.daos.impls;

import org.bhel.hrm.common.dtos.TrainingCourseDTO;
import org.bhel.hrm.common.dtos.TrainingEnrollmentDTO;
import org.bhel.hrm.common.exceptions.DataAccessException;
import org.bhel.hrm.server.config.DatabaseManager;
import org.bhel.hrm.server.daos.AbstractDAO;
import org.bhel.hrm.server.daos.TrainingCourseDAO;
import org.bhel.hrm.server.domain.TrainingCourse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.List;
import java.util.Optional;

public class TrainingCourseDAOImpl extends AbstractDAO<TrainingCourse> implements TrainingCourseDAO {
    private static final Logger logger = LoggerFactory.getLogger(TrainingCourseDAOImpl.class);

    // This mapper tells Java how to convert a row from the DB table into a TrainingCourse object
    private final RowMapper<TrainingCourse> rowMapper = rs -> new TrainingCourse(
            rs.getInt("id"),
            rs.getString("title"),
            rs.getString("description"),
            rs.getInt("duration_in_hours"),
            mapRole(rs.getObject("department", Integer.class))
    );

    public TrainingCourseDAOImpl(DatabaseManager dbManager) {
        super(dbManager);
    }

    @Override
    public Optional<TrainingCourse> findById(Integer id) {
        String sql = "SELECT * FROM training_courses WHERE id = ?";
        return findOne(sql, stmt -> stmt.setInt(1, id), rowMapper);
    }

    @Override
    public List<TrainingCourse> findAll() {
        String sql = "SELECT * FROM training_courses ORDER BY title ASC";
        return findMany(sql, stmt -> {}, rowMapper);
    }

    @Override
    public void save(TrainingCourse course) {
        if (course.getId() == 0) {
            insert(course);
        } else {
                                                                        update(course);
        }
    }

    @Override
    protected void insert(TrainingCourse course) {
        String sql = """
                    INSERT INTO
                        training_courses (
                            title,
                            description,
                            duration_in_hours,
                            department
                        )
                    VALUES (
                        ?,
                        ?,
                        ?,
                        ?
                    )
                """;


        // We use a manual try-catch here because we need the Generated Keys (the new ID)
        Connection conn = null;

            try {
                conn = dbManager.getConnection();

                try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    setSaveParameters(stmt, course);
                    stmt.executeUpdate();

                    // Get the new ID from the database and set it on our object
                    try (ResultSet keys = stmt.getGeneratedKeys()) {
                        if (keys.next()) course.setId(keys.getInt(1));
                    }
                }
            } catch (SQLException e) {
                // Now this catches errors from getConnection() AND the query execution
                throw new DataAccessException("Error inserting new training course", e);
            } finally {
                dbManager.releaseConnection(conn);
            }
        }

    @Override
    protected void update(TrainingCourse course) {
        String sql = """
                UPDATE training_courses SET title = ?,
                    description = ?,
                    duration_in_hours = ?,
                    department = ?
                    WHERE id = ?
                """;

        executeUpdate(sql, stmt -> {
            // 1. Set the common fields first
            setSaveParameters(stmt, course);
            // 2. Set the ID (the 5th parameter) manually
            stmt.setInt(5, course.getId());
        });
    }

    @Override
    protected void setSaveParameters(PreparedStatement stmt, TrainingCourse course) throws SQLException {
        stmt.setString(1, course.getTitle());
        stmt.setString(2, course.getDescription());
        stmt.setInt(3, course.getDurationInHours());


        int department;
        if (course.getDepartment() == null) {
            department = 5; // Default fallback if null
        } else {
            switch (course.getDepartment()) {
                case TrainingCourseDTO.Department.IT -> department = 1;
                case TrainingCourseDTO.Department.HR -> department = 2;
                case TrainingCourseDTO.Department.FINANCE -> department = 3;
                case TrainingCourseDTO.Department.OPERATIONS -> department = 4;
                default -> department = 5;
            }
        }

        stmt.setInt(4, department);
    }

    // Now, update your save() method to look like this.
    // It uses setSaveParameters for the INSERT logic to keep it clean.

    @Override
    public void deleteById(Integer id) {
        String sql = "DELETE FROM training_courses WHERE id = ?";
        executeUpdate(sql, stmt -> stmt.setInt(1, id));
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM training_courses";
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error counting training courses", e);
        } finally {
            dbManager.releaseConnection(conn);
        }
        return 0;
    }

    private static TrainingCourseDTO.Department mapRole(Integer departmentId) {
        if (departmentId == null)
            throw new IllegalStateException("training_courses.department_id is NULL");

        return switch (departmentId) {
            case 1 -> TrainingCourseDTO.Department.IT;
            case 2 -> TrainingCourseDTO.Department.HR;
            case 3 -> TrainingCourseDTO.Department.FINANCE;
            case 4 -> TrainingCourseDTO.Department.OPERATIONS;
            case 5 -> TrainingCourseDTO.Department.SALES;

            default -> throw new IllegalArgumentException("Unknown training_courses.department_id=" + departmentId);
        };
    }

}

