package com.thinkcode.transportbackend.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import com.thinkcode.transportbackend.entity.RoleName;

public record MessageResponse(
    UUID id,
    String senderName,
    UUID senderId,
    RoleName senderRole,
    String recipientName,
    UUID recipientId,
    RoleName recipientRole,
    String content,
    Boolean isRead,
    String attachmentUrl,
    String attachmentName,
    LocalDateTime createdAt,
    LocalDateTime readAt
) {}
