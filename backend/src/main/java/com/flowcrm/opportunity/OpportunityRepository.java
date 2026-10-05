package com.flowcrm.opportunity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OpportunityRepository extends JpaRepository<Opportunity, UUID> {

    Page<Opportunity> findByCompanyId(UUID companyId, Pageable pageable);

    Page<Opportunity> findByCompanyIdAndStage(UUID companyId, OpportunityStage stage, Pageable pageable);

    Page<Opportunity> findByCompanyIdAndClientId(UUID companyId, UUID clientId, Pageable pageable);

    Page<Opportunity> findByCompanyIdAndLeadId(UUID companyId, UUID leadId, Pageable pageable);

    Page<Opportunity> findByCompanyIdAndAssignedToId(UUID companyId, UUID assignedToId, Pageable pageable);

    @Query("SELECT o FROM Opportunity o WHERE o.company.id = :companyId AND " +
           "(LOWER(o.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(o.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Opportunity> searchByCompanyId(@Param("companyId") UUID companyId,
                                        @Param("search") String search,
                                        Pageable pageable);

    Optional<Opportunity> findByIdAndCompanyId(UUID id, UUID companyId);
}
