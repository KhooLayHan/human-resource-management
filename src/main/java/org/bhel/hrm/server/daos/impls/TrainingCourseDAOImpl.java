package org.bhel.hrm.server.daos.impls;

import org.bhel.hrm.server.config.DatabaseManager;
import org.bhel.hrm.server.daos.AbstractDAO;
import org.bhel.hrm.server.daos.TrainingCourseDAO;
import org.bhel.hrm.server.domain.TrainingCourse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class TrainingCourseDAOImpl extends AbstractDAO<TrainingCourse> implements TrainingCourseDAO {
    private static final Logger logger = LoggerFactory.getLogger(TrainingCourseDAOImpl.class);

    public TrainingCourseDAOImpl(DatabaseManager dbManager) {
        super(dbManager);
    }

    @Override
    public Optional<TrainingCourse> findById(Integer integer) {
        return Optional.empty();
    }

    @Override
    public List<TrainingCourse> findAll() {
        return List.of();
    }

    @Override
    public void save(TrainingCourse course) {
        if (course.getId() == 0)
            insert(course);
        else
            update(course);
    }

    @Override
    protected void insert(TrainingCourse course) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    protected void update(TrainingCourse course) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    protected void setSaveParameters(PreparedStatement stmt, TrainingCourse entity) throws SQLException {
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
