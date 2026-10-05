package com.flowcrm.auth.dto;

import com.flowcrm.user.Role;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email,
        Role role,
        Boolean active,
        UUID companyId,
        String companyName,
        Instant createdAt
) {
}
