package org.bhel.hrm.server.daos.impls;

import org.bhel.hrm.server.config.DatabaseManager;
import org.bhel.hrm.server.daos.AbstractDAO;
import org.bhel.hrm.server.daos.ApplicantDAO;
import org.bhel.hrm.server.domain.Applicant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class ApplicantDAOImpl extends AbstractDAO<Applicant> implements ApplicantDAO {
    private static final Logger logger = LoggerFactory.getLogger(ApplicantDAOImpl.class);

    public ApplicantDAOImpl(DatabaseManager dbManager) {
        super(dbManager);
    }

    @Override
    public List<Applicant> findByJobOpeningId(int jobOpeningId) {
        return List.of();
    }

    @Override
    public Optional<Applicant> findById(Integer integer) {
        return Optional.empty();
    }

    @Override
    public List<Applicant> findAll() {
        return List.of();
    }

    @Override
    public void save(Applicant entity) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    protected void insert(Applicant entity) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    protected void update(Applicant entity) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    protected void setSaveParameters(PreparedStatement stmt, Applicant entity) throws SQLException {
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
