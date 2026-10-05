package com.flowcrm.task.dto;

import com.flowcrm.task.TaskPriority;
import com.flowcrm.task.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record TaskRequest(
        @NotBlank(message = "Título é obrigatório")
        @Size(max = 200, message = "Título deve ter no máximo 200 caracteres")
        String title,

        String description,

        TaskStatus status,

        TaskPriority priority,

        Instant dueDate,

        UUID assignedToId,

        UUID clientId,

        UUID leadId
) {
}
