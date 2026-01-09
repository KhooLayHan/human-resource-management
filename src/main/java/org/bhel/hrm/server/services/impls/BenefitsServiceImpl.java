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

    public BenefitsServiceImpl(BenefitPlanDAO benefitPlanDAO,
                               EmployeeBenefitDAO employeeBenefitDAO,
                               EmployeeDAO employeeDAO) {
        this.benefitPlanDAO = benefitPlanDAO;
        this.employeeBenefitDAO = employeeBenefitDAO;
        this.employeeDAO = employeeDAO;
    }

    @Override
    public List<BenefitPlanDTO> getAllBenefitPlans() {
        return benefitPlanDAO.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public List<BenefitPlanDTO> getMyBenefitPlans(int employeeId) {
        // Validate employee exists
        employeeDAO.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));

        // employeeBenefitDAO should return plan IDs (or plan records). Based on your earlier usage:
        // employeeBenefitDAO.findPlansForEmployee(employeeId) -> List<Integer> planIds
        return employeeBenefitDAO.findPlansForEmployee(employeeId).stream()
                .map(benefitPlanDAO::findById)          // Optional<BenefitPlan>
                .flatMap(java.util.Optional::stream)   // remove missing plans safely
                .map(this::toDTO)
                .toList();
    }

    @Override
    public void enrollInBenefitPlan(int employeeId, int planId) {
        // Validate employee exists
        employeeDAO.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));

        // Validate plan exists
        BenefitPlan plan = benefitPlanDAO.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Benefit plan not found: " + planId));

        // Optional: prevent duplicate enrollment (if you have such DAO method)
        // if (employeeBenefitDAO.isEnrolled(employeeId, planId)) return;

        employeeBenefitDAO.enroll(employeeId, plan.getId());
    }

    private BenefitPlanDTO toDTO(BenefitPlan p) {
        return new BenefitPlanDTO(
                p.getId(),
                p.getPlanName(),
                p.getProvider(),
                p.getDescription(),
                p.getCostPerMonth()
        );
    }
}
