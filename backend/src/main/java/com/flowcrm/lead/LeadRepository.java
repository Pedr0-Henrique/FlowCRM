package com.flowcrm.lead;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeadRepository extends JpaRepository<Lead, UUID> {

    Page<Lead> findByCompanyId(UUID companyId, Pageable pageable);

    Page<Lead> findByCompanyIdAndStatus(UUID companyId, LeadStatus status, Pageable pageable);

    Page<Lead> findByCompanyIdAndPriority(UUID companyId, LeadPriority priority, Pageable pageable);

    Page<Lead> findByCompanyIdAndAssignedToId(UUID companyId, UUID assignedToId, Pageable pageable);

    @Query("SELECT l FROM Lead l WHERE l.company.id = :companyId AND " +
           "(LOWER(l.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(l.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(l.phone) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Lead> searchByCompanyId(@Param("companyId") UUID companyId,
                                  @Param("search") String search,
                                  Pageable pageable);

    Optional<Lead> findByIdAndCompanyId(UUID id, UUID companyId);

    boolean existsByEmailAndCompanyId(String email, UUID companyId);
}
