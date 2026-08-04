package com.admin.service.impl;

import java.time.Instant;
import java.time.ZoneId;
import java.time.zone.ZoneRulesException;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.admin.dto.CreateTenantRequest;
import com.admin.dto.CreateTenantResponse;
import com.admin.dto.TenantListItemResponse;
import com.admin.dto.TenantPageResponse;
import com.admin.dto.TenantAccessResponse;
import com.admin.entity.SubscriptionPlan;
import com.admin.entity.SystemRole;
import com.admin.exception.TenantConflictException;
import com.admin.exception.TenantEmailAlreadyExistsException;
import com.admin.exception.TenantNotFoundException;
import com.admin.exception.TenantProvisioningException;
import com.admin.repository.TenantRepository;
import com.admin.security.PasswordPolicy;
import com.admin.security.PlatformAdminPrincipal;
import com.admin.security.TemporaryPasswordGenerator;
import com.admin.security.TenantCodeGenerator;
import com.admin.service.TenantCredentialEmailService;
import com.admin.service.TenantProvisioningService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantProvisioningServiceImpl implements TenantProvisioningService {

    private static final String TENANT_MANAGER_ROLE = "TENANT_MANAGER";
    private static final List<PolicyTemplate> DEFAULT_POLICIES = List.of(
            new PolicyTemplate("CUSTOMER_CONTACT", 730, false),
            new PolicyTemplate("ORDER_ADDRESS", 730, false),
            new PolicyTemplate("MESSAGE_CONTENT", 365, true),
            new PolicyTemplate("RAW_PAYLOAD", 90, false),
            new PolicyTemplate("WEBHOOK_PAYLOAD", 90, false),
            new PolicyTemplate("API_AUDIT", 180, false),
            new PolicyTemplate("AI_CONTEXT", 180, true),
            new PolicyTemplate("AI_OUTPUT", 365, true));

    private final TenantRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final TemporaryPasswordGenerator temporaryPasswordGenerator;
    private final TenantCodeGenerator tenantCodeGenerator;
    private final TenantCredentialEmailService credentialEmailService;

    public TenantProvisioningServiceImpl(
            TenantRepository repository,
            PasswordEncoder passwordEncoder,
            PasswordPolicy passwordPolicy,
            TemporaryPasswordGenerator temporaryPasswordGenerator,
            TenantCodeGenerator tenantCodeGenerator,
            TenantCredentialEmailService credentialEmailService
    ) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
        this.temporaryPasswordGenerator = temporaryPasswordGenerator;
        this.tenantCodeGenerator = tenantCodeGenerator;
        this.credentialEmailService = credentialEmailService;
    }

    @Override
    @Transactional
    public CreateTenantResponse createTenant(
            CreateTenantRequest request,
            PlatformAdminPrincipal admin,
            String ipAddress
    ) {
        String tenantCode = generateUniqueTenantCode();
        String planCode = request.subscriptionPlanCode().trim().toUpperCase(Locale.ROOT);
        String ownerEmail = request.ownerEmail().trim().toLowerCase(Locale.ROOT);
        String contactEmail = normalizeNullableEmail(request.contactEmail());
        String currency = validateCurrency(request.defaultCurrency());
        String timezone = validateTimezone(request.timezoneName());

        if (repository.ownerEmailExists(ownerEmail)) {
            throw new TenantEmailAlreadyExistsException(
                    "Email đăng nhập đã được sử dụng bởi một tài khoản tenant khác.");
        }

        SubscriptionPlan plan = repository.findActivePlan(planCode)
                .orElseThrow(() -> new TenantProvisioningException(
                        "Active subscription plan was not found"));
        SystemRole managerRole = repository.findSystemRole(TENANT_MANAGER_ROLE)
                .orElseThrow(() -> new TenantProvisioningException(
                        "TENANT_MANAGER system role was not seeded"));

        Instant now = Instant.now();
        Instant trialEndsAt = now.plusSeconds(request.trialDays() * 86_400L);
        String tenantId = UUID.randomUUID().toString();
        String subscriptionId = UUID.randomUUID().toString();
        String ownerUserId = UUID.randomUUID().toString();
        String temporaryPassword = temporaryPasswordGenerator.generate();
        passwordPolicy.validate(temporaryPassword);
        String passwordHash = passwordEncoder.encode(temporaryPassword);

        repository.insertTenant(
                tenantId,
                tenantCode,
                request.tenantName().trim(),
                trimToNull(request.legalName()),
                contactEmail,
                timezone,
                currency,
                admin.id(),
                now);
        repository.insertSubscription(
                subscriptionId,
                tenantId,
                plan.id(),
                now,
                trialEndsAt,
                admin.id());
        repository.insertTenantOwner(
                ownerUserId,
                tenantId,
                ownerEmail,
                request.ownerDisplayName().trim(),
                admin.id());
        repository.insertTenantCredential(ownerUserId, passwordHash);
        repository.assignRole(
                ownerUserId,
                tenantId,
                managerRole.id(),
                managerRole.scopeKey());

        for (PolicyTemplate policy : DEFAULT_POLICIES) {
            repository.insertDataProtectionPolicy(
                    UUID.randomUUID().toString(),
                    tenantId,
                    policy.category(),
                    policy.retentionDays(),
                    policy.allowAiProcessing());
        }

        repository.insertProvisioningAudit(
                tenantId,
                admin.id(),
                sanitizeIp(ipAddress),
                tenantCode,
                plan.code(),
                TENANT_MANAGER_ROLE);

        credentialEmailService.sendTemporaryPassword(
                ownerEmail,
                request.ownerDisplayName().trim(),
                request.tenantName().trim(),
                tenantCode,
                temporaryPassword);

        return new CreateTenantResponse(
                tenantId,
                tenantCode,
                "TRIAL",
                subscriptionId,
                "TRIAL",
                trialEndsAt,
                ownerUserId,
                ownerEmail,
                TENANT_MANAGER_ROLE,
                true,
                true);
    }

    @Override
    @Transactional(readOnly = true)
    public TenantPageResponse listTenants(String search, String status, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(100, Math.max(1, size));
        String normalizedSearch = search == null ? "" : search.trim();
        String normalizedStatus = normalizeStatus(status);
        long totalElements = repository.countTenantSummaries(normalizedSearch, normalizedStatus);
        int totalPages = totalElements == 0
                ? 0
                : (int) Math.ceil((double) totalElements / safeSize);

        var items = repository.findTenantSummaries(
                        normalizedSearch,
                        normalizedStatus,
                        safeSize,
                        safePage * safeSize)
                .stream()
                .map(item -> new TenantListItemResponse(
                        item.tenantId(),
                        item.tenantCode(),
                        item.tenantName(),
                        item.tenantStatus(),
                        item.contactEmail(),
                        item.timezoneName(),
                        item.defaultCurrency(),
                        item.ownerUserId(),
                        item.ownerEmail(),
                        item.ownerDisplayName(),
                        item.subscriptionPlanCode(),
                        item.subscriptionPlanName(),
                        item.subscriptionStatus(),
                        item.trialEndsAt(),
                        item.periodEndsAt(),
                        item.createdAt()))
                .toList();

        return new TenantPageResponse(items, totalElements, safePage, safeSize, totalPages);
    }

    @Override
    @Transactional
    public TenantAccessResponse setTenantLocked(
            String tenantId,
            boolean locked,
            PlatformAdminPrincipal admin,
            String ipAddress
    ) {
        var tenant = repository.findTenantAccessState(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("Không tìm thấy tài khoản tenant."));

        if ("CLOSED".equals(tenant.tenantStatus())) {
            throw new IllegalArgumentException("Tài khoản tenant đã đóng, không thể khóa hoặc mở khóa.");
        }

        String resultingStatus = locked
                ? "SUSPENDED"
                : restoreStatusFromSubscription(tenant.subscriptionStatus());
        Instant now = Instant.now();
        repository.updateTenantAccessStatus(tenantId, resultingStatus, now);
        int revokedSessions = locked ? repository.revokeTenantSessions(tenantId, now) : 0;
        repository.insertTenantAccessAudit(
                tenantId,
                admin.id(),
                sanitizeIp(ipAddress),
                locked,
                resultingStatus,
                revokedSessions);
        return new TenantAccessResponse(
                tenantId,
                resultingStatus,
                locked,
                revokedSessions);
    }

    private String generateUniqueTenantCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String candidate = tenantCodeGenerator.generate();
            if (!repository.tenantCodeExists(candidate)) {
                return candidate;
            }
        }
        throw new TenantConflictException("Không thể tạo mã tenant duy nhất. Vui lòng thử lại.");
    }

    private String restoreStatusFromSubscription(String subscriptionStatus) {
        return switch (subscriptionStatus) {
            case "TRIAL" -> "TRIAL";
            case "ACTIVE" -> "ACTIVE";
            default -> throw new IllegalArgumentException(
                    "Gói thuê hiện tại không ở trạng thái cho phép mở tài khoản tenant.");
        };
    }

    private String validateTimezone(String value) {
        String timezone = value.trim();
        try {
            ZoneId.of(timezone);
            return timezone;
        } catch (ZoneRulesException exception) {
            throw new IllegalArgumentException("Invalid timezone name");
        }
    }

    private String validateCurrency(String value) {
        String currency = value.trim().toUpperCase(Locale.ROOT);
        try {
            Currency.getInstance(currency);
            return currency;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid ISO-4217 currency code");
        }
    }

    private String normalizeNullableEmail(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String normalizeStatus(String value) {
        if (value == null || value.isBlank() || "ALL".equalsIgnoreCase(value)) {
            return "";
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!List.of("TRIAL", "ACTIVE", "SUSPENDED", "CLOSED").contains(normalized)) {
            throw new IllegalArgumentException("Invalid tenant status");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String sanitizeIp(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.substring(0, Math.min(value.length(), 45));
    }

    private record PolicyTemplate(String category, int retentionDays, boolean allowAiProcessing) {
    }
}
