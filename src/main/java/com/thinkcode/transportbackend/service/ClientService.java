package com.thinkcode.transportbackend.service;

import com.thinkcode.transportbackend.entity.Client;
import com.thinkcode.transportbackend.repository.ClientRepository;
import com.thinkcode.transportbackend.dto.ClientRequest;
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

    public Client findById(UUID id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Client not found"));
    }

    public Client findByIdForCompany(UUID id, UUID companyId) {
        return clientRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Client not found"));
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

    public Client update(UUID clientId, ClientRequest request) {
        UUID authenticatedCompanyId = authenticatedCompanyProvider.requireCompanyId();
        Client client = findByIdForCompany(clientId, authenticatedCompanyId);
        client.setName(request.name());
        client.setEmail(request.email());
        client.setPhoneNumber(request.phoneNumber());
        return clientRepository.save(client);
    }

    public void delete(UUID clientId) {
        UUID authenticatedCompanyId = authenticatedCompanyProvider.requireCompanyId();
        Client client = findByIdForCompany(clientId, authenticatedCompanyId);
        clientRepository.delete(client);
    }
}

