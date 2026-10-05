package com.flowcrm.dashboard.dto;

import java.math.BigDecimal;

public record DashboardKPIs(
        long totalLeads,
        long totalClients,
        long totalOpportunities,
        long totalTasks,
        long leadsWon,
        long leadsLost,
        BigDecimal totalOpportunityValue,
        BigDecimal weightedOpportunityValue,
        long openTasks,
        long overdueTasks,
        double conversionRate
) {
}
