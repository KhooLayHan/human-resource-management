package org.bhel.hrm.server.daos.impls;

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

    // ---------------- SQL fragments ----------------

    private static final String SELECT_ALL = """
        SELECT
            id,
            plan_name,
            provider,
            description,
            cost_per_month
        FROM %s
        """.formatted(TABLE_NAME);

    private static final String ORDER_BY_PLAN_NAME_ASC = " ORDER BY plan_name ASC";

    private static final String WHERE_ID = " WHERE id = ?";
    private static final String WHERE_PROVIDER = " WHERE provider = ?";
    private static final String WHERE_PLAN_NAME = " WHERE plan_name = ?";

    private static final String DELETE_BY_ID_SQL =
            "DELETE FROM " + TABLE_NAME + WHERE_ID;

    private static final String INSERT_SQL = """
        INSERT INTO %s
            (plan_name, provider, description, cost_per_month)
        VALUES (?, ?, ?, ?)
        """.formatted(TABLE_NAME);

    private static final String UPDATE_SQL = """
        UPDATE %s
        SET plan_name = ?, provider = ?, description = ?, cost_per_month = ?
        WHERE id = ?
        """.formatted(TABLE_NAME);

    public BenefitPlanDAOImpl(DatabaseManager dbManager) {
        super(dbManager);
    }

    // ---------------- DAO<BenefitPlan, Integer> ----------------

    @Override
    public Optional<BenefitPlan> findById(Integer id) {
        return findOne(SELECT_ALL + WHERE_ID,
                stmt -> stmt.setInt(1, id),
                this::mapRow);
    }

    @Override
    public List<BenefitPlan> findAll() {
        return findMany(SELECT_ALL + ORDER_BY_PLAN_NAME_ASC,
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
        executeUpdate(DELETE_BY_ID_SQL, stmt -> stmt.setInt(1, id));
    }

    @Override
    public long count() {
        // ✅ no duplicated boilerplate anymore
        return countFromTable(TABLE_NAME);
    }

    // ---------------- BenefitPlanDAO custom methods ----------------

    @Override
    public List<BenefitPlan> findByProvider(String provider) {
        return findByStringFieldOrdered(WHERE_PROVIDER, provider);
    }

    @Override
    public Optional<BenefitPlan> findByPlanName(String planName) {
        return findOne(SELECT_ALL + WHERE_PLAN_NAME,
                stmt -> stmt.setString(1, planName),
                this::mapRow);
    }

    // ---------------- AbstractDAO hooks ----------------

    @Override
    protected void insert(BenefitPlan entity) {
        int newId = executeInsertReturningId(
                INSERT_SQL,
                stmt -> setSaveParameters(stmt, entity),
                "Inserting benefit plan failed, no rows affected.",
                "Inserting benefit plan failed, no ID obtained.",
                "Error inserting benefit plan"
        );

        entity.setId(newId);
        logger.info("Inserted benefit plan id={}", newId);
    }

    @Override
    protected void update(BenefitPlan entity) {
        executeUpdate(UPDATE_SQL, stmt -> {
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

    // ---------------- Helpers (duplication reducers) ----------------

    private List<BenefitPlan> findByStringFieldOrdered(String whereClauseWithPlaceholder, String value) {
        String sql = SELECT_ALL + whereClauseWithPlaceholder + ORDER_BY_PLAN_NAME_ASC;
        return findMany(sql,
                stmt -> stmt.setString(1, value),
                this::mapRow);
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
