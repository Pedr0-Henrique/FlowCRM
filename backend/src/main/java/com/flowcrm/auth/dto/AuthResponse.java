package com.flowcrm.auth.dto;

import com.flowcrm.user.Role;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        UUID userId,
        String name,
        String email,
        Role role,
        UUID companyId,
        String companyName
) {
}
