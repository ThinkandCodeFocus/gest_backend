package com.thinkcode.transportbackend.controller;

import com.thinkcode.transportbackend.dto.MessageRequest;
import com.thinkcode.transportbackend.dto.MessageResponse;
import com.thinkcode.transportbackend.entity.Message;
import com.thinkcode.transportbackend.entity.UserAccount;
import com.thinkcode.transportbackend.service.MessageService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/contacts")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'ASSISTANT', 'DRIVER', 'CLIENT')")
    public List<Object> getContacts() {
        List<UserAccount> contacts = messageService.getContacts();
        return contacts.stream()
                .map(c -> Map.of(
                        "id", (Object) c.getId(),
                        "name", (Object) c.getFullName(),
                        "email", (Object) c.getEmail(),
                        "role", (Object) c.getRole().name()
                ))
                .collect(Collectors.toList());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'ASSISTANT', 'DRIVER', 'CLIENT')")
    public Page<MessageResponse> getConversation(
            @RequestParam UUID contactId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        Page<Message> messages = messageService.getConversation(contactId, page, pageSize);
        return messages.map(this::mapToResponse);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'ASSISTANT', 'DRIVER', 'CLIENT')")
    public MessageResponse sendMessage(@Valid @RequestBody MessageRequest request) {
        Message message = messageService.sendMessage(
                request.recipientId(),
                request.content(),
                request.attachmentUrl(),
                request.attachmentName()
        );
        return mapToResponse(message);
    }

    @PatchMapping("/{messageId}/read")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'ASSISTANT', 'DRIVER', 'CLIENT')")
    public MessageResponse markAsRead(@PathVariable UUID messageId) {
        Message message = messageService.markAsRead(messageId);
        return mapToResponse(message);
    }

    @GetMapping("/unread-count")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'ASSISTANT', 'DRIVER', 'CLIENT')")
    public Map<String, Long> getUnreadCount() {
        return Map.of("unreadCount", messageService.getUnreadCount());
    }

    @GetMapping("/recent")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'ASSISTANT', 'DRIVER', 'CLIENT')")
    public List<MessageResponse> getRecent(@RequestParam(defaultValue = "10") int limit) {
        return messageService.getRecentMessages(limit).stream().map(this::mapToResponse).toList();
    }

    @DeleteMapping("/{messageId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'ASSISTANT', 'DRIVER', 'CLIENT')")
    public void deleteMessage(@PathVariable UUID messageId) {
        messageService.deleteMessage(messageId);
    }

    private MessageResponse mapToResponse(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getSender().getFullName(),
                message.getSender().getId(),
                message.getSender().getRole(),
                message.getRecipient().getFullName(),
                message.getRecipient().getId(),
                message.getRecipient().getRole(),
                message.getContent(),
                message.getIsRead(),
                message.getAttachmentUrl(),
                message.getAttachmentName(),
                message.getCreatedAt(),
                message.getReadAt()
        );
    }
}
