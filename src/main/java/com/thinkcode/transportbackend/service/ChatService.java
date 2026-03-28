package com.thinkcode.transportbackend.service;

import com.thinkcode.transportbackend.dto.ChatChannelCreateRequest;
import com.thinkcode.transportbackend.dto.ChatChannelResponse;
import com.thinkcode.transportbackend.dto.ChatMessageCreateRequest;
import com.thinkcode.transportbackend.dto.ChatMessageResponse;
import com.thinkcode.transportbackend.entity.ChatChannel;
import com.thinkcode.transportbackend.entity.ChatMessage;
import com.thinkcode.transportbackend.entity.UserAccount;
import com.thinkcode.transportbackend.repository.ChatChannelRepository;
import com.thinkcode.transportbackend.repository.ChatMessageRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class ChatService {

    private final ChatChannelRepository chatChannelRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final CompanyResolver companyResolver;
    private final AuthenticatedCompanyProvider authenticatedCompanyProvider;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final ChatRealtimeService chatRealtimeService;
    private final Path chatStoragePath;

    public ChatService(
            ChatChannelRepository chatChannelRepository,
            ChatMessageRepository chatMessageRepository,
            CompanyResolver companyResolver,
            AuthenticatedCompanyProvider authenticatedCompanyProvider,
            AuthenticatedUserProvider authenticatedUserProvider,
            ChatRealtimeService chatRealtimeService,
            @Value("${app.storage.chat-dir:uploads/chat}") String chatStorageDir
    ) {
        this.chatChannelRepository = chatChannelRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.companyResolver = companyResolver;
        this.authenticatedCompanyProvider = authenticatedCompanyProvider;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.chatRealtimeService = chatRealtimeService;
        this.chatStoragePath = Paths.get(chatStorageDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.chatStoragePath);
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to initialize chat storage");
        }
    }

    public List<ChatChannelResponse> findChannels(boolean includeArchived) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        if (includeArchived) {
            return chatChannelRepository.findAllByCompanyIdOrderByCreatedAtAsc(companyId)
                    .stream()
                    .map(this::toChannelResponse)
                    .toList();
        }
        return chatChannelRepository.findAllByCompanyIdAndArchivedOrderByCreatedAtAsc(companyId, false)
                .stream()
                .map(this::toChannelResponse)
                .toList();
    }

    public ChatChannelResponse createChannel(ChatChannelCreateRequest request) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        ChatChannel channel = new ChatChannel();
        channel.setCompany(companyResolver.require(companyId));
        channel.setName(request.name());
        channel.setModule(request.module());
        return toChannelResponse(chatChannelRepository.save(channel));
    }

    public ChatChannelResponse archiveChannel(UUID channelId) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        ChatChannel channel = chatChannelRepository.findByIdAndCompanyId(channelId, companyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Channel not found"));
        channel.setArchived(true);
        return toChannelResponse(chatChannelRepository.save(channel));
    }

    public List<ChatMessageResponse> findMessages(UUID channelId) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        ensureChannelOwnership(channelId, companyId);
        return chatMessageRepository.findAllByChannelIdAndChannelCompanyIdOrderByCreatedAtAsc(channelId, companyId)
                .stream()
                .map(this::toMessageResponse)
                .toList();
    }

    public ChatMessageResponse sendMessage(UUID channelId, ChatMessageCreateRequest request) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        ChatChannel channel = ensureChannelOwnership(channelId, companyId);
        if (channel.isArchived()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Channel is archived");
        }

        UserAccount author = authenticatedUserProvider.requireUser();
        ChatMessage message = new ChatMessage();
        message.setChannel(channel);
        message.setAuthor(author);
        message.setContent(request.content());

        ChatMessage saved = chatMessageRepository.save(message);
        ChatMessageResponse response = toMessageResponse(saved);
        chatRealtimeService.broadcast(companyId, response);
        return response;
    }

    public ChatMessageResponse sendAttachment(UUID channelId, MultipartFile file, String content) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        ChatChannel channel = ensureChannelOwnership(channelId, companyId);
        if (channel.isArchived()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Channel is archived");
        }

        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Attachment file is required");
        }

        if (file.getSize() > 10L * 1024L * 1024L) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Attachment exceeds maximum size of 10MB");
        }

        String originalName = file.getOriginalFilename() == null ? "attachment.bin" : Paths.get(file.getOriginalFilename()).getFileName().toString();
        String storedName = UUID.randomUUID() + "_" + originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
        Path destination = chatStoragePath.resolve(storedName).normalize();

        if (!destination.startsWith(chatStoragePath)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid attachment path");
        }

        try {
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to store attachment");
        }

        UserAccount author = authenticatedUserProvider.requireUser();
        ChatMessage message = new ChatMessage();
        message.setChannel(channel);
        message.setAuthor(author);
        message.setContent(content == null || content.isBlank() ? "[Attachment] " + originalName : content);
        message.setAttachmentOriginalName(originalName);
        message.setAttachmentStoredName(storedName);
        message.setAttachmentContentType(file.getContentType());
        message.setAttachmentSize(file.getSize());

        ChatMessage saved = chatMessageRepository.save(message);
        ChatMessageResponse response = toMessageResponse(saved);
        chatRealtimeService.broadcast(companyId, response);
        return response;
    }

    public ChatAttachmentData downloadAttachment(UUID messageId) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        ChatMessage message = chatMessageRepository.findByIdAndChannelCompanyId(messageId, companyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Message not found"));

        if (message.getAttachmentStoredName() == null || message.getAttachmentStoredName().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No attachment associated with this message");
        }

        Path attachmentPath = chatStoragePath.resolve(message.getAttachmentStoredName()).normalize();
        if (!attachmentPath.startsWith(chatStoragePath)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid attachment path");
        }

        try {
            byte[] bytes = Files.readAllBytes(attachmentPath);
            return new ChatAttachmentData(
                    message.getAttachmentOriginalName(),
                    message.getAttachmentContentType(),
                    bytes
            );
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Attachment file not found");
        }
    }

    public void deleteMessage(UUID messageId) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        ChatMessage message = chatMessageRepository.findByIdAndChannelCompanyId(messageId, companyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Message not found"));

        deleteAttachmentIfExists(message.getAttachmentStoredName());
        chatMessageRepository.delete(message);
    }

    public SseEmitter subscribe() {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        return chatRealtimeService.subscribe(companyId);
    }

    private ChatChannel ensureChannelOwnership(UUID channelId, UUID companyId) {
        return chatChannelRepository.findByIdAndCompanyId(channelId, companyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Channel not found"));
    }

    private ChatChannelResponse toChannelResponse(ChatChannel channel) {
        return new ChatChannelResponse(
                channel.getId(),
                channel.getName(),
                channel.getModule(),
                channel.isArchived(),
                channel.getCreatedAt()
        );
    }

    private ChatMessageResponse toMessageResponse(ChatMessage message) {
        String attachmentUrl = message.getAttachmentStoredName() == null
                ? null
                : "/api/chat/messages/" + message.getId() + "/attachment";

        return new ChatMessageResponse(
                message.getId(),
                message.getChannel().getId(),
                message.getAuthor().getId(),
                message.getAuthor().getFullName(),
                message.getContent(),
                message.getCreatedAt(),
                message.getAttachmentOriginalName(),
                message.getAttachmentContentType(),
                message.getAttachmentSize(),
                attachmentUrl
        );
    }

    private void deleteAttachmentIfExists(String storedName) {
        if (storedName == null || storedName.isBlank()) {
            return;
        }

        Path attachmentPath = chatStoragePath.resolve(storedName).normalize();
        if (!attachmentPath.startsWith(chatStoragePath)) {
            return;
        }

        try {
            Files.deleteIfExists(attachmentPath);
        } catch (IOException ignored) {
            // Best-effort cleanup, message deletion should still proceed.
        }
    }

    public record ChatAttachmentData(String fileName, String contentType, byte[] bytes) {
    }
}
