package com.thinkcode.transportbackend.repository;

import com.thinkcode.transportbackend.entity.ChatChannel;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatChannelRepository extends JpaRepository<ChatChannel, UUID> {

    List<ChatChannel> findAllByCompanyIdOrderByCreatedAtAsc(UUID companyId);

    List<ChatChannel> findAllByCompanyIdAndArchivedOrderByCreatedAtAsc(UUID companyId, boolean archived);

    Optional<ChatChannel> findByIdAndCompanyId(UUID channelId, UUID companyId);
}
