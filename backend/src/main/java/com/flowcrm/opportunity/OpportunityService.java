package com.flowcrm.opportunity;

import com.flowcrm.client.Client;
import com.flowcrm.client.ClientRepository;
import com.flowcrm.company.Company;
import com.flowcrm.company.CompanyRepository;
import com.flowcrm.lead.Lead;
import com.flowcrm.lead.LeadRepository;
import com.flowcrm.opportunity.dto.OpportunityRequest;
import com.flowcrm.opportunity.dto.OpportunityResponse;
import com.flowcrm.opportunity.dto.OpportunityUpdateRequest;
import com.flowcrm.shared.exception.ResourceNotFoundException;
import com.flowcrm.user.User;
import com.flowcrm.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OpportunityService {

    private final OpportunityRepository opportunityRepository;
    private final CompanyRepository companyRepository;
    private final ClientRepository clientRepository;
    private final LeadRepository leadRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<OpportunityResponse> findAll(UUID companyId, Pageable pageable) {
        return opportunityRepository.findByCompanyId(companyId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<OpportunityResponse> findByStage(UUID companyId, OpportunityStage stage, Pageable pageable) {
        return opportunityRepository.findByCompanyIdAndStage(companyId, stage, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<OpportunityResponse> findByClient(UUID companyId, UUID clientId, Pageable pageable) {
        return opportunityRepository.findByCompanyIdAndClientId(companyId, clientId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<OpportunityResponse> findByLead(UUID companyId, UUID leadId, Pageable pageable) {
        return opportunityRepository.findByCompanyIdAndLeadId(companyId, leadId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<OpportunityResponse> findByAssignedTo(UUID companyId, UUID assignedToId, Pageable pageable) {
        return opportunityRepository.findByCompanyIdAndAssignedToId(companyId, assignedToId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<OpportunityResponse> search(UUID companyId, String search, Pageable pageable) {
        if (search == null || search.isBlank()) {
            return findAll(companyId, pageable);
        }
        return opportunityRepository.searchByCompanyId(companyId, search, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public OpportunityResponse findById(UUID id, UUID companyId) {
        Opportunity opportunity = opportunityRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Oportunidade não encontrada"));
        return toResponse(opportunity);
    }

    @Transactional
    public OpportunityResponse create(UUID companyId, OpportunityRequest request) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada"));

        Client client = null;
        if (request.clientId() != null) {
            client = clientRepository.findByIdAndCompanyId(request.clientId(), companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));
        }

        Lead lead = null;
        if (request.leadId() != null) {
            lead = leadRepository.findByIdAndCompanyId(request.leadId(), companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Lead não encontrado"));
        }

        User assignedTo = null;
        if (request.assignedToId() != null) {
            assignedTo = userRepository.findByIdAndCompanyId(request.assignedToId(), companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Responsável não encontrado"));
        }

        Opportunity opportunity = new Opportunity(company, request.title(), request.value());
        updateOpportunityFromRequest(opportunity, request);
        opportunity.setClient(client);
        opportunity.setLead(lead);
        opportunity.setAssignedTo(assignedTo);
        opportunity = opportunityRepository.save(opportunity);
        return toResponse(opportunity);
    }

    @Transactional
    public OpportunityResponse update(UUID id, UUID companyId, OpportunityUpdateRequest request) {
        Opportunity opportunity = opportunityRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Oportunidade não encontrada"));

        if (request.clientId() != null) {
            Client client = clientRepository.findByIdAndCompanyId(request.clientId(), companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));
            opportunity.setClient(client);
        } else if (request.clientId() == null) {
            opportunity.setClient(null);
        }

        if (request.leadId() != null) {
            Lead lead = leadRepository.findByIdAndCompanyId(request.leadId(), companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Lead não encontrado"));
            opportunity.setLead(lead);
        } else if (request.leadId() == null) {
            opportunity.setLead(null);
        }

        if (request.assignedToId() != null) {
            User assignedTo = userRepository.findByIdAndCompanyId(request.assignedToId(), companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Responsável não encontrado"));
            opportunity.setAssignedTo(assignedTo);
        } else if (request.assignedToId() == null) {
            opportunity.setAssignedTo(null);
        }

        updateOpportunityFromRequest(opportunity, request);
        opportunity = opportunityRepository.save(opportunity);
        return toResponse(opportunity);
    }

    @Transactional
    public void delete(UUID id, UUID companyId) {
        Opportunity opportunity = opportunityRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Oportunidade não encontrada"));
        opportunityRepository.delete(opportunity);
    }

    private void updateOpportunityFromRequest(Opportunity opportunity, Object request) {
        if (request instanceof OpportunityRequest req) {
            opportunity.setDescription(req.description());
            opportunity.setStage(req.stage() != null ? req.stage() : OpportunityStage.NEW);
            opportunity.setProbability(req.probability() != null ? req.probability() : 10);
            opportunity.setExpectedCloseDate(req.expectedCloseDate());
            opportunity.setNotes(req.notes());
        } else if (request instanceof OpportunityUpdateRequest req) {
            if (req.title() != null) opportunity.setTitle(req.title());
            if (req.description() != null) opportunity.setDescription(req.description());
            if (req.value() != null) opportunity.setValue(req.value());
            if (req.stage() != null) opportunity.setStage(req.stage());
            if (req.probability() != null) opportunity.setProbability(req.probability());
            if (req.expectedCloseDate() != null) opportunity.setExpectedCloseDate(req.expectedCloseDate());
            if (req.actualCloseDate() != null) opportunity.setActualCloseDate(req.actualCloseDate());
            if (req.notes() != null) opportunity.setNotes(req.notes());
        }
    }

    private OpportunityResponse toResponse(Opportunity opportunity) {
        return new OpportunityResponse(
                opportunity.getId(),
                opportunity.getTitle(),
                opportunity.getDescription(),
                opportunity.getValue(),
                opportunity.getStage(),
                opportunity.getProbability(),
                opportunity.getExpectedCloseDate(),
                opportunity.getActualCloseDate(),
                opportunity.getNotes(),
                opportunity.getClient() != null ? opportunity.getClient().getId() : null,
                opportunity.getClient() != null ? opportunity.getClient().getName() : null,
                opportunity.getLead() != null ? opportunity.getLead().getId() : null,
                opportunity.getLead() != null ? opportunity.getLead().getName() : null,
                opportunity.getAssignedTo() != null ? opportunity.getAssignedTo().getId() : null,
                opportunity.getAssignedTo() != null ? opportunity.getAssignedTo().getName() : null,
                opportunity.getCompany().getId(),
                opportunity.getCreatedAt(),
                opportunity.getUpdatedAt()
        );
    }
}
