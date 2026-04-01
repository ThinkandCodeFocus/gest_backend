package com.thinkcode.transportbackend.repository;

import com.thinkcode.transportbackend.entity.Notification;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findAllByCompanyIdOrderByCreatedAtDesc(UUID companyId);

    List<Notification> findAllByCompanyIdAndReadOrderByCreatedAtDesc(UUID companyId, boolean read);

    Optional<Notification> findByIdAndCompanyId(UUID notificationId, UUID companyId);

    long countByCompanyIdAndRead(UUID companyId, boolean read);
}
