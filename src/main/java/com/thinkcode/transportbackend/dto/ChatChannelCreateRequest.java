package com.thinkcode.transportbackend.dto;

import com.thinkcode.transportbackend.entity.ChatModule;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ChatChannelCreateRequest(
        @NotBlank String name,
        @NotNull ChatModule module
) {
}
