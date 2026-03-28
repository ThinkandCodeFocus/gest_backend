package com.thinkcode.transportbackend.dto;

import java.time.Instant;
import java.util.UUID;

public record ChatMessageResponse(
        UUID id,
        UUID channelId,
        UUID authorId,
        String authorName,
        String content,
        Instant createdAt,
        String attachmentName,
        String attachmentContentType,
        Long attachmentSize,
        String attachmentDownloadUrl
) {
}
