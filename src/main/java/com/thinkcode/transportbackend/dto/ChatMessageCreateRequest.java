package com.thinkcode.transportbackend.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatMessageCreateRequest(@NotBlank String content) {
}
