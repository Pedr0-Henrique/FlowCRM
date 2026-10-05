package com.flowcrm.lead.dto;

import com.flowcrm.lead.LeadPriority;
import com.flowcrm.lead.LeadStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record LeadUpdateRequest(
        @Size(max = 180, message = "Nome deve ter no máximo 180 caracteres")
        String name,

        @Email(message = "E-mail inválido")
        @Size(max = 180, message = "E-mail deve ter no máximo 180 caracteres")
        String email,

        @Size(max = 32, message = "Telefone deve ter no máximo 32 caracteres")
        String phone,

        @Size(max = 50, message = "Origem deve ter no máximo 50 caracteres")
        String source,

        LeadStatus status,

        LeadPriority priority,

        BigDecimal estimatedValue,

        String notes,

        UUID assignedToId
) {
}
