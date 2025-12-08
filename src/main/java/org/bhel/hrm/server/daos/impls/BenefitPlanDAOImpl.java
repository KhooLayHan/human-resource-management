package org.bhel.hrm.server.daos.impls;

import org.bhel.hrm.server.config.DatabaseManager;
import org.bhel.hrm.server.daos.AbstractDAO;
import org.bhel.hrm.server.daos.BenefitPlanDAO;
import org.bhel.hrm.server.domain.BenefitPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class BenefitPlanDAOImpl extends AbstractDAO<BenefitPlan> implements BenefitPlanDAO {
    private static final Logger logger = LoggerFactory.getLogger(BenefitPlanDAOImpl.class);

    public BenefitPlanDAOImpl(DatabaseManager dbManager) {
        super(dbManager);
    }

    @Override
    public Optional<BenefitPlan> findById(Integer integer) {
        return Optional.empty();
    }

    @Override
    public List<BenefitPlan> findAll() {
        return List.of();
    }

    @Override
    public void save(BenefitPlan entity) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    protected void insert(BenefitPlan entity) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    protected void update(BenefitPlan entity) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    protected void setSaveParameters(PreparedStatement stmt, BenefitPlan entity) throws SQLException {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public void deleteById(Integer integer) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public long count() {
        return 0;
    }
}
