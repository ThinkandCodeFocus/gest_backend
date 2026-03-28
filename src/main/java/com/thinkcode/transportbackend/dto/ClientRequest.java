package com.thinkcode.transportbackend.dto;

import jakarta.validation.constraints.NotBlank;

public record ClientRequest(
        @NotBlank String name,
        String email,
        String phoneNumber
) {
}

