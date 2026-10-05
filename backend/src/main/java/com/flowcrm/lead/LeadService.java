package com.flowcrm.lead;

import com.flowcrm.company.Company;
import com.flowcrm.company.CompanyRepository;
import com.flowcrm.lead.dto.LeadRequest;
import com.flowcrm.lead.dto.LeadResponse;
import com.flowcrm.lead.dto.LeadUpdateRequest;
import com.flowcrm.shared.exception.ConflictException;
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
public class LeadService {

    private final LeadRepository leadRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

    public Page<LeadResponse> findAll(UUID companyId, Pageable pageable) {
        return leadRepository.findByCompanyId(companyId, pageable)
                .map(this::toResponse);
    }

    public Page<LeadResponse> findByStatus(UUID companyId, LeadStatus status, Pageable pageable) {
        return leadRepository.findByCompanyIdAndStatus(companyId, status, pageable)
                .map(this::toResponse);
    }

    public Page<LeadResponse> findByPriority(UUID companyId, LeadPriority priority, Pageable pageable) {
        return leadRepository.findByCompanyIdAndPriority(companyId, priority, pageable)
                .map(this::toResponse);
    }

    public Page<LeadResponse> findByAssignedTo(UUID companyId, UUID assignedToId, Pageable pageable) {
        return leadRepository.findByCompanyIdAndAssignedToId(companyId, assignedToId, pageable)
                .map(this::toResponse);
    }

    public Page<LeadResponse> search(UUID companyId, String search, Pageable pageable) {
        if (search == null || search.isBlank()) {
            return findAll(companyId, pageable);
        }
        return leadRepository.searchByCompanyId(companyId, search, pageable)
                .map(this::toResponse);
    }

    public LeadResponse findById(UUID id, UUID companyId) {
        Lead lead = leadRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead não encontrado"));
        return toResponse(lead);
    }

    @Transactional
    public LeadResponse create(UUID companyId, LeadRequest request) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada"));

        if (request.email() != null && leadRepository.existsByEmailAndCompanyId(request.email(), companyId)) {
            throw new ConflictException("E-mail já cadastrado para outro lead");
        }

        User assignedTo = null;
        if (request.assignedToId() != null) {
            assignedTo = userRepository.findByIdAndCompanyId(request.assignedToId(), companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Responsável não encontrado"));
        }

        Lead lead = new Lead(company, request.name());
        updateLeadFromRequest(lead, request);
        lead.setAssignedTo(assignedTo);
        lead = leadRepository.save(lead);
        return toResponse(lead);
    }

    @Transactional
    public LeadResponse update(UUID id, UUID companyId, LeadUpdateRequest request) {
        Lead lead = leadRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead não encontrado"));

        if (request.email() != null && !request.email().equals(lead.getEmail())) {
            if (leadRepository.existsByEmailAndCompanyId(request.email(), companyId)) {
                throw new ConflictException("E-mail já cadastrado para outro lead");
            }
        }

        if (request.assignedToId() != null) {
            User assignedTo = userRepository.findByIdAndCompanyId(request.assignedToId(), companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Responsável não encontrado"));
            lead.setAssignedTo(assignedTo);
        } else if (request.assignedToId() == null) {
            lead.setAssignedTo(null);
        }

        updateLeadFromRequest(lead, request);
        lead = leadRepository.save(lead);
        return toResponse(lead);
    }

    @Transactional
    public void delete(UUID id, UUID companyId) {
        Lead lead = leadRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead não encontrado"));
        leadRepository.delete(lead);
    }

    private void updateLeadFromRequest(Lead lead, Object request) {
        if (request instanceof LeadRequest req) {
            lead.setEmail(req.email());
            lead.setPhone(req.phone());
            lead.setSource(req.source());
            lead.setStatus(req.status() != null ? req.status() : LeadStatus.NEW);
            lead.setPriority(req.priority() != null ? req.priority() : LeadPriority.MEDIUM);
            lead.setEstimatedValue(req.estimatedValue());
            lead.setNotes(req.notes());
        } else if (request instanceof LeadUpdateRequest req) {
            if (req.name() != null) lead.setName(req.name());
            if (req.email() != null) lead.setEmail(req.email());
            if (req.phone() != null) lead.setPhone(req.phone());
            if (req.source() != null) lead.setSource(req.source());
            if (req.status() != null) lead.setStatus(req.status());
            if (req.priority() != null) lead.setPriority(req.priority());
            if (req.estimatedValue() != null) lead.setEstimatedValue(req.estimatedValue());
            if (req.notes() != null) lead.setNotes(req.notes());
        }
    }

    private LeadResponse toResponse(Lead lead) {
        return new LeadResponse(
                lead.getId(),
                lead.getName(),
                lead.getEmail(),
                lead.getPhone(),
                lead.getSource(),
                lead.getStatus(),
                lead.getPriority(),
                lead.getEstimatedValue(),
                lead.getNotes(),
                lead.getAssignedTo() != null ? lead.getAssignedTo().getId() : null,
                lead.getAssignedTo() != null ? lead.getAssignedTo().getName() : null,
                lead.getCompany().getId(),
                lead.getCreatedAt(),
                lead.getUpdatedAt()
        );
    }
}
