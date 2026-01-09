package org.bhel.hrm.server.daos.impls;

import org.bhel.hrm.common.exceptions.DataAccessException;
import org.bhel.hrm.server.config.DatabaseManager;
import org.bhel.hrm.server.daos.EmployeeBenefitDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class EmployeeBenefitDAOImpl implements EmployeeBenefitDAO {

    private final DatabaseManager dbManager;

    public EmployeeBenefitDAOImpl(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public void enroll(int employeeId, int planId) {
        String sql = """
            INSERT INTO employee_benefits (employee_id, plan_id)
            VALUES (?, ?)
        """;

        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, employeeId);
                stmt.setInt(2, planId);
                stmt.executeUpdate();
            }
        } catch (Exception e) {
            throw new DataAccessException("Failed to enroll employee in benefit plan.", e);
        } finally {
            dbManager.releaseConnection(conn);
        }
    }

    @Override
    public List<Integer> findPlansForEmployee(int employeeId) {
        String sql = """
            SELECT plan_id
            FROM employee_benefits
            WHERE employee_id = ?
            ORDER BY plan_id
        """;

        List<Integer> planIds = new ArrayList<>();
        Connection conn = null;

        try {
            conn = dbManager.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, employeeId);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        planIds.add(rs.getInt("plan_id"));
                    }
                }
            }
            return planIds;

        } catch (Exception e) {
            throw new DataAccessException("Failed to fetch benefit plans for employeeId=" + employeeId, e);
        } finally {
            dbManager.releaseConnection(conn);
        }
    }
}
