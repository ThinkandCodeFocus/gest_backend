package com.thinkcode.transportbackend.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record MessageRequest(
    UUID recipientId,
    String content,
    String attachmentUrl,
    String attachmentName
) {}
