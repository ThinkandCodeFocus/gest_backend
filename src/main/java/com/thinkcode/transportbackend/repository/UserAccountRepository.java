package com.thinkcode.transportbackend.repository;

import com.thinkcode.transportbackend.entity.UserAccount;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

    Optional<UserAccount> findByEmail(String email);

    Optional<UserAccount> findByCompanyIdAndEmail(UUID companyId, String email);

    Optional<UserAccount> findByPasswordResetToken(String passwordResetToken);

    java.util.List<UserAccount> findAllByCompanyIdOrderByFullNameAsc(UUID companyId);

    java.util.List<UserAccount> findAllByCompanyIdAndIdNotOrderByFullNameAsc(UUID companyId, UUID excludedUserId);
}

