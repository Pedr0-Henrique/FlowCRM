package com.flowcrm.lead.dto;

import com.flowcrm.lead.LeadPriority;
import com.flowcrm.lead.LeadStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LeadResponse(
        UUID id,
        String name,
        String email,
        String phone,
        String source,
        LeadStatus status,
        LeadPriority priority,
        BigDecimal estimatedValue,
        String notes,
        UUID assignedToId,
        String assignedToName,
        UUID companyId,
        Instant createdAt,
        Instant updatedAt
) {
}
