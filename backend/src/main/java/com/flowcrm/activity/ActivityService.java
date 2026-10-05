package com.flowcrm.activity;

import com.flowcrm.activity.dto.ActivityRequest;
import com.flowcrm.activity.dto.ActivityResponse;
import com.flowcrm.client.ClientRepository;
import com.flowcrm.company.CompanyRepository;
import com.flowcrm.lead.LeadRepository;
import com.flowcrm.shared.exception.ResourceNotFoundException;
import com.flowcrm.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final LeadRepository leadRepository;

    @Transactional(readOnly = true)
    public Page<ActivityResponse> findAll(UUID companyId, Pageable pageable) {
        return activityRepository.findByCompanyId(companyId, pageable).map(this::toResponse);
    }

    @Transactional
    public ActivityResponse create(UUID companyId, UUID userId, ActivityRequest request) {
        var company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada"));
        var user = userRepository.findByIdAndCompanyId(userId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        var activity = new Activity(
                company,
                user,
                request.title(),
                request.type(),
                request.occurredAt() == null ? Instant.now() : request.occurredAt()
        );
        activity.setDescription(request.description());
        if (request.clientId() != null) {
            activity.setClient(clientRepository.findByIdAndCompanyId(request.clientId(), companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado")));
        }
        if (request.leadId() != null) {
            activity.setLead(leadRepository.findByIdAndCompanyId(request.leadId(), companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Lead não encontrado")));
        }
        return toResponse(activityRepository.save(activity));
    }

    @Transactional
    public void delete(UUID id, UUID companyId) {
        Activity activity = activityRepository.findById(id)
                .filter(item -> item.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new ResourceNotFoundException("Atividade não encontrada"));
        activityRepository.delete(activity);
    }

    private ActivityResponse toResponse(Activity activity) {
        return new ActivityResponse(
                activity.getId(),
                activity.getTitle(),
                activity.getDescription(),
                activity.getType(),
                activity.getOccurredAt(),
                activity.getUser().getId(),
                activity.getUser().getName(),
                activity.getClient() == null ? null : activity.getClient().getId(),
                activity.getClient() == null ? null : activity.getClient().getName(),
                activity.getLead() == null ? null : activity.getLead().getId(),
                activity.getLead() == null ? null : activity.getLead().getName()
        );
    }
}
