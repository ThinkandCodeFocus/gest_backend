package com.thinkcode.transportbackend.service;

import com.thinkcode.transportbackend.dto.ClientRequest;
import com.thinkcode.transportbackend.dto.ClientResponse;
import com.thinkcode.transportbackend.entity.Client;
import com.thinkcode.transportbackend.entity.RoleName;
import com.thinkcode.transportbackend.entity.UserAccount;
import com.thinkcode.transportbackend.repository.ClientRepository;
import com.thinkcode.transportbackend.repository.UserAccountRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class ClientService {

    private final ClientRepository clientRepository;
    private final UserAccountRepository userAccountRepository;
    private final CompanyResolver companyResolver;
    private final AuthenticatedCompanyProvider authenticatedCompanyProvider;
    private final PasswordEncoder passwordEncoder;

    public ClientService(
            ClientRepository clientRepository,
            UserAccountRepository userAccountRepository,
            CompanyResolver companyResolver,
            AuthenticatedCompanyProvider authenticatedCompanyProvider,
            PasswordEncoder passwordEncoder
    ) {
        this.clientRepository = clientRepository;
        this.userAccountRepository = userAccountRepository;
        this.companyResolver = companyResolver;
        this.authenticatedCompanyProvider = authenticatedCompanyProvider;
        this.passwordEncoder = passwordEncoder;
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
        Client saved = clientRepository.save(client);
        syncUserAccount(saved, null, request.password(), authenticatedCompanyId);
        return saved;
    }

    public ClientResponse createResponse(ClientRequest request) {
        return toResponse(create(request));
    }

    public Client update(UUID clientId, ClientRequest request) {
        UUID authenticatedCompanyId = authenticatedCompanyProvider.requireCompanyId();
        Client client = findByIdForCompany(clientId, authenticatedCompanyId);
        String previousEmail = client.getEmail();
        client.setName(request.name());
        client.setEmail(request.email());
        client.setPhoneNumber(request.phoneNumber());
        Client saved = clientRepository.save(client);
        syncUserAccount(saved, previousEmail, request.password(), authenticatedCompanyId);
        return saved;
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

    private void syncUserAccount(Client client, String previousEmail, String rawPassword, UUID companyId) {
        String normalizedEmail = client.getEmail() == null ? null : client.getEmail().trim().toLowerCase();
        String normalizedPreviousEmail = previousEmail == null ? null : previousEmail.trim().toLowerCase();

        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            return;
        }

        if (normalizedPreviousEmail != null && !normalizedPreviousEmail.equals(normalizedEmail)) {
            userAccountRepository.findByCompanyIdAndEmail(companyId, normalizedPreviousEmail)
                    .ifPresent(existing -> {
                        existing.setEmail(normalizedEmail);
                        existing.setFullName(client.getName());
                        existing.setRole(RoleName.CLIENT);
                        if (rawPassword != null && !rawPassword.isBlank()) {
                            existing.setPasswordHash(passwordEncoder.encode(rawPassword));
                        }
                        userAccountRepository.save(existing);
                    });
            return;
        }

        UserAccount account = userAccountRepository.findByCompanyIdAndEmail(companyId, normalizedEmail)
                .orElseGet(UserAccount::new);
        account.setCompany(companyResolver.require(companyId));
        account.setFullName(client.getName());
        account.setEmail(normalizedEmail);
        account.setRole(RoleName.CLIENT);
        if (account.getId() == null) {
            if (rawPassword == null || rawPassword.isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "A password is required to create a client account");
            }
            account.setPasswordHash(passwordEncoder.encode(rawPassword));
        } else if (rawPassword != null && !rawPassword.isBlank()) {
            account.setPasswordHash(passwordEncoder.encode(rawPassword));
        }
        userAccountRepository.save(account);
    }
}

