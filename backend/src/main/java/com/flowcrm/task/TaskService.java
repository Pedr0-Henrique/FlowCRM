package com.flowcrm.task;

import com.flowcrm.client.Client;
import com.flowcrm.client.ClientRepository;
import com.flowcrm.company.Company;
import com.flowcrm.company.CompanyRepository;
import com.flowcrm.lead.Lead;
import com.flowcrm.lead.LeadRepository;
import com.flowcrm.shared.exception.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import com.flowcrm.task.dto.TaskRequest;
import com.flowcrm.task.dto.TaskResponse;
import com.flowcrm.task.dto.TaskUpdateRequest;
import com.flowcrm.user.User;
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
public class TaskService {

    private final TaskRepository taskRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final LeadRepository leadRepository;

    @Transactional(readOnly = true)
    public Page<TaskResponse> findAll(UUID companyId, Pageable pageable) {
        return taskRepository.findByCompanyId(companyId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> findByStatus(UUID companyId, TaskStatus status, Pageable pageable) {
        return taskRepository.findByCompanyIdAndStatus(companyId, status, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> findByPriority(UUID companyId, TaskPriority priority, Pageable pageable) {
        return taskRepository.findByCompanyIdAndPriority(companyId, priority, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> findByAssignedTo(UUID companyId, UUID assignedToId, Pageable pageable) {
        return taskRepository.findByCompanyIdAndAssignedToId(companyId, assignedToId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> findByClient(UUID companyId, UUID clientId, Pageable pageable) {
        return taskRepository.findByCompanyIdAndClientId(companyId, clientId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> findByLead(UUID companyId, UUID leadId, Pageable pageable) {
        return taskRepository.findByCompanyIdAndLeadId(companyId, leadId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> search(UUID companyId, String search, Pageable pageable) {
        if (search == null || search.isBlank()) {
            return findAll(companyId, pageable);
        }
        return taskRepository.searchByCompanyId(companyId, search, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public TaskResponse findById(UUID id, UUID companyId) {
        Task task = taskRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa não encontrada"));
        return toResponse(task);
    }

    @Transactional
    public TaskResponse create(
            UUID companyId,
            UUID userId,
            boolean canManageAllTasks,
            TaskRequest request
    ) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada"));

        User assignedTo = null;
        UUID assignedToId = canManageAllTasks ? request.assignedToId() : userId;
        if (assignedToId != null) {
            assignedTo = userRepository.findByIdAndCompanyId(assignedToId, companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Responsável não encontrado"));
        }

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

        Task task = new Task(company, request.title());
        updateTaskFromRequest(task, request);
        task.setAssignedTo(assignedTo);
        task.setClient(client);
        task.setLead(lead);
        task = taskRepository.save(task);
        return toResponse(task);
    }

    @Transactional
    public TaskResponse update(
            UUID id,
            UUID companyId,
            UUID userId,
            boolean canManageAllTasks,
            TaskUpdateRequest request
    ) {
        Task task = taskRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa não encontrada"));
        if (!canManageAllTasks
                && (task.getAssignedTo() == null || !userId.equals(task.getAssignedTo().getId())
                || (request.assignedToId() != null && !userId.equals(request.assignedToId())))) {
            throw new AccessDeniedException("Sem permissão para alterar tarefa de outro usuário");
        }

        UUID assignedToId = canManageAllTasks ? request.assignedToId() : userId;
        if (assignedToId != null) {
            User assignedTo = userRepository.findByIdAndCompanyId(assignedToId, companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Responsável não encontrado"));
            task.setAssignedTo(assignedTo);
        } else {
            task.setAssignedTo(null);
        }

        if (request.clientId() != null) {
            Client client = clientRepository.findByIdAndCompanyId(request.clientId(), companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));
            task.setClient(client);
        } else if (request.clientId() == null) {
            task.setClient(null);
        }

        if (request.leadId() != null) {
            Lead lead = leadRepository.findByIdAndCompanyId(request.leadId(), companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Lead não encontrado"));
            task.setLead(lead);
        } else if (request.leadId() == null) {
            task.setLead(null);
        }

        TaskStatus oldStatus = task.getStatus();
        updateTaskFromRequest(task, request);

        if (oldStatus != TaskStatus.COMPLETED && task.getStatus() == TaskStatus.COMPLETED) {
            task.setCompletedAt(Instant.now());
        } else if (task.getStatus() != TaskStatus.COMPLETED) {
            task.setCompletedAt(null);
        }

        task = taskRepository.save(task);
        return toResponse(task);
    }

    @Transactional
    public TaskResponse complete(UUID id, UUID companyId, UUID userId, boolean canManageAllTasks) {
        Task task = taskRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa não encontrada"));
        if (!canManageAllTasks
                && (task.getAssignedTo() == null || !userId.equals(task.getAssignedTo().getId()))) {
            throw new AccessDeniedException("Sem permissão para concluir esta tarefa");
        }
        if (task.getStatus() != TaskStatus.COMPLETED) {
            task.setStatus(TaskStatus.COMPLETED);
            task.setCompletedAt(Instant.now());
        }
        return toResponse(taskRepository.save(task));
    }

    @Transactional
    public void delete(UUID id, UUID companyId) {
        Task task = taskRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa não encontrada"));
        taskRepository.delete(task);
    }

    private void updateTaskFromRequest(Task task, Object request) {
        if (request instanceof TaskRequest req) {
            task.setDescription(req.description());
            task.setStatus(req.status() != null ? req.status() : TaskStatus.TODO);
            task.setPriority(req.priority() != null ? req.priority() : TaskPriority.MEDIUM);
            task.setDueDate(req.dueDate());
        } else if (request instanceof TaskUpdateRequest req) {
            if (req.title() != null) task.setTitle(req.title());
            if (req.description() != null) task.setDescription(req.description());
            if (req.status() != null) task.setStatus(req.status());
            if (req.priority() != null) task.setPriority(req.priority());
            if (req.dueDate() != null) task.setDueDate(req.dueDate());
        }
    }

    private TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate(),
                task.getCompletedAt(),
                task.getAssignedTo() != null ? task.getAssignedTo().getId() : null,
                task.getAssignedTo() != null ? task.getAssignedTo().getName() : null,
                task.getClient() != null ? task.getClient().getId() : null,
                task.getClient() != null ? task.getClient().getName() : null,
                task.getLead() != null ? task.getLead().getId() : null,
                task.getLead() != null ? task.getLead().getName() : null,
                task.getCompany().getId(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
