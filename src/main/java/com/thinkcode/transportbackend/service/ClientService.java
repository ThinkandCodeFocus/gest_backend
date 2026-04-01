package com.thinkcode.transportbackend.service;

import com.thinkcode.transportbackend.dto.ClientRequest;
import com.thinkcode.transportbackend.dto.ClientResponse;
import com.thinkcode.transportbackend.entity.Client;
import com.thinkcode.transportbackend.repository.ClientRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ClientService {

    private final ClientRepository clientRepository;
    private final CompanyResolver companyResolver;
    private final AuthenticatedCompanyProvider authenticatedCompanyProvider;

    public ClientService(
            ClientRepository clientRepository,
            CompanyResolver companyResolver,
            AuthenticatedCompanyProvider authenticatedCompanyProvider
    ) {
        this.clientRepository = clientRepository;
        this.companyResolver = companyResolver;
        this.authenticatedCompanyProvider = authenticatedCompanyProvider;
    }

    public List<Client> findAll(UUID companyId) {
        return clientRepository.findAllByCompanyId(companyId);
    }

    public List<ClientResponse> findAllResponses(UUID companyId) {
        return findAll(companyId).stream()
                .map(this::toResponse)
                .toList();
    }

    public Client findById(UUID id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Client not found"));
    }

    public Client findByIdForCompany(UUID id, UUID companyId) {
        return clientRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Client not found"));
    }

    public Client findOrCreateForCompany(UUID companyId, String name, String email, String phoneNumber) {
        if (name == null || name.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Client name is required");
        }

        Client client = clientRepository.findByCompanyIdAndNameIgnoreCase(companyId, name.trim())
                .orElseGet(() -> {
                    Client created = new Client();
                    created.setCompany(companyResolver.require(companyId));
                    created.setName(name.trim());
                    return created;
                });

        if (email != null && !email.isBlank()) {
            client.setEmail(email.trim());
        }
        if (phoneNumber != null && !phoneNumber.isBlank()) {
            client.setPhoneNumber(phoneNumber.trim());
        }

        return clientRepository.save(client);
    }

    public Client create(ClientRequest request) {
        UUID authenticatedCompanyId = authenticatedCompanyProvider.requireCompanyId();
        Client client = new Client();
        client.setName(request.name());
        client.setEmail(request.email());
        client.setPhoneNumber(request.phoneNumber());
        client.setCompany(companyResolver.require(authenticatedCompanyId));
        return clientRepository.save(client);
    }

    public ClientResponse createResponse(ClientRequest request) {
        return toResponse(create(request));
    }

    public Client update(UUID clientId, ClientRequest request) {
        UUID authenticatedCompanyId = authenticatedCompanyProvider.requireCompanyId();
        Client client = findByIdForCompany(clientId, authenticatedCompanyId);
        client.setName(request.name());
        client.setEmail(request.email());
        client.setPhoneNumber(request.phoneNumber());
        return clientRepository.save(client);
    }

    public ClientResponse updateResponse(UUID clientId, ClientRequest request) {
        return toResponse(update(clientId, request));
    }

    public void delete(UUID clientId) {
        UUID authenticatedCompanyId = authenticatedCompanyProvider.requireCompanyId();
        Client client = findByIdForCompany(clientId, authenticatedCompanyId);
        clientRepository.delete(client);
    }

    private ClientResponse toResponse(Client client) {
        return new ClientResponse(
                client.getId(),
                client.getName(),
                client.getEmail(),
                client.getPhoneNumber()
        );
    }
}

