package com.flowcrm.company;

import com.flowcrm.company.dto.CompanyRequest;
import com.flowcrm.company.dto.CompanyResponse;
import com.flowcrm.shared.exception.ConflictException;
import com.flowcrm.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;

    public List<CompanyResponse> findAll() {
        return companyRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public CompanyResponse findById(UUID id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada"));
        return toResponse(company);
    }

    public CompanyResponse findBySlug(String slug) {
        Company company = companyRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada"));
        return toResponse(company);
    }

    @Transactional
    public CompanyResponse create(CompanyRequest request) {
        if (companyRepository.existsBySlug(request.slug())) {
            throw new ConflictException("Slug já está em uso");
        }

        Company company = new Company(request.name(), request.slug());
        company = companyRepository.save(company);
        return toResponse(company);
    }

    @Transactional
    public CompanyResponse update(UUID id, CompanyRequest request) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada"));

        if (!company.getSlug().equals(request.slug()) && companyRepository.existsBySlug(request.slug())) {
            throw new ConflictException("Slug já está em uso");
        }

        company.setName(request.name());
        company.setSlug(request.slug());
        company = companyRepository.save(company);
        return toResponse(company);
    }

    @Transactional
    public void delete(UUID id) {
        if (!companyRepository.existsById(id)) {
            throw new ResourceNotFoundException("Empresa não encontrada");
        }
        companyRepository.deleteById(id);
    }

    private CompanyResponse toResponse(Company company) {
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getSlug(),
                company.getCreatedAt(),
                company.getUpdatedAt()
        );
    }
}
