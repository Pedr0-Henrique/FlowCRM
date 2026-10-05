package com.flowcrm.company;

import com.flowcrm.company.dto.CompanyRequest;
import com.flowcrm.company.dto.CompanyResponse;
import com.flowcrm.shared.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/companies")
@Tag(name = "Companies", description = "Gerenciamento de empresas")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping
    @Operation(summary = "Listar todas as empresas (ADMIN apenas)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CompanyResponse>> findAll() {
        return ResponseEntity.ok(companyService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar empresa por ID (ADMIN apenas)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CompanyResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(companyService.findById(id));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Buscar empresa por slug (ADMIN apenas)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CompanyResponse> findBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(companyService.findBySlug(slug));
    }

    @PostMapping
    @Operation(summary = "Criar nova empresa (ADMIN apenas)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CompanyResponse> create(@Valid @RequestBody CompanyRequest request) {
        return ResponseEntity.ok(companyService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar empresa (ADMIN apenas)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CompanyResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody CompanyRequest request) {
        return ResponseEntity.ok(companyService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir empresa (ADMIN apenas)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        companyService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
