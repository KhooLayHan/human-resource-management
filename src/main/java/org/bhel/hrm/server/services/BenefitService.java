package org.bhel.hrm.server.services;

import org.bhel.hrm.common.dtos.BenefitPlanDTO;
import org.bhel.hrm.server.config.DatabaseManager;
import org.bhel.hrm.server.daos.BenefitPlanDAO;
import org.bhel.hrm.server.domain.BenefitPlan;
import org.bhel.hrm.server.mapper.BenefitPlanMapper;

import java.util.List;

public class BenefitService {
    private final DatabaseManager dbManager;
    private final BenefitPlanDAO benefitDAO;

    public BenefitService(DatabaseManager dbManager, BenefitPlanDAO benefitDAO) {
        this.dbManager = dbManager;
        this.benefitDAO = benefitDAO;
    }

    public List<BenefitPlanDTO> getAllPlans() {
        List<BenefitPlan> plans = benefitDAO.findAll();
        return plans.stream().map(BenefitPlanMapper::mapToDto).toList();
    }
}