package com.flowcrm.user.dto;

import com.flowcrm.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @Size(max = 160, message = "Nome deve ter no máximo 160 caracteres")
        String name,

        @Email(message = "E-mail inválido")
        @Size(max = 180, message = "E-mail deve ter no máximo 180 caracteres")
        String email,

        @Size(min = 6, max = 100, message = "Senha deve ter entre 6 e 100 caracteres")
        String password,

        Role role,

        Boolean active
) {
}
