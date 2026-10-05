package com.flowcrm.user;

import com.flowcrm.company.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailAndCompanyId(String email, UUID companyId);

    boolean existsByEmail(String email);

    boolean existsByEmailAndCompanyId(String email, UUID companyId);

    List<User> findAllByCompanyIdOrderByNameAsc(UUID companyId);

    Optional<User> findByIdAndCompanyId(UUID id, UUID companyId);
}
