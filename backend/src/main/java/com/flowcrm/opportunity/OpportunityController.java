package com.flowcrm.opportunity;

import com.flowcrm.opportunity.dto.OpportunityRequest;
import com.flowcrm.opportunity.dto.OpportunityResponse;
import com.flowcrm.opportunity.dto.OpportunityUpdateRequest;
import com.flowcrm.shared.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/opportunities")
@Tag(name = "Opportunities", description = "Gerenciamento de oportunidades de vendas")
public class OpportunityController {

    private final OpportunityService opportunityService;

    public OpportunityController(OpportunityService opportunityService) {
        this.opportunityService = opportunityService;
    }

    @GetMapping
    @Operation(summary = "Listar oportunidades com paginação, busca e filtros")
    public ResponseEntity<Page<OpportunityResponse>> findAll(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "title") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) OpportunityStage stage,
            @RequestParam(required = false) UUID client,
            @RequestParam(required = false) UUID lead,
            @RequestParam(required = false) UUID assignedTo) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<OpportunityResponse> result;
        if (search != null && !search.isBlank()) {
            result = opportunityService.search(userPrincipal.getCompanyId(), search, pageable);
        } else if (stage != null) {
            result = opportunityService.findByStage(userPrincipal.getCompanyId(), stage, pageable);
        } else if (client != null) {
            result = opportunityService.findByClient(userPrincipal.getCompanyId(), client, pageable);
        } else if (lead != null) {
            result = opportunityService.findByLead(userPrincipal.getCompanyId(), lead, pageable);
        } else if (assignedTo != null) {
            result = opportunityService.findByAssignedTo(userPrincipal.getCompanyId(), assignedTo, pageable);
        } else {
            result = opportunityService.findAll(userPrincipal.getCompanyId(), pageable);
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar oportunidade por ID")
    public ResponseEntity<OpportunityResponse> findById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(opportunityService.findById(id, userPrincipal.getCompanyId()));
    }

    @PostMapping
    @Operation(summary = "Criar nova oportunidade")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SALES')")
    public ResponseEntity<OpportunityResponse> create(
            @Valid @RequestBody OpportunityRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(opportunityService.create(userPrincipal.getCompanyId(), request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar oportunidade")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SALES')")
    public ResponseEntity<OpportunityResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody OpportunityUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(opportunityService.update(id, userPrincipal.getCompanyId(), request));
    }

    @PatchMapping("/{id}/stage")
    @Operation(summary = "Atualizar estágio da oportunidade (para pipeline)")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SALES')")
    public ResponseEntity<OpportunityResponse> updateStage(
            @PathVariable UUID id,
            @RequestParam OpportunityStage stage,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        OpportunityUpdateRequest request = new OpportunityUpdateRequest(null, null, null, stage, null, null, null, null, null, null, null);
        return ResponseEntity.ok(opportunityService.update(id, userPrincipal.getCompanyId(), request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir oportunidade")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        opportunityService.delete(id, userPrincipal.getCompanyId());
        return ResponseEntity.noContent().build();
    }
}
