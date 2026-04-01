package com.thinkcode.transportbackend.dto;

import java.util.UUID;

public record ClientResponse(
        UUID id,
        String name,
        String email,
        String phoneNumber
) {
}
