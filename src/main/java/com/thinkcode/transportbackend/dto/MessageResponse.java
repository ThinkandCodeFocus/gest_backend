package com.thinkcode.transportbackend.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record MessageResponse(
    UUID id,
    String senderName,
    UUID senderId,
    String recipientName,
    UUID recipientId,
    String content,
    Boolean isRead,
    String attachmentUrl,
    String attachmentName,
    LocalDateTime createdAt,
    LocalDateTime readAt
) {}
