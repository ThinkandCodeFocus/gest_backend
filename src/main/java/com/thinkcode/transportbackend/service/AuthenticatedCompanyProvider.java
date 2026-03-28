package com.thinkcode.transportbackend.service;

import com.thinkcode.transportbackend.repository.UserAccountRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedCompanyProvider {

    private final UserAccountRepository userAccountRepository;

    public AuthenticatedCompanyProvider(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    public UUID requireCompanyId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        String email = authentication.getName();
        return userAccountRepository.findByEmail(email.toLowerCase())
                .map(user -> user.getCompany().getId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
    }
}
