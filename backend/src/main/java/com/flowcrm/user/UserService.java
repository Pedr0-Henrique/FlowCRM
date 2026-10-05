package com.flowcrm.user;

import com.flowcrm.company.Company;
import com.flowcrm.company.CompanyRepository;
import com.flowcrm.shared.exception.ConflictException;
import com.flowcrm.shared.exception.ResourceNotFoundException;
import com.flowcrm.user.dto.UserRequest;
import com.flowcrm.user.dto.UserResponse;
import com.flowcrm.user.dto.UserUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserResponse> findAllByCompanyId(UUID companyId) {
        return userRepository.findAllByCompanyIdOrderByNameAsc(companyId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(UUID id, UUID companyId) {
        User user = userRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        return toResponse(user);
    }

    @Transactional
    public UserResponse create(UserRequest request) {
        Company company = companyRepository.findById(request.companyId())
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada"));

        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("E-mail já cadastrado");
        }

        String passwordHash = request.password() != null
                ? passwordEncoder.encode(request.password())
                : null;

        User user = new User(
                company,
                request.name(),
                request.email(),
                passwordHash,
                request.role() != null ? request.role() : Role.USER
        );

        if (request.active() != null) {
            user.setActive(request.active());
        }

        user = userRepository.save(user);
        return toResponse(user);
    }

    @Transactional
    public UserResponse update(UUID id, UUID companyId, UserUpdateRequest request) {
        User user = userRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        if (request.name() != null) {
            user.setName(request.name());
        }

        if (request.email() != null && !request.email().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.email())) {
                throw new ConflictException("E-mail já cadastrado");
            }
            user.setEmail(request.email());
        }

        if (request.password() != null) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        if (request.role() != null) {
            user.setRole(request.role());
        }

        if (request.active() != null) {
            user.setActive(request.active());
        }

        user = userRepository.save(user);
        return toResponse(user);
    }

    @Transactional
    public void delete(UUID id, UUID companyId) {
        User user = userRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        userRepository.delete(user);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getActive(),
                user.getCompany().getId(),
                user.getCompany().getName(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
