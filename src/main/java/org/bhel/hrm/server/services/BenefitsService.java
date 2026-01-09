package org.bhel.hrm.server.services;

import org.bhel.hrm.common.dtos.BenefitPlanDTO;

import java.util.List;

public interface BenefitsService {
    List<BenefitPlanDTO> getAllBenefitPlans();
    void enrollInBenefitPlan(int employeeId, int planId);

    List<BenefitPlanDTO> getMyBenefitPlans(int employeeId);
}
