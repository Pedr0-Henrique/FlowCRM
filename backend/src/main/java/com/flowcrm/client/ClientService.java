package com.flowcrm.client;

import com.flowcrm.client.dto.ClientRequest;
import com.flowcrm.client.dto.ClientResponse;
import com.flowcrm.client.dto.ClientUpdateRequest;
import com.flowcrm.company.Company;
import com.flowcrm.company.CompanyRepository;
import com.flowcrm.shared.exception.ConflictException;
import com.flowcrm.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final CompanyRepository companyRepository;

    public Page<ClientResponse> findAll(UUID companyId, Pageable pageable) {
        return clientRepository.findByCompanyId(companyId, pageable)
                .map(this::toResponse);
    }

    public Page<ClientResponse> findByStatus(UUID companyId, ClientStatus status, Pageable pageable) {
        return clientRepository.findByCompanyIdAndStatus(companyId, status, pageable)
                .map(this::toResponse);
    }

    public Page<ClientResponse> search(UUID companyId, String search, Pageable pageable) {
        if (search == null || search.isBlank()) {
            return findAll(companyId, pageable);
        }
        return clientRepository.searchByCompanyId(companyId, search, pageable)
                .map(this::toResponse);
    }

    public ClientResponse findById(UUID id, UUID companyId) {
        Client client = clientRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));
        return toResponse(client);
    }

    @Transactional
    public ClientResponse create(UUID companyId, ClientRequest request) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada"));

        if (request.email() != null && clientRepository.existsByEmailAndCompanyId(request.email(), companyId)) {
            throw new ConflictException("E-mail já cadastrado para outro cliente");
        }

        Client client = new Client(company, request.name());
        updateClientFromRequest(client, request);
        client = clientRepository.save(client);
        return toResponse(client);
    }

    @Transactional
    public ClientResponse update(UUID id, UUID companyId, ClientUpdateRequest request) {
        Client client = clientRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));

        if (request.email() != null && !request.email().equals(client.getEmail())) {
            if (clientRepository.existsByEmailAndCompanyId(request.email(), companyId)) {
                throw new ConflictException("E-mail já cadastrado para outro cliente");
            }
        }

        updateClientFromRequest(client, request);
        client = clientRepository.save(client);
        return toResponse(client);
    }

    @Transactional
    public void delete(UUID id, UUID companyId) {
        Client client = clientRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));
        clientRepository.delete(client);
    }

    private void updateClientFromRequest(Client client, Object request) {
        if (request instanceof ClientRequest req) {
            client.setEmail(req.email());
            client.setPhone(req.phone());
            client.setAddress(req.address());
            client.setCity(req.city());
            client.setState(req.state());
            client.setZipCode(req.zipCode());
            client.setCountry(req.country());
            client.setStatus(req.status() != null ? req.status() : ClientStatus.ACTIVE);
            client.setNotes(req.notes());
        } else if (request instanceof ClientUpdateRequest req) {
            if (req.name() != null) client.setName(req.name());
            if (req.email() != null) client.setEmail(req.email());
            if (req.phone() != null) client.setPhone(req.phone());
            if (req.address() != null) client.setAddress(req.address());
            if (req.city() != null) client.setCity(req.city());
            if (req.state() != null) client.setState(req.state());
            if (req.zipCode() != null) client.setZipCode(req.zipCode());
            if (req.country() != null) client.setCountry(req.country());
            if (req.status() != null) client.setStatus(req.status());
            if (req.notes() != null) client.setNotes(req.notes());
        }
    }

    private ClientResponse toResponse(Client client) {
        return new ClientResponse(
                client.getId(),
                client.getName(),
                client.getEmail(),
                client.getPhone(),
                client.getAddress(),
                client.getCity(),
                client.getState(),
                client.getZipCode(),
                client.getCountry(),
                client.getStatus(),
                client.getNotes(),
                client.getCompany().getId(),
                client.getCreatedAt(),
                client.getUpdatedAt()
        );
    }
}
