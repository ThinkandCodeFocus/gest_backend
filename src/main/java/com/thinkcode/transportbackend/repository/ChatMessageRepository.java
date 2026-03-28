package com.thinkcode.transportbackend.repository;

import com.thinkcode.transportbackend.entity.ChatMessage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findAllByChannelIdAndChannelCompanyIdOrderByCreatedAtAsc(UUID channelId, UUID companyId);

    Optional<ChatMessage> findByIdAndChannelCompanyId(UUID messageId, UUID companyId);
}
