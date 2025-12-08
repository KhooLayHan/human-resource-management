package org.bhel.hrm.server.daos.impls;

import org.bhel.hrm.server.config.DatabaseManager;
import org.bhel.hrm.server.daos.AbstractDAO;
import org.bhel.hrm.server.daos.LeaveApplicationDAO;
import org.bhel.hrm.server.domain.LeaveApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class LeaveApplicationDAOImpl extends AbstractDAO<LeaveApplication> implements LeaveApplicationDAO {
    private static final Logger logger = LoggerFactory.getLogger(LeaveApplicationDAOImpl.class);

    public LeaveApplicationDAOImpl(DatabaseManager dbManager) {
        super(dbManager);
    }

    @Override
    public List<LeaveApplication> findByEmployeeId(int employeeId) {
        return List.of();
    }

    @Override
    public Optional<LeaveApplication> findById(Integer integer) {
        return Optional.empty();
    }

    @Override
    public List<LeaveApplication> findAll() {
        return List.of();
    }

    @Override
    public void save(LeaveApplication entity) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    protected void insert(LeaveApplication entity) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    protected void update(LeaveApplication entity) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    protected void setSaveParameters(PreparedStatement stmt, LeaveApplication entity) throws SQLException {
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
