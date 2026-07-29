package com.admin.service.impl;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import com.admin.config.AdminSecurityProperties;
import com.admin.entity.AdminLoginResult;
import com.admin.entity.PlatformAdmin;
import com.admin.repository.PlatformAdminRepository;
import com.admin.security.SecureTokenService;
import com.admin.service.AdminAuthenticationService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class AdminAuthenticationServiceImpl implements AdminAuthenticationService {

    private final PlatformAdminRepository repository;
    private final TransactionTemplate transactionTemplate;
    private final PasswordEncoder passwordEncoder;
    private final SecureTokenService tokenService;
    private final AdminSecurityProperties properties;
    private final String dummyPasswordHash;

    public AdminAuthenticationServiceImpl(
            PlatformAdminRepository repository,
            TransactionTemplate transactionTemplate,
            PasswordEncoder passwordEncoder,
            SecureTokenService tokenService,
            AdminSecurityProperties properties
    ) {
        this.repository = repository;
        this.transactionTemplate = transactionTemplate;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.properties = properties;
        this.dummyPasswordHash = passwordEncoder.encode("Dummy-password-which-is-never-valid-7!");
    }

    @Override
    public AdminLoginResult login(String email, String password, String ipAddress, String userAgent) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        return transactionTemplate.execute(status ->
                doLogin(normalizedEmail, password, sanitizeIp(ipAddress), sanitizeUserAgent(userAgent)));
    }

    @Override
    public void logout(String sessionId, String adminId, String ipAddress) {
        transactionTemplate.executeWithoutResult(status -> {
            boolean revoked = repository.revokeSession(sessionId, adminId, Instant.now());
            if (revoked) {
                audit(adminId, "ADMIN_LOGOUT", adminId, "SUCCEEDED", sanitizeIp(ipAddress), "LOGOUT");
            }
        });
    }

    private AdminLoginResult doLogin(
            String normalizedEmail,
            String password,
            String ipAddress,
            String userAgent
    ) {
        Instant now = Instant.now();
        var found = repository.findByEmailForUpdate(normalizedEmail);

        if (found.isEmpty()) {
            safePasswordMatches(password, dummyPasswordHash);
            audit(null, "ADMIN_LOGIN", null, "FAILED", ipAddress, "INVALID_CREDENTIALS");
            return AdminLoginResult.failed();
        }

        PlatformAdmin admin = found.get();
        boolean temporarilyLocked =
                "LOCKED".equals(admin.status())
                        && (admin.lockedUntil() == null || now.isBefore(admin.lockedUntil()));
        if ("DISABLED".equals(admin.status()) || temporarilyLocked) {
            safePasswordMatches(password, admin.passwordHash());
            audit(admin.id(), "ADMIN_LOGIN", admin.id(), "DENIED", ipAddress, "ACCOUNT_UNAVAILABLE");
            return AdminLoginResult.failed();
        }

        if ("LOCKED".equals(admin.status()) && admin.lockedUntil() != null) {
            repository.reactivateAfterTemporaryLock(admin.id());
            admin = new PlatformAdmin(
                    admin.id(),
                    admin.email(),
                    admin.passwordHash(),
                    admin.displayName(),
                    "ACTIVE",
                    0,
                    null);
        }

        if (!safePasswordMatches(password, admin.passwordHash())) {
            int failureCount = admin.failedLoginCount() + 1;
            if (failureCount >= properties.getMaxFailedLogins()) {
                repository.lockAfterFailedLogin(
                        admin.id(),
                        failureCount,
                        now.plus(properties.getLockDuration()));
            } else {
                repository.recordFailedLogin(admin.id(), failureCount);
            }
            audit(admin.id(), "ADMIN_LOGIN", admin.id(), "FAILED", ipAddress, "INVALID_CREDENTIALS");
            return AdminLoginResult.failed();
        }

        repository.recordSuccessfulLogin(admin.id(), now);
        repository.revokeActiveSessions(admin.id(), now);

        String rawToken = tokenService.newOpaqueToken();
        String sessionId = UUID.randomUUID().toString();
        Instant expiresAt = now.plus(properties.getSessionTtl());
        repository.createSession(
                sessionId,
                admin.id(),
                tokenService.sha256(rawToken),
                ipAddress,
                userAgent,
                now,
                expiresAt);

        audit(admin.id(), "ADMIN_LOGIN", admin.id(), "SUCCEEDED", ipAddress, "PASSWORD");
        return AdminLoginResult.succeeded(
                rawToken,
                expiresAt,
                admin.id(),
                admin.email(),
                admin.displayName());
    }

    private boolean safePasswordMatches(String rawPassword, String encodedPassword) {
        try {
            return passwordEncoder.matches(rawPassword, encodedPassword);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private void audit(
            String actorId,
            String action,
            String targetId,
            String result,
            String ipAddress,
            String reason
    ) {
        repository.insertAudit(
                actorId,
                action,
                "PLATFORM_ADMIN",
                targetId,
                result,
                ipAddress,
                reason);
    }

    private String sanitizeIp(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.substring(0, Math.min(value.length(), 45));
    }

    private String sanitizeUserAgent(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.substring(0, Math.min(value.length(), 500));
    }
}
