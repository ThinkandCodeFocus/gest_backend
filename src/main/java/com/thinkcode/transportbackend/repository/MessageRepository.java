package com.thinkcode.transportbackend.repository;

import com.thinkcode.transportbackend.entity.Message;
import com.thinkcode.transportbackend.entity.UserAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    // Find messages between two users
    @Query("SELECT m FROM Message m WHERE m.company.id = :companyId " +
           "AND ((m.sender.id = :userId1 AND m.recipient.id = :userId2) " +
           "OR (m.sender.id = :userId2 AND m.recipient.id = :userId1)) " +
           "ORDER BY m.createdAt DESC")
    Page<Message> findConversation(
        @Param("companyId") UUID companyId,
        @Param("userId1") UUID userId1,
        @Param("userId2") UUID userId2,
        Pageable pageable
    );

    // Find all contacts (distinct users who sent/received messages)
    @Query("SELECT DISTINCT u FROM UserAccount u WHERE u.company.id = :companyId " +
           "AND u.id IN (SELECT DISTINCT CASE WHEN m.sender.id = :userId THEN m.recipient.id ELSE m.sender.id END " +
           "FROM Message m WHERE m.company.id = :companyId AND (m.sender.id = :userId OR m.recipient.id = :userId))")
    List<UserAccount> findContacts(
        @Param("companyId") UUID companyId,
        @Param("userId") UUID userId
    );

    // Count unread messages for a user
    @Query("SELECT COUNT(m) FROM Message m WHERE m.company.id = :companyId " +
           "AND m.recipient.id = :userId AND m.isRead = false")
    long countUnreadMessages(
        @Param("companyId") UUID companyId,
        @Param("userId") UUID userId
    );

    @Query("SELECT m FROM Message m WHERE m.company.id = :companyId " +
           "AND (m.sender.id = :userId OR m.recipient.id = :userId) " +
           "ORDER BY m.createdAt DESC")
    List<Message> findRecentMessages(
        @Param("companyId") UUID companyId,
        @Param("userId") UUID userId,
        Pageable pageable
    );

    // Find unread messages from specific sender
    List<Message> findByCompanyIdAndRecipientIdAndSenderIdAndIsReadFalse(
        UUID companyId,
        UUID recipientId,
        UUID senderId
    );

    // Mark messages as read
    void deleteByIdAndRecipientId(UUID messageId, UUID recipientId);
}
