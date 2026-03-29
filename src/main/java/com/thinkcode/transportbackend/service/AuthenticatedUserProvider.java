package com.thinkcode.transportbackend.service;

import com.thinkcode.transportbackend.entity.Company;
import com.thinkcode.transportbackend.entity.UserAccount;
import com.thinkcode.transportbackend.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedUserProvider {

    private final UserAccountRepository userAccountRepository;

    public AuthenticatedUserProvider(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    public UserAccount requireUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        String email = authentication.getName();
        return userAccountRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
    }

    public Company requireCompany() {
        UserAccount user = requireUser();
        Company company = user.getCompany();
        if (company == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "User company not found");
        }
        return company;
    }
}
