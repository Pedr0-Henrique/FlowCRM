package com.flowcrm.activity.dto;

import com.flowcrm.activity.ActivityType;

import java.time.Instant;
import java.util.UUID;

public record ActivityResponse(
        UUID id,
        String title,
        String description,
        ActivityType type,
        Instant occurredAt,
        UUID userId,
        String userName,
        UUID clientId,
        String clientName,
        UUID leadId,
        String leadName
) {
}
