package com.thinkcode.transportbackend.controller;

import com.thinkcode.transportbackend.entity.Client;
import com.thinkcode.transportbackend.service.AuthenticatedCompanyProvider;
import com.thinkcode.transportbackend.service.ClientService;
import com.thinkcode.transportbackend.dto.ClientRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/clients")
public class ClientController {

    private final ClientService clientService;
    private final AuthenticatedCompanyProvider authenticatedCompanyProvider;

    public ClientController(ClientService clientService, AuthenticatedCompanyProvider authenticatedCompanyProvider) {
        this.clientService = clientService;
        this.authenticatedCompanyProvider = authenticatedCompanyProvider;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'ASSISTANT')")
    public List<Client> findAll() {
        return clientService.findAll(authenticatedCompanyProvider.requireCompanyId());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'ASSISTANT')")
    public Client create(@Valid @RequestBody ClientRequest request) {
        return clientService.create(request);
    }

    @PutMapping("/{clientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'ASSISTANT')")
    public Client update(@PathVariable UUID clientId, @Valid @RequestBody ClientRequest request) {
        return clientService.update(clientId, request);
    }

    @DeleteMapping("/{clientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER')")
    public void delete(@PathVariable UUID clientId) {
        clientService.delete(clientId);
    }
}

