package com.flowcrm.client.dto;

import com.flowcrm.client.ClientStatus;

import java.time.Instant;
import java.util.UUID;

public record ClientResponse(
        UUID id,
        String name,
        String email,
        String phone,
        String address,
        String city,
        String state,
        String zipCode,
        String country,
        ClientStatus status,
        String notes,
        UUID companyId,
        Instant createdAt,
        Instant updatedAt
) {
}
