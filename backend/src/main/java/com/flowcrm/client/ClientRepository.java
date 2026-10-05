package com.flowcrm.client;

import com.flowcrm.client.ClientStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientRepository extends JpaRepository<Client, UUID> {

    Page<Client> findByCompanyId(UUID companyId, Pageable pageable);

    Page<Client> findByCompanyIdAndStatus(UUID companyId, ClientStatus status, Pageable pageable);

    @Query("SELECT c FROM Client c WHERE c.company.id = :companyId AND " +
           "(LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.phone) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Client> searchByCompanyId(@Param("companyId") UUID companyId,
                                    @Param("search") String search,
                                    Pageable pageable);

    Optional<Client> findByIdAndCompanyId(UUID id, UUID companyId);

    boolean existsByEmailAndCompanyId(String email, UUID companyId);
}
