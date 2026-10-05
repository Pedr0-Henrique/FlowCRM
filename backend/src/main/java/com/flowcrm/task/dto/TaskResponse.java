package com.flowcrm.task.dto;

import com.flowcrm.task.TaskPriority;
import com.flowcrm.task.TaskStatus;

import java.time.Instant;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        Instant dueDate,
        Instant completedAt,
        UUID assignedToId,
        String assignedToName,
        UUID clientId,
        String clientName,
        UUID leadId,
        String leadName,
        UUID companyId,
        Instant createdAt,
        Instant updatedAt
) {
}
