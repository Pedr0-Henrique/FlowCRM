package com.flowcrm.company.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanyRequest(
        @NotBlank(message = "Nome da empresa é obrigatório")
        @Size(max = 180, message = "Nome deve ter no máximo 180 caracteres")
        String name,

        @NotBlank(message = "Slug é obrigatório")
        @Size(max = 80, message = "Slug deve ter no máximo 80 caracteres")
        String slug
) {
}
