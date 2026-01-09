package org.bhel.hrm.server.daos;

import org.bhel.hrm.server.domain.BenefitPlan;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for BenefitPlan entities.
 * Manages the catalog of available benefit plans.
 */
public interface BenefitPlanDAO extends DAO<BenefitPlan, Integer> {

    /**
     * Finds benefit plans by provider.
     */
    List<BenefitPlan> findByProvider(String provider);

    /**
     * Finds a benefit plan by plan name.
     */
    Optional<BenefitPlan> findByPlanName(String planName);
}
