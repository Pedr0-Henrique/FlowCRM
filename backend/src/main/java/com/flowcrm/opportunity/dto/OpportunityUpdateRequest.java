package com.flowcrm.opportunity.dto;

import com.flowcrm.opportunity.OpportunityStage;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record OpportunityUpdateRequest(
        @Size(max = 200, message = "Título deve ter no máximo 200 caracteres")
        String title,

        String description,

        @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
        BigDecimal value,

        OpportunityStage stage,

        @Min(value = 0, message = "Probabilidade deve ser entre 0 e 100")
        @Max(value = 100, message = "Probabilidade deve ser entre 0 e 100")
        Integer probability,

        LocalDate expectedCloseDate,

        LocalDate actualCloseDate,

        String notes,

        UUID clientId,

        UUID leadId,

        UUID assignedToId
) {
}
