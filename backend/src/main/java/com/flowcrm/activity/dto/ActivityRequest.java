package com.flowcrm.activity.dto;

import com.flowcrm.activity.ActivityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record ActivityRequest(
        @NotBlank @Size(max = 180) String title,
        String description,
        @NotNull ActivityType type,
        Instant occurredAt,
        UUID clientId,
        UUID leadId
) {
}
