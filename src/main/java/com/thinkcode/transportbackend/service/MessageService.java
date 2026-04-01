package com.thinkcode.transportbackend.service;

import com.thinkcode.transportbackend.entity.Message;
import com.thinkcode.transportbackend.entity.UserAccount;
import com.thinkcode.transportbackend.repository.MessageRepository;
import com.thinkcode.transportbackend.repository.UserAccountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuthenticatedCompanyProvider authenticatedCompanyProvider;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public MessageService(
            MessageRepository messageRepository,
            UserAccountRepository userAccountRepository,
            AuthenticatedCompanyProvider authenticatedCompanyProvider,
            AuthenticatedUserProvider authenticatedUserProvider
    ) {
        this.messageRepository = messageRepository;
        this.userAccountRepository = userAccountRepository;
        this.authenticatedCompanyProvider = authenticatedCompanyProvider;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    /**
     * Get list of contacts (users who have active conversations)
     */
    public List<UserAccount> getContacts() {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        UserAccount user = authenticatedUserProvider.requireUser();
        return userAccountRepository.findAllByCompanyIdAndIdNotOrderByFullNameAsc(companyId, user.getId());
    }

    /**
     * Get conversation with a specific contact (paginated)
     */
    public Page<Message> getConversation(UUID contactId, int page, int pageSize) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        UserAccount user = authenticatedUserProvider.requireUser();
        UUID userId = user.getId();
        
        // Ensure contact exists and belongs to same company
        UserAccount contact = userAccountRepository.findById(contactId).orElse(null);
        if (contact == null || !contact.getCompany().getId().equals(companyId)) {
            throw new IllegalArgumentException("Contact not found or unauthorized");
        }

        return messageRepository.findConversation(
            companyId,
            userId,
            contactId,
            PageRequest.of(page, pageSize)
        );
    }

    /**
     * Send a message to a recipient
     */
    public Message sendMessage(UUID recipientId, String content, String attachmentUrl, String attachmentName) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        UserAccount sender = authenticatedUserProvider.requireUser();
        UUID userId = sender.getId();

        UserAccount recipient = userAccountRepository.findById(recipientId).orElse(null);

        if (recipient == null) {
            throw new IllegalArgumentException("Recipient not found");
        }

        if (!recipient.getCompany().getId().equals(companyId)) {
            throw new IllegalArgumentException("Recipient must belong to same company");
        }

        Message message = new Message();
        message.setSender(sender);
        message.setRecipient(recipient);
        message.setContent(content);
        message.setAttachmentUrl(attachmentUrl);
        message.setAttachmentName(attachmentName);
        message.setCompany(sender.getCompany());
        message.setIsRead(false);

        return messageRepository.save(message);
    }

    /**
     * Mark a message as read
     */
    public Message markAsRead(UUID messageId) {
        UserAccount user = authenticatedUserProvider.requireUser();
        UUID recipientId = user.getId();
        
        Message message = messageRepository.findById(messageId).orElse(null);
        if (message == null) {
            throw new IllegalArgumentException("Message not found");
        }

        if (!message.getRecipient().getId().equals(recipientId)) {
            throw new IllegalArgumentException("Only recipient can mark as read");
        }

        message.setIsRead(true);
        message.setReadAt(LocalDateTime.now());
        return messageRepository.save(message);
    }

    /**
     * Get count of unread messages
     */
    public long getUnreadCount() {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        UserAccount user = authenticatedUserProvider.requireUser();
        return messageRepository.countUnreadMessages(companyId, user.getId());
    }

    public List<Message> getRecentMessages(int limit) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        UserAccount user = authenticatedUserProvider.requireUser();
        return messageRepository.findRecentMessages(companyId, user.getId(), PageRequest.of(0, limit));
    }

    /**
     * Delete a message
     */
    public void deleteMessage(UUID messageId) {
        UserAccount user = authenticatedUserProvider.requireUser();
        UUID userId = user.getId();
        Message message = messageRepository.findById(messageId).orElse(null);
        
        if (message == null) {
            throw new IllegalArgumentException("Message not found");
        }

        // Only sender or recipient can delete
        if (!message.getSender().getId().equals(userId) && !message.getRecipient().getId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized to delete this message");
        }

        messageRepository.delete(message);
    }
}
