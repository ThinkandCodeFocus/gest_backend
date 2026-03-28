package com.thinkcode.transportbackend.controller;

import com.thinkcode.transportbackend.dto.ChatChannelCreateRequest;
import com.thinkcode.transportbackend.dto.ChatChannelResponse;
import com.thinkcode.transportbackend.dto.ChatMessageCreateRequest;
import com.thinkcode.transportbackend.dto.ChatMessageResponse;
import com.thinkcode.transportbackend.service.ChatService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/channels")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'ASSISTANT', 'DRIVER')")
    public List<ChatChannelResponse> findChannels(@RequestParam(defaultValue = "false") boolean includeArchived) {
        return chatService.findChannels(includeArchived);
    }

    @PostMapping("/channels")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'ASSISTANT')")
    public ChatChannelResponse createChannel(@Valid @RequestBody ChatChannelCreateRequest request) {
        return chatService.createChannel(request);
    }

    @PatchMapping("/channels/{channelId}/archive")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER')")
    public ChatChannelResponse archiveChannel(@PathVariable UUID channelId) {
        return chatService.archiveChannel(channelId);
    }

    @GetMapping("/channels/{channelId}/messages")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'ASSISTANT', 'DRIVER')")
    public List<ChatMessageResponse> findMessages(@PathVariable UUID channelId) {
        return chatService.findMessages(channelId);
    }

    @PostMapping("/channels/{channelId}/messages")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'ASSISTANT', 'DRIVER')")
    public ChatMessageResponse sendMessage(
            @PathVariable UUID channelId,
            @Valid @RequestBody ChatMessageCreateRequest request
    ) {
        return chatService.sendMessage(channelId, request);
    }

    @PostMapping(value = "/channels/{channelId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'ASSISTANT', 'DRIVER')")
    public ChatMessageResponse sendAttachment(
            @PathVariable UUID channelId,
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "content", required = false) String content
    ) {
        return chatService.sendAttachment(channelId, file, content);
    }

    @GetMapping("/messages/{messageId}/attachment")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'ASSISTANT', 'DRIVER')")
    public ResponseEntity<byte[]> downloadAttachment(@PathVariable UUID messageId) {
        ChatService.ChatAttachmentData attachment = chatService.downloadAttachment(messageId);
        String contentType = attachment.contentType() == null || attachment.contentType().isBlank()
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : attachment.contentType();

        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.parseMediaType(contentType))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(attachment.fileName()).build().toString()
                )
                .body(attachment.bytes());
    }

    @DeleteMapping("/messages/{messageId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'ASSISTANT')")
    public void deleteMessage(@PathVariable UUID messageId) {
        chatService.deleteMessage(messageId);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'ASSISTANT', 'DRIVER')")
    public SseEmitter stream() {
        return chatService.subscribe();
    }
}
