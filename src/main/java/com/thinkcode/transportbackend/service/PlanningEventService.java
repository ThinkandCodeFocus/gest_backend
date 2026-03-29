package com.thinkcode.transportbackend.service;

import com.thinkcode.transportbackend.dto.PlanningEventRequest;
import com.thinkcode.transportbackend.entity.Company;
import com.thinkcode.transportbackend.entity.PlanningEvent;
import com.thinkcode.transportbackend.entity.UserAccount;
import com.thinkcode.transportbackend.repository.PlanningEventRepository;
import com.thinkcode.transportbackend.repository.UserAccountRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class PlanningEventService {

    private final PlanningEventRepository planningEventRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final UserAccountRepository userAccountRepository;

    public PlanningEventService(PlanningEventRepository planningEventRepository, 
                              AuthenticatedUserProvider authenticatedUserProvider,
                              UserAccountRepository userAccountRepository) {
        this.planningEventRepository = planningEventRepository;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.userAccountRepository = userAccountRepository;
    }

    public List<PlanningEvent> getEventsByDateRange(LocalDate startDate, LocalDate endDate) {
        Company company = authenticatedUserProvider.requireCompany();
        return planningEventRepository.findByCompanyAndDateRange(company.getId(), startDate, endDate);
    }

    public List<PlanningEvent> getEventsByType(String type, LocalDate startDate, LocalDate endDate) {
        Company company = authenticatedUserProvider.requireCompany();
        return planningEventRepository.findByCompanyAndType(company.getId(), type, startDate, endDate);
    }

    public PlanningEvent createEvent(PlanningEventRequest request) {
        Company company = authenticatedUserProvider.requireCompany();
        UserAccount user = authenticatedUserProvider.requireUser();

        PlanningEvent event = new PlanningEvent();
        event.setCompany(company);
        event.setEventDate(request.getEventDate());
        event.setSlot(request.getSlot());
        event.setType(request.getType());
        event.setTitle(request.getTitle());
        
        if (request.getOwnerId() != null) {
            UserAccount owner = userAccountRepository.findById(request.getOwnerId())
                    .orElse(user);
            event.setOwner(owner);
        } else {
            event.setOwner(user);
        }
        
        event.setPriority(request.getPriority());

        return planningEventRepository.save(event);
    }

    public PlanningEvent updateEvent(UUID eventId, PlanningEventRequest request) {
        Company company = authenticatedUserProvider.requireCompany();
        PlanningEvent event = planningEventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Planning event not found"));

        if (!event.getCompany().getId().equals(company.getId())) {
            throw new IllegalArgumentException("Unauthorized");
        }

        event.setEventDate(request.getEventDate());
        event.setSlot(request.getSlot());
        event.setType(request.getType());
        event.setTitle(request.getTitle());
        event.setPriority(request.getPriority());

        return planningEventRepository.save(event);
    }

    public void deleteEvent(UUID eventId) {
        Company company = authenticatedUserProvider.requireCompany();
        PlanningEvent event = planningEventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Planning event not found"));

        if (!event.getCompany().getId().equals(company.getId())) {
            throw new IllegalArgumentException("Unauthorized");
        }

        planningEventRepository.deleteById(eventId);
    }
}
