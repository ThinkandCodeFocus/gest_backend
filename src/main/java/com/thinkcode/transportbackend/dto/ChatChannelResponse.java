package com.thinkcode.transportbackend.dto;

import com.thinkcode.transportbackend.entity.ChatModule;
import java.time.Instant;
import java.util.UUID;

public record ChatChannelResponse(
        UUID id,
        String name,
        ChatModule module,
        boolean archived,
        Instant createdAt
) {
}
