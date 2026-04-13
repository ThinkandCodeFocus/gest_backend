package com.thinkcode.transportbackend.dto;

public record ResetUserPasswordResponse(
        String fullName,
        String email,
        String generatedPassword
) {
}
