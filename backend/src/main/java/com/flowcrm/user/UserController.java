package com.flowcrm.user;

import com.flowcrm.shared.security.UserPrincipal;
import com.flowcrm.user.dto.UserRequest;
import com.flowcrm.user.dto.UserResponse;
import com.flowcrm.user.dto.UserUpdateRequest;
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
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Gerenciamento de usuários")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "Listar usuários da empresa (ADMIN, MANAGER)")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<UserResponse>> findAll(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(userService.findAllByCompanyId(userPrincipal.getCompanyId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar usuário por ID")
    public ResponseEntity<UserResponse> findById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(userService.findById(id, userPrincipal.getCompanyId()));
    }

    @PostMapping
    @Operation(summary = "Criar novo usuário (ADMIN, MANAGER)")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<UserResponse> create(
            @Valid @RequestBody UserRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal.getRole().equals("MANAGER") &&
                request.role() != null &&
                request.role() != Role.SALES &&
                request.role() != Role.USER) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Managers podem criar apenas usuários SALES ou USER"
            );
        }

        UserRequest companyScopedRequest = new UserRequest(
                userPrincipal.getCompanyId(),
                request.name(),
                request.email(),
                request.password(),
                request.role(),
                request.active()
        );
        return ResponseEntity.ok(userService.create(companyScopedRequest));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar usuário (ADMIN pode alterar qualquer usuário, outros apenas próprio)")
    public ResponseEntity<UserResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UserUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        if (!userPrincipal.getRole().equals("ADMIN") && !userPrincipal.getUserId().equals(id)) {
            throw new org.springframework.security.access.AccessDeniedException("Sem permissão para alterar outro usuário");
        }

        UserUpdateRequest scopedRequest = userPrincipal.getRole().equals("ADMIN")
                ? request
                : new UserUpdateRequest(request.name(), request.email(), request.password(), null, null);
        return ResponseEntity.ok(userService.update(id, userPrincipal.getCompanyId(), scopedRequest));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir usuário (ADMIN apenas)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        userService.delete(id, userPrincipal.getCompanyId());
        return ResponseEntity.noContent().build();
    }
}
