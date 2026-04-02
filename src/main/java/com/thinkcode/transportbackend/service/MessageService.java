package com.thinkcode.transportbackend.service;

import com.thinkcode.transportbackend.entity.Message;
import com.thinkcode.transportbackend.entity.RoleName;
import com.thinkcode.transportbackend.entity.UserAccount;
import com.thinkcode.transportbackend.repository.MessageRepository;
import com.thinkcode.transportbackend.repository.UserAccountRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

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

    public List<UserAccount> getContacts() {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        UserAccount user = authenticatedUserProvider.requireUser();
        return userAccountRepository.findAllByCompanyIdAndIdNotOrderByFullNameAsc(companyId, user.getId()).stream()
                .filter(contact -> canInteract(user, contact))
                .toList();
    }

    public Page<Message> getConversation(UUID contactId, int page, int pageSize) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        UserAccount user = authenticatedUserProvider.requireUser();
        UUID userId = user.getId();

        UserAccount contact = userAccountRepository.findById(contactId).orElse(null);
        if (contact == null || !contact.getCompany().getId().equals(companyId)) {
            throw new IllegalArgumentException("Contact not found or unauthorized");
        }
        if (!canInteract(user, contact)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Unauthorized contact");
        }

        return messageRepository.findConversation(
            companyId,
            userId,
            contactId,
            PageRequest.of(page, pageSize)
        );
    }

    public Message sendMessage(UUID recipientId, String content, String attachmentUrl, String attachmentName) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        UserAccount sender = authenticatedUserProvider.requireUser();

        UserAccount recipient = userAccountRepository.findById(recipientId).orElse(null);

        if (recipient == null) {
            throw new IllegalArgumentException("Recipient not found");
        }

        if (!recipient.getCompany().getId().equals(companyId)) {
            throw new IllegalArgumentException("Recipient must belong to same company");
        }
        if (!canInteract(sender, recipient)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Unauthorized recipient");
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

    public void deleteMessage(UUID messageId) {
        UserAccount user = authenticatedUserProvider.requireUser();
        UUID userId = user.getId();
        Message message = messageRepository.findById(messageId).orElse(null);

        if (message == null) {
            throw new IllegalArgumentException("Message not found");
        }

        if (!message.getSender().getId().equals(userId) && !message.getRecipient().getId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized to delete this message");
        }

        messageRepository.delete(message);
    }

    private boolean canInteract(UserAccount currentUser, UserAccount contact) {
        if (currentUser.getRole() != RoleName.CLIENT) {
            return true;
        }
        return contact.getRole() != RoleName.CLIENT;
    }
}
