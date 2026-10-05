package com.flowcrm.dashboard.dto;

import java.util.List;

public record DashboardResponse(
        DashboardKPIs kpis,
        List<ChartSeries> leadsByStatus,
        List<ChartSeries> leadsBySource,
        List<ChartSeries> opportunitiesByStage,
        List<ChartSeries> tasksByStatus,
        List<ChartSeries> monthlyRevenue
) {
}
