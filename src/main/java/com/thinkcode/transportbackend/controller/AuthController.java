package com.thinkcode.transportbackend.controller;

import com.thinkcode.transportbackend.repository.UserAccountRepository;
import com.thinkcode.transportbackend.security.JwtService;
import com.thinkcode.transportbackend.service.ApiException;
import com.thinkcode.transportbackend.dto.AuthRequest;
import com.thinkcode.transportbackend.dto.AuthResponse;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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

    public AuthController(
            AuthenticationManager authenticationManager,
            UserAccountRepository userAccountRepository,
            JwtService jwtService
    ) {
        this.authenticationManager = authenticationManager;
        this.userAccountRepository = userAccountRepository;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        var user = userAccountRepository.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
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
                user.getCompany().getId()
        );
    }
}

