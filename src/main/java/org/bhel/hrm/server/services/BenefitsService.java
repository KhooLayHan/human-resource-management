package org.bhel.hrm.server.services;

import org.bhel.hrm.common.dtos.BenefitPlanDTO;
import org.bhel.hrm.common.exceptions.HRMException;

import java.util.List;

public interface BenefitsService {
    List<BenefitPlanDTO> getAllBenefitPlans();
    List<BenefitPlanDTO> getMyBenefitPlans(int employeeId) throws HRMException;
    void enrollInBenefitPlan(int employeeId, int planId) throws HRMException;
}
