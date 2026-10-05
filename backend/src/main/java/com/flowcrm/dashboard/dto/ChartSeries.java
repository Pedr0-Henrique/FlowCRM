package com.flowcrm.dashboard.dto;

import java.util.List;

public record ChartSeries(
        String name,
        List<ChartDataPoint> data
) {
}
