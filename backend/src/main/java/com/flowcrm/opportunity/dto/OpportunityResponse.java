package com.flowcrm.opportunity.dto;

import com.flowcrm.opportunity.OpportunityStage;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record OpportunityResponse(
        UUID id,
        String title,
        String description,
        BigDecimal value,
        OpportunityStage stage,
        Integer probability,
        LocalDate expectedCloseDate,
        LocalDate actualCloseDate,
        String notes,
        UUID clientId,
        String clientName,
        UUID leadId,
        String leadName,
        UUID assignedToId,
        String assignedToName,
        UUID companyId,
        Instant createdAt,
        Instant updatedAt
) {
}
