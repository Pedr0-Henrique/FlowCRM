package com.flowcrm.task;

import com.flowcrm.shared.security.UserPrincipal;
import com.flowcrm.task.dto.TaskRequest;
import com.flowcrm.task.dto.TaskResponse;
import com.flowcrm.task.dto.TaskUpdateRequest;
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
@RequestMapping("/api/v1/tasks")
@Tag(name = "Tasks", description = "Gerenciamento de tarefas")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    @Operation(summary = "Listar tarefas com paginação, busca e filtros")
    public ResponseEntity<Page<TaskResponse>> findAll(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "title") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) UUID assignedTo,
            @RequestParam(required = false) UUID client,
            @RequestParam(required = false) UUID lead) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<TaskResponse> result;
        if (search != null && !search.isBlank()) {
            result = taskService.search(userPrincipal.getCompanyId(), search, pageable);
        } else if (status != null) {
            result = taskService.findByStatus(userPrincipal.getCompanyId(), status, pageable);
        } else if (priority != null) {
            result = taskService.findByPriority(userPrincipal.getCompanyId(), priority, pageable);
        } else if (assignedTo != null) {
            result = taskService.findByAssignedTo(userPrincipal.getCompanyId(), assignedTo, pageable);
        } else if (client != null) {
            result = taskService.findByClient(userPrincipal.getCompanyId(), client, pageable);
        } else if (lead != null) {
            result = taskService.findByLead(userPrincipal.getCompanyId(), lead, pageable);
        } else {
            result = taskService.findAll(userPrincipal.getCompanyId(), pageable);
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar tarefa por ID")
    public ResponseEntity<TaskResponse> findById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(taskService.findById(id, userPrincipal.getCompanyId()));
    }

    @PostMapping
    @Operation(summary = "Criar nova tarefa")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SALES', 'USER')")
    public ResponseEntity<TaskResponse> create(
            @Valid @RequestBody TaskRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (!canManageAllTasks(userPrincipal)
                && request.assignedToId() != null
                && !userPrincipal.getUserId().equals(request.assignedToId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Somente ADMIN e MANAGER podem atribuir tarefas a outros usuários"
            );
        }
        return ResponseEntity.ok(taskService.create(
                userPrincipal.getCompanyId(),
                userPrincipal.getUserId(),
                canManageAllTasks(userPrincipal),
                request
        ));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar tarefa")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SALES', 'USER')")
    public ResponseEntity<TaskResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody TaskUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(taskService.update(
                id,
                userPrincipal.getCompanyId(),
                userPrincipal.getUserId(),
                canManageAllTasks(userPrincipal),
                request
        ));
    }

    @PatchMapping("/{id}/complete")
    @Operation(summary = "Marcar tarefa como concluída")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SALES', 'USER')")
    public ResponseEntity<TaskResponse> complete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(taskService.complete(
                id,
                userPrincipal.getCompanyId(),
                userPrincipal.getUserId(),
                canManageAllTasks(userPrincipal)
        ));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir tarefa")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        taskService.delete(id, userPrincipal.getCompanyId());
        return ResponseEntity.noContent().build();
    }

    private boolean canManageAllTasks(UserPrincipal userPrincipal) {
        return "ADMIN".equals(userPrincipal.getRole()) || "MANAGER".equals(userPrincipal.getRole());
    }
}
