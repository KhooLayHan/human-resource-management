package org.bhel.hrm.server.services.impls;

import org.bhel.hrm.common.dtos.BenefitPlanDTO;
import org.bhel.hrm.server.daos.BenefitPlanDAO;
import org.bhel.hrm.server.daos.EmployeeBenefitDAO;
import org.bhel.hrm.server.daos.EmployeeDAO;
import org.bhel.hrm.server.domain.BenefitPlan;
import org.bhel.hrm.server.services.BenefitsService;

import java.util.List;

public class BenefitsServiceImpl implements BenefitsService {
    private final BenefitPlanDAO benefitPlanDAO;
    private final EmployeeBenefitDAO employeeBenefitDAO;
    private final EmployeeDAO employeeDAO;

    public BenefitsServiceImpl(BenefitPlanDAO benefitPlanDAO, EmployeeBenefitDAO employeeBenefitDAO, EmployeeDAO employeeDAO) {
        this.benefitPlanDAO = benefitPlanDAO;
        this.employeeBenefitDAO = employeeBenefitDAO;
        this.employeeDAO = employeeDAO;
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

        // ✅ Validate employee exists
        employeeDAO.findById(employeeId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Employee not found: " + employeeId)
                );

        // ✅ Validate plan exists
        BenefitPlan plan = benefitPlanDAO.findById(planId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Benefit plan not found: " + planId)
                );

        employeeBenefitDAO.enroll(employeeId, plan.getId());
    }




}
