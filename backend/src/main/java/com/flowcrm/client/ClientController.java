package com.flowcrm.client;

import com.flowcrm.client.dto.ClientRequest;
import com.flowcrm.client.dto.ClientResponse;
import com.flowcrm.client.dto.ClientUpdateRequest;
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
@RequestMapping("/api/v1/clients")
@Tag(name = "Clients", description = "Gerenciamento de clientes")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping
    @Operation(summary = "Listar clientes com paginação, busca e filtros")
    public ResponseEntity<Page<ClientResponse>> findAll(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ClientStatus status) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ClientResponse> result;
        if (search != null && !search.isBlank()) {
            result = clientService.search(userPrincipal.getCompanyId(), search, pageable);
        } else if (status != null) {
            result = clientService.findByStatus(userPrincipal.getCompanyId(), status, pageable);
        } else {
            result = clientService.findAll(userPrincipal.getCompanyId(), pageable);
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar cliente por ID")
    public ResponseEntity<ClientResponse> findById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(clientService.findById(id, userPrincipal.getCompanyId()));
    }

    @PostMapping
    @Operation(summary = "Criar novo cliente")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SALES')")
    public ResponseEntity<ClientResponse> create(
            @Valid @RequestBody ClientRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(clientService.create(userPrincipal.getCompanyId(), request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar cliente")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SALES')")
    public ResponseEntity<ClientResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody ClientUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(clientService.update(id, userPrincipal.getCompanyId(), request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir cliente")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        clientService.delete(id, userPrincipal.getCompanyId());
        return ResponseEntity.noContent().build();
    }
}
