package com.flowcrm.auth;

import com.flowcrm.auth.dto.*;
import com.flowcrm.company.Company;
import com.flowcrm.company.CompanyRepository;
import com.flowcrm.shared.exception.ConflictException;
import com.flowcrm.shared.exception.ResourceNotFoundException;
import com.flowcrm.shared.exception.UnauthorizedException;
import com.flowcrm.shared.security.JwtService;
import com.flowcrm.user.Role;
import com.flowcrm.user.User;
import com.flowcrm.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("E-mail já cadastrado");
        }

        if (companyRepository.existsBySlug(request.companySlug())) {
            throw new ConflictException("Slug da empresa já está em uso");
        }

        Company company = new Company(request.companyName(), request.companySlug());
        company = companyRepository.save(company);

        String passwordHash = passwordEncoder.encode(request.password());
        User user = new User(company, request.name(), request.email(), passwordHash, Role.ADMIN);
        user = userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(
                user.getId(),
                company.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        String refreshToken = jwtService.generateRefreshToken(user.getId());

        return new AuthResponse(
                accessToken,
                refreshToken,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                company.getId(),
                company.getName()
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException("Credenciais inválidas"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Credenciais inválidas");
        }

        if (!user.getActive()) {
            throw new UnauthorizedException("Usuário desativado");
        }

        Company company = user.getCompany();

        String accessToken = jwtService.generateAccessToken(
                user.getId(),
                company.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        String refreshToken = jwtService.generateRefreshToken(user.getId());

        return new AuthResponse(
                accessToken,
                refreshToken,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                company.getId(),
                company.getName()
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshRequest request) {
        String refreshToken = request.refreshToken();

        if (!jwtService.isTokenValid(refreshToken)) {
            throw new UnauthorizedException("Refresh token inválido ou expirado");
        }

        UUID userId = jwtService.extractUserId(refreshToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        if (!user.getActive()) {
            throw new UnauthorizedException("Usuário desativado");
        }

        Company company = user.getCompany();

        String accessToken = jwtService.generateAccessToken(
                user.getId(),
                company.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        String newRefreshToken = jwtService.generateRefreshToken(user.getId());

        return new AuthResponse(
                accessToken,
                newRefreshToken,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                company.getId(),
                company.getName()
        );
    }

    public void logout() {
    }

    @Transactional(readOnly = true)
    public UserResponse getMe(UUID userId, UUID companyId) {
        User user = userRepository.findByIdAndCompanyId(userId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getActive(),
                user.getCompany().getId(),
                user.getCompany().getName(),
                user.getCreatedAt()
        );
    }
}
