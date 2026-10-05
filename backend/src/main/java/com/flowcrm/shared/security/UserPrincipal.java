package com.flowcrm.shared.security;

import java.util.UUID;

public class UserPrincipal {

    private final UUID userId;
    private final UUID companyId;
    private final String email;
    private final String role;

    public UserPrincipal(String userId, String companyId, String email, String role) {
        this.userId = UUID.fromString(userId);
        this.companyId = UUID.fromString(companyId);
        this.email = email;
        this.role = role;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }
}
