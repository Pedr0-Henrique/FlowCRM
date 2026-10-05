package com.flowcrm.auth.dto;

import com.flowcrm.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Nome da empresa é obrigatório")
        @Size(max = 180, message = "Nome da empresa deve ter no máximo 180 caracteres")
        String companyName,

        @NotBlank(message = "Slug da empresa é obrigatório")
        @Size(max = 80, message = "Slug deve ter no máximo 80 caracteres")
        String companySlug,

        @NotBlank(message = "Nome do usuário é obrigatório")
        @Size(max = 160, message = "Nome deve ter no máximo 160 caracteres")
        String name,

        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail inválido")
        @Size(max = 180, message = "E-mail deve ter no máximo 180 caracteres")
        String email,

        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 6, max = 100, message = "Senha deve ter entre 6 e 100 caracteres")
        String password
) {
}
