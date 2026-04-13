package com.thinkcode.transportbackend.controller;

import com.thinkcode.transportbackend.dto.AuthRequest;
import com.thinkcode.transportbackend.dto.AuthMeResponse;
import com.thinkcode.transportbackend.dto.AuthResponse;
import com.thinkcode.transportbackend.dto.PasswordResetConfirmRequest;
import com.thinkcode.transportbackend.dto.PasswordResetRequest;
import com.thinkcode.transportbackend.entity.UserAccount;
import com.thinkcode.transportbackend.repository.UserAccountRepository;
import com.thinkcode.transportbackend.security.JwtService;
import com.thinkcode.transportbackend.service.ApiException;
import com.thinkcode.transportbackend.service.AuthenticatedUserProvider;
import com.thinkcode.transportbackend.service.TransactionalEmailService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserAccountRepository userAccountRepository;
    private final JwtService jwtService;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final PasswordEncoder passwordEncoder;
    private final TransactionalEmailService transactionalEmailService;

    public AuthController(
            AuthenticationManager authenticationManager,
            UserAccountRepository userAccountRepository,
            JwtService jwtService,
            AuthenticatedUserProvider authenticatedUserProvider,
            PasswordEncoder passwordEncoder,
            TransactionalEmailService transactionalEmailService
    ) {
        this.authenticationManager = authenticationManager;
        this.userAccountRepository = userAccountRepository;
        this.jwtService = jwtService;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.passwordEncoder = passwordEncoder;
        this.transactionalEmailService = transactionalEmailService;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        var user = userAccountRepository.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        user.setLastLoginAt(Instant.now());
        userAccountRepository.save(user);
        String token = jwtService.generateToken(user.getEmail(), Map.of(
                "role", user.getRole().name(),
                "companyId", user.getCompany().getId().toString()
        ));
        return new AuthResponse(
                token,
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getCompany().getId(),
                user.getLastLoginAt(),
                user.isPasswordChangeRequired()
        );
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public AuthMeResponse me() {
        var user = authenticatedUserProvider.requireUser();
        return new AuthMeResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getCompany().getId(),
                user.getLastLoginAt(),
                user.isPasswordChangeRequired()
        );
    }

    @PostMapping("/password-reset/request")
    public Map<String, String> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        UserAccount user = userAccountRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Aucun compte n'est associe a cette adresse email."));
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        user.setPasswordResetToken(token);
        user.setPasswordResetTokenExpiresAt(Instant.now().plusSeconds(900));
        userAccountRepository.save(user);
        transactionalEmailService.sendPasswordResetEmail(user, token);
        return Map.of("message", "Un code de confirmation vient d'etre envoye a votre adresse email.");
    }

    @PostMapping("/password-reset/confirm")
    public Map<String, String> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        UserAccount user = userAccountRepository.findByPasswordResetToken(request.token().trim())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Le code de confirmation est invalide."));
        if (user.getPasswordResetTokenExpiresAt() == null || user.getPasswordResetTokenExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Le code de confirmation a expire. Veuillez en demander un nouveau.");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setPasswordChangeRequired(false);
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiresAt(null);
        userAccountRepository.save(user);
        return Map.of("message", "Votre mot de passe a bien ete mis a jour.");
    }
}

