package com.flowcrm.lead;

import com.flowcrm.lead.dto.LeadRequest;
import com.flowcrm.lead.dto.LeadResponse;
import com.flowcrm.lead.dto.LeadUpdateRequest;
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
@RequestMapping("/api/v1/leads")
@Tag(name = "Leads", description = "Gerenciamento de leads com pipeline")
public class LeadController {

    private final LeadService leadService;

    public LeadController(LeadService leadService) {
        this.leadService = leadService;
    }

    @GetMapping
    @Operation(summary = "Listar leads com paginação, busca e filtros")
    public ResponseEntity<Page<LeadResponse>> findAll(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) LeadStatus status,
            @RequestParam(required = false) LeadPriority priority,
            @RequestParam(required = false) UUID assignedTo) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<LeadResponse> result;
        if (search != null && !search.isBlank()) {
            result = leadService.search(userPrincipal.getCompanyId(), search, pageable);
        } else if (status != null) {
            result = leadService.findByStatus(userPrincipal.getCompanyId(), status, pageable);
        } else if (priority != null) {
            result = leadService.findByPriority(userPrincipal.getCompanyId(), priority, pageable);
        } else if (assignedTo != null) {
            result = leadService.findByAssignedTo(userPrincipal.getCompanyId(), assignedTo, pageable);
        } else {
            result = leadService.findAll(userPrincipal.getCompanyId(), pageable);
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar lead por ID")
    public ResponseEntity<LeadResponse> findById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(leadService.findById(id, userPrincipal.getCompanyId()));
    }

    @PostMapping
    @Operation(summary = "Criar novo lead")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SALES')")
    public ResponseEntity<LeadResponse> create(
            @Valid @RequestBody LeadRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(leadService.create(userPrincipal.getCompanyId(), request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar lead (inclui mudança de status para pipeline)")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SALES')")
    public ResponseEntity<LeadResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody LeadUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(leadService.update(id, userPrincipal.getCompanyId(), request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Atualizar status do lead (para drag & drop do pipeline)")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SALES')")
    public ResponseEntity<LeadResponse> updateStatus(
            @PathVariable UUID id,
            @RequestParam LeadStatus status,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        LeadUpdateRequest request = new LeadUpdateRequest(null, null, null, null, status, null, null, null, null);
        return ResponseEntity.ok(leadService.update(id, userPrincipal.getCompanyId(), request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir lead")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        leadService.delete(id, userPrincipal.getCompanyId());
        return ResponseEntity.noContent().build();
    }
}
