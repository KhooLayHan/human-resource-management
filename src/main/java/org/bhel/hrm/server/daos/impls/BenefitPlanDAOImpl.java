package org.bhel.hrm.server.daos.impls;

import org.bhel.hrm.common.exceptions.DataAccessException;
import org.bhel.hrm.server.config.DatabaseManager;
import org.bhel.hrm.server.daos.AbstractDAO;
import org.bhel.hrm.server.daos.BenefitPlanDAO;
import org.bhel.hrm.server.domain.BenefitPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.util.List;
import java.util.Optional;

public class BenefitPlanDAOImpl extends AbstractDAO<BenefitPlan> implements BenefitPlanDAO {
    private static final Logger logger = LoggerFactory.getLogger(BenefitPlanDAOImpl.class);

    private static final String TABLE_NAME = "benefit_plans";

    private static final String SELECT_BASE = """
        SELECT
            id,
            plan_name,
            provider,
            description,
            cost_per_month
        FROM benefit_plans
        """;

    public BenefitPlanDAOImpl(DatabaseManager dbManager) {
        super(dbManager);
    }

    // ---------------- DAO<BenefitPlan, Integer> ----------------

    @Override
    public Optional<BenefitPlan> findById(Integer id) {
        String sql = SELECT_BASE + " WHERE id = ?";
        return findOne(sql,
                stmt -> stmt.setInt(1, id),
                this::mapRow);
    }

    @Override
    public List<BenefitPlan> findAll() {
        String sql = SELECT_BASE + " ORDER BY plan_name ASC";
        return findMany(sql,
                stmt -> { /* no params */ },
                this::mapRow);
    }

    @Override
    public void save(BenefitPlan entity) {
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
            throw new DataAccessException("Error counting benefit plans", e);
        } finally {
            dbManager.releaseConnection(conn);
        }
    }

    // ---------------- BenefitPlanDAO custom methods ----------------

    @Override
    public List<BenefitPlan> findByProvider(String provider) {
        String sql = SELECT_BASE + " WHERE provider = ? ORDER BY plan_name ASC";
        return findMany(sql,
                stmt -> stmt.setString(1, provider),
                this::mapRow);
    }

    @Override
    public Optional<BenefitPlan> findByPlanName(String planName) {
        String sql = SELECT_BASE + " WHERE plan_name = ?";
        return findOne(sql,
                stmt -> stmt.setString(1, planName),
                this::mapRow);
    }

    // ---------------- AbstractDAO hooks ----------------

    @Override
    protected void insert(BenefitPlan entity) {
        String sql = """
            INSERT INTO benefit_plans
                (plan_name, provider, description, cost_per_month)
            VALUES (?, ?, ?, ?)
            """;

        Connection conn = null;
        try {
            conn = dbManager.getConnection();

            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                setSaveParameters(stmt, entity);

                int affectedRows = stmt.executeUpdate();
                logger.info("Executed insert: {}", stmt);

                if (affectedRows == 0) {
                    throw new DataAccessException("Inserting benefit plan failed, no rows affected.", null);
                }

                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        entity.setId(keys.getInt(1));
                    } else {
                        throw new DataAccessException("Inserting benefit plan failed, no ID obtained.", null);
                    }
                }

            }
        } catch (SQLException e) {
            throw new DataAccessException("Error inserting benefit plan", e);
        } finally {
            dbManager.releaseConnection(conn);
        }
    }

    @Override
    protected void update(BenefitPlan entity) {
        String sql = """
            UPDATE benefit_plans
            SET plan_name = ?, provider = ?, description = ?, cost_per_month = ?
            WHERE id = ?
            """;

        executeUpdate(sql, stmt -> {
            setSaveParameters(stmt, entity);
            stmt.setInt(5, entity.getId());
        });
    }

    @Override
    protected void setSaveParameters(PreparedStatement stmt, BenefitPlan entity) throws SQLException {
        stmt.setString(1, entity.getPlanName());
        stmt.setString(2, entity.getProvider());
        stmt.setString(3, entity.getDescription());

        BigDecimal cost = entity.getCostPerMonth();
        if (cost != null) {
            stmt.setBigDecimal(4, cost);
        } else {
            stmt.setNull(4, Types.DECIMAL);
        }
    }

    // ---------------- Row mapping ----------------

    private BenefitPlan mapRow(ResultSet rs) throws SQLException {
        BenefitPlan plan = new BenefitPlan();
        plan.setId(rs.getInt("id"));
        plan.setPlanName(rs.getString("plan_name"));
        plan.setProvider(rs.getString("provider"));
        plan.setDescription(rs.getString("description"));
        plan.setCostPerMonth(rs.getBigDecimal("cost_per_month"));
        return plan;
    }
}
