package com.thinkcode.transportbackend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.thinkcode.transportbackend.dto.ChatMessageCreateRequest;
import com.thinkcode.transportbackend.dto.ChatMessageResponse;
import com.thinkcode.transportbackend.entity.ChatChannel;
import com.thinkcode.transportbackend.entity.ChatMessage;
import com.thinkcode.transportbackend.entity.ChatModule;
import com.thinkcode.transportbackend.entity.Company;
import com.thinkcode.transportbackend.entity.UserAccount;
import java.nio.file.Files;
import java.nio.file.Path;
import com.thinkcode.transportbackend.repository.ChatChannelRepository;
import com.thinkcode.transportbackend.repository.ChatMessageRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatChannelRepository chatChannelRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private CompanyResolver companyResolver;

    @Mock
    private AuthenticatedCompanyProvider authenticatedCompanyProvider;

    @Mock
    private AuthenticatedUserProvider authenticatedUserProvider;

    @Mock
    private ChatRealtimeService chatRealtimeService;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(
                chatChannelRepository,
                chatMessageRepository,
                companyResolver,
                authenticatedCompanyProvider,
                authenticatedUserProvider,
            chatRealtimeService,
            "target/test-chat-files"
        );
    }

    @Test
    void sendMessageShouldBroadcastRealtimeEvent() {
        UUID companyId = UUID.randomUUID();
        UUID channelId = UUID.randomUUID();

        Company company = new Company();
        ChatChannel channel = new ChatChannel();
        channel.setCompany(company);
        channel.setModule(ChatModule.GENERAL);
        channel.setName("General");

        UserAccount author = new UserAccount();
        author.setFullName("Test User");

        when(authenticatedCompanyProvider.requireCompanyId()).thenReturn(companyId);
        when(chatChannelRepository.findByIdAndCompanyId(channelId, companyId)).thenReturn(Optional.of(channel));
        when(authenticatedUserProvider.requireUser()).thenReturn(author);
        when(chatMessageRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));

        ChatMessageResponse response = chatService.sendMessage(channelId, new ChatMessageCreateRequest("Bonjour"));

        assertEquals("Bonjour", response.content());
        assertEquals("Test User", response.authorName());
        assertEquals(null, response.attachmentName());
        verify(chatRealtimeService).broadcast(companyId, response);
    }

    @Test
    void deleteMessageShouldRemoveAttachmentFile() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        String storedName = "delete-me.txt";

        Path storageDir = Path.of("target/test-chat-files").toAbsolutePath().normalize();
        Files.createDirectories(storageDir);
        Path attachmentPath = storageDir.resolve(storedName);
        Files.writeString(attachmentPath, "temporary file");

        ChatMessage message = new ChatMessage();
        message.setAttachmentStoredName(storedName);

        when(authenticatedCompanyProvider.requireCompanyId()).thenReturn(companyId);
        when(chatMessageRepository.findByIdAndChannelCompanyId(messageId, companyId)).thenReturn(Optional.of(message));

        chatService.deleteMessage(messageId);

        verify(chatMessageRepository).delete(message);
        assertTrue(!Files.exists(attachmentPath));
    }
}
