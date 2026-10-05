package com.flowcrm.task;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {

    Page<Task> findByCompanyId(UUID companyId, Pageable pageable);

    Page<Task> findByCompanyIdAndStatus(UUID companyId, TaskStatus status, Pageable pageable);

    Page<Task> findByCompanyIdAndPriority(UUID companyId, TaskPriority priority, Pageable pageable);

    Page<Task> findByCompanyIdAndAssignedToId(UUID companyId, UUID assignedToId, Pageable pageable);

    Page<Task> findByCompanyIdAndClientId(UUID companyId, UUID clientId, Pageable pageable);

    Page<Task> findByCompanyIdAndLeadId(UUID companyId, UUID leadId, Pageable pageable);

    @Query("SELECT t FROM Task t WHERE t.company.id = :companyId AND " +
           "(LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Task> searchByCompanyId(@Param("companyId") UUID companyId,
                                 @Param("search") String search,
                                 Pageable pageable);

    Optional<Task> findByIdAndCompanyId(UUID id, UUID companyId);
}
