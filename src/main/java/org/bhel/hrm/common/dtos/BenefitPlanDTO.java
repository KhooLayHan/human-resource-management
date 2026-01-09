package org.bhel.hrm.common.dtos;

import java.io.Serializable;
import java.math.BigDecimal;

public record BenefitPlanDTO(
    int id,
    String planName,
    String provider,
    String description,
    BigDecimal costPerMonth
) implements Serializable {
}




