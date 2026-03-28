package com.thinkcode.transportbackend.service;

import com.thinkcode.transportbackend.dto.NotificationCreateRequest;
import com.thinkcode.transportbackend.dto.NotificationResponse;
import com.thinkcode.transportbackend.entity.Notification;
import com.thinkcode.transportbackend.entity.NotificationType;
import com.thinkcode.transportbackend.repository.NotificationRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final CompanyResolver companyResolver;
    private final AuthenticatedCompanyProvider authenticatedCompanyProvider;
    private final NotificationRealtimeService notificationRealtimeService;

    public NotificationService(
            NotificationRepository notificationRepository,
            CompanyResolver companyResolver,
            AuthenticatedCompanyProvider authenticatedCompanyProvider,
            NotificationRealtimeService notificationRealtimeService
    ) {
        this.notificationRepository = notificationRepository;
        this.companyResolver = companyResolver;
        this.authenticatedCompanyProvider = authenticatedCompanyProvider;
        this.notificationRealtimeService = notificationRealtimeService;
    }

    public List<NotificationResponse> findAll(boolean unreadOnly) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        if (unreadOnly) {
            return notificationRepository.findAllByCompanyIdAndReadOrderByCreatedAtDesc(companyId, false)
                    .stream()
                    .map(this::toResponse)
                    .toList();
        }

        return notificationRepository.findAllByCompanyIdOrderByCreatedAtDesc(companyId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public NotificationResponse create(NotificationCreateRequest request) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        return createForCompany(companyId, request.title(), request.message(), request.type());
    }

    public NotificationResponse createForCompany(UUID companyId, String title, String message, NotificationType type) {
        Notification notification = new Notification();
        notification.setCompany(companyResolver.require(companyId));
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type == null ? NotificationType.INFO : type);

        Notification saved = notificationRepository.save(notification);
        NotificationResponse response = toResponse(saved);
        notificationRealtimeService.broadcast(companyId, response);
        return response;
    }

    public NotificationResponse markAsRead(UUID notificationId) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        Notification notification = notificationRepository.findByIdAndCompanyId(notificationId, companyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Notification not found"));
        notification.setRead(true);
        return toResponse(notification);
    }

    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter subscribe() {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        return notificationRealtimeService.subscribe(companyId);
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getType(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
