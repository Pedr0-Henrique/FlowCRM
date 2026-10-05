package com.flowcrm.client.dto;

import com.flowcrm.client.ClientStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClientRequest(
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 180, message = "Nome deve ter no máximo 180 caracteres")
        String name,

        @Email(message = "E-mail inválido")
        @Size(max = 180, message = "E-mail deve ter no máximo 180 caracteres")
        String email,

        @Size(max = 32, message = "Telefone deve ter no máximo 32 caracteres")
        String phone,

        String address,

        @Size(max = 100, message = "Cidade deve ter no máximo 100 caracteres")
        String city,

        @Size(max = 50, message = "Estado deve ter no máximo 50 caracteres")
        String state,

        @Size(max = 20, message = "CEP deve ter no máximo 20 caracteres")
        String zipCode,

        @Size(max = 100, message = "País deve ter no máximo 100 caracteres")
        String country,

        ClientStatus status,

        String notes
) {
}
