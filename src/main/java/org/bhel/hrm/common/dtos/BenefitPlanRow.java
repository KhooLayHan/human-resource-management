package org.bhel.hrm.common.dtos;

import java.math.BigDecimal;

public record BenefitPlanRow(BenefitPlanDTO plan, String status) {
    public int getId() { return plan.id(); }
    public String getPlanName() { return plan.planName(); }
    public String getProvider() { return plan.provider(); }
    public BigDecimal getCostPerMonth() { return plan.costPerMonth(); }
    public String getDescription() { return plan.description(); }
    public String getStatus() { return status; }
}
