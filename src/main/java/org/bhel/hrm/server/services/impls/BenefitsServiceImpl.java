package org.bhel.hrm.server.services.impls;

import org.bhel.hrm.common.dtos.BenefitPlanDTO;
import org.bhel.hrm.server.daos.BenefitPlanDAO;
import org.bhel.hrm.server.daos.EmployeeBenefitDAO;
import org.bhel.hrm.server.daos.EmployeeDAO;
import org.bhel.hrm.server.domain.BenefitPlan;
import org.bhel.hrm.server.services.BenefitsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class BenefitsServiceImpl implements BenefitsService {

    private static final Logger logger = LoggerFactory.getLogger(BenefitsServiceImpl.class);

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
        employeeDAO.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));

        return employeeBenefitDAO.findPlansForEmployee(employeeId).stream()
                .map(planId -> {
                    Optional<BenefitPlan> opt = benefitPlanDAO.findById(planId);
                    if (opt.isEmpty()) {
                        logger.warn("Orphaned benefit enrollment detected: employeeId={}, planId={} not found in benefit_plans",
                                employeeId, planId);
                    }
                    return opt;
                })
                .flatMap(Optional::stream)
                .map(this::toDTO)
                .toList();
    }

    @Override
    public void enrollInBenefitPlan(int employeeId, int planId) {
        employeeDAO.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));

        BenefitPlan plan = benefitPlanDAO.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Benefit plan not found: " + planId));

        // Duplicate enrollment validation (server-side)
        boolean alreadyEnrolled = employeeBenefitDAO.findPlansForEmployee(employeeId).stream()
                .anyMatch(id -> id == plan.getId());

        if (alreadyEnrolled) {
            throw new IllegalArgumentException(
                    "Employee " + employeeId + " is already enrolled in benefit plan " + plan.getId());
        }

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
