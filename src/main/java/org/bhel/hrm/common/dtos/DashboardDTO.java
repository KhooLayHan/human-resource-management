package org.bhel.hrm.common.dtos;

import java.io.Serializable;
import java.util.Map;

public record DashboardDTO(
    // Common
    String welcomeMessage,

    // Employee-specific
    int annualLeaveBalance,
    int medicalLeaveBalance,
    int upcomingTrainingsCount,

    // HR-specific
    int totalEmployees,
    int pendingLeaveRequests,
    int openJobPositions,

    // Charts
    Map<String, Integer> departmentDistribution,
    Map<String, Integer> leaveStatusBreakdown,
    Map<String, Integer> recruitmentPipelineData,
    Map<String, Integer> trainingEnrollmentTrend
) implements Serializable {}
