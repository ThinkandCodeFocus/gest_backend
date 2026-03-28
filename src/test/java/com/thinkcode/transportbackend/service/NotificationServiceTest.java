package com.thinkcode.transportbackend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.thinkcode.transportbackend.dto.NotificationCreateRequest;
import com.thinkcode.transportbackend.dto.NotificationResponse;
import com.thinkcode.transportbackend.entity.Company;
import com.thinkcode.transportbackend.entity.Notification;
import com.thinkcode.transportbackend.entity.NotificationType;
import com.thinkcode.transportbackend.repository.NotificationRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private CompanyResolver companyResolver;

    @Mock
    private AuthenticatedCompanyProvider authenticatedCompanyProvider;

    @Mock
    private NotificationRealtimeService notificationRealtimeService;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                notificationRepository,
                companyResolver,
                authenticatedCompanyProvider,
                notificationRealtimeService
        );
    }

    @Test
    void createShouldPersistAndBroadcast() {
        UUID companyId = UUID.randomUUID();
        Company company = new Company();
        Notification saved = new Notification();
        saved.setCompany(company);
        saved.setTitle("Alerte");
        saved.setMessage("Nouvelle alerte");
        saved.setType(NotificationType.WARNING);

        when(authenticatedCompanyProvider.requireCompanyId()).thenReturn(companyId);
        when(companyResolver.require(companyId)).thenReturn(company);
        when(notificationRepository.save(org.mockito.ArgumentMatchers.any(Notification.class))).thenReturn(saved);

        NotificationResponse response = notificationService.create(
                new NotificationCreateRequest("Alerte", "Nouvelle alerte", NotificationType.WARNING)
        );

        assertEquals("Alerte", response.title());
        assertEquals(NotificationType.WARNING, response.type());
        verify(notificationRealtimeService).broadcast(companyId, response);
    }

    @Test
    void findAllUnreadShouldFilterReadOnes() {
        UUID companyId = UUID.randomUUID();
        when(authenticatedCompanyProvider.requireCompanyId()).thenReturn(companyId);
        when(notificationRepository.findAllByCompanyIdAndReadOrderByCreatedAtDesc(companyId, false)).thenReturn(List.of());

        List<NotificationResponse> responses = notificationService.findAll(true);

        assertEquals(0, responses.size());
    }
}
