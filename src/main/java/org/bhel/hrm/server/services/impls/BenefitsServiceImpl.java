package org.bhel.hrm.server.services.impls;

import org.bhel.hrm.common.dtos.BenefitPlanDTO;
import org.bhel.hrm.server.daos.BenefitPlanDAO;
import org.bhel.hrm.server.daos.EmployeeBenefitDAO;
import org.bhel.hrm.server.domain.BenefitPlan;
import org.bhel.hrm.server.services.BenefitsService;

import java.util.List;

public class BenefitsServiceImpl implements BenefitsService {
    private final BenefitPlanDAO benefitPlanDAO;
    private final EmployeeBenefitDAO employeeBenefitDAO;

    public BenefitsServiceImpl(BenefitPlanDAO benefitPlanDAO, EmployeeBenefitDAO employeeBenefitDAO) {
        this.benefitPlanDAO = benefitPlanDAO;
        this.employeeBenefitDAO = employeeBenefitDAO;
    }

    @Override
    public List<BenefitPlanDTO> getAllBenefitPlans() {
        return benefitPlanDAO.findAll().stream()
                .map(p -> new BenefitPlanDTO(
                        p.getId(),
                        p.getPlanName(),
                        p.getProvider(),
                        p.getDescription(),
                        p.getCostPerMonth()
                ))
                .toList();
    }

    @Override
    public List<BenefitPlanDTO> getMyBenefitPlans(int employeeId) {
        return employeeBenefitDAO.findPlansForEmployee(employeeId).stream()
                .map(benefitPlanDAO::findById)          // Optional<BenefitPlan>
                .flatMap(java.util.Optional::stream)    // BenefitPlan
                .map(p -> new BenefitPlanDTO(
                        p.getId(),
                        p.getPlanName(),
                        p.getProvider(),
                        p.getDescription(),
                        p.getCostPerMonth()
                ))
                .toList();
    }


    @Override
    public void enrollInBenefitPlan(int employeeId, int planId) {
        // Validate plan exists
        BenefitPlan plan = benefitPlanDAO.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Benefit plan not found: " + planId));

        // Optionally: validate employee exists via EmployeeDAO
        // employeeDAO.findById(employeeId) ...

        employeeBenefitDAO.enroll(employeeId, plan.getId());
    }



}
