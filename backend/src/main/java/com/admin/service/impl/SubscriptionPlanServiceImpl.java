package com.admin.service.impl;

import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.admin.dto.SubscriptionPlanPageResponse;
import com.admin.dto.SubscriptionPlanRequest;
import com.admin.dto.SubscriptionPlanResponse;
import com.admin.entity.SubscriptionPlanDetails;
import com.admin.exception.SubscriptionPlanConflictException;
import com.admin.exception.SubscriptionPlanNotFoundException;
import com.admin.repository.PlatformAdminRepository;
import com.admin.repository.SubscriptionPlanRepository;
import com.admin.security.PlatformAdminPrincipal;
import com.admin.service.SubscriptionPlanService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
public class SubscriptionPlanServiceImpl implements SubscriptionPlanService {

    private static final int MAX_JSON_LENGTH = 65_535;
    private static final List<String> STATUSES = List.of("ACTIVE", "INACTIVE", "ARCHIVED");
    private static final List<String> BILLING_PERIODS =
            List.of("MONTHLY", "QUARTERLY", "YEARLY", "CUSTOM");
    private static final TypeReference<Map<String, Object>> JSON_MAP_TYPE =
            new TypeReference<>() {
            };

    private final SubscriptionPlanRepository planRepository;
    private final PlatformAdminRepository adminRepository;
    private final ObjectMapper objectMapper;

    public SubscriptionPlanServiceImpl(
            SubscriptionPlanRepository planRepository,
            PlatformAdminRepository adminRepository,
            ObjectMapper objectMapper
    ) {
        this.planRepository = planRepository;
        this.adminRepository = adminRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public SubscriptionPlanResponse create(
            SubscriptionPlanRequest request,
            PlatformAdminPrincipal admin,
            String ipAddress
    ) {
        NormalizedPlan plan = normalize(request);
        if (planRepository.planCodeExists(plan.code())) {
            throw new SubscriptionPlanConflictException("Subscription plan code already exists");
        }

        String planId = UUID.randomUUID().toString();
        planRepository.insert(
                planId,
                plan.code(),
                plan.name(),
                plan.billingPeriod(),
                request.priceAmount(),
                plan.currency(),
                plan.limitsJson(),
                plan.featuresJson(),
                plan.status());
        audit(admin, "SUBSCRIPTION_PLAN_CREATE", planId, ipAddress, plan.code());
        return get(planId);
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionPlanPageResponse list(
            String search,
            String status,
            int page,
            int size
    ) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(100, Math.max(1, size));
        String normalizedSearch = search == null ? "" : search.trim();
        String normalizedStatus = normalizeFilterStatus(status);
        long totalElements = planRepository.countAll(normalizedSearch, normalizedStatus);
        int totalPages = totalElements == 0
                ? 0
                : (int) Math.ceil((double) totalElements / safeSize);
        var items = planRepository.findAll(
                        normalizedSearch,
                        normalizedStatus,
                        safeSize,
                        safePage * safeSize)
                .stream()
                .map(this::toResponse)
                .toList();
        return new SubscriptionPlanPageResponse(
                items,
                totalElements,
                safePage,
                safeSize,
                totalPages);
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionPlanResponse get(String planId) {
        return toResponse(findPlan(planId));
    }

    @Override
    @Transactional
    public SubscriptionPlanResponse update(
            String planId,
            SubscriptionPlanRequest request,
            PlatformAdminPrincipal admin,
            String ipAddress
    ) {
        findPlan(planId);
        NormalizedPlan plan = normalize(request);
        if (planRepository.planCodeExistsForAnotherPlan(plan.code(), planId)) {
            throw new SubscriptionPlanConflictException("Subscription plan code already exists");
        }
        if (!planRepository.update(
                planId,
                plan.code(),
                plan.name(),
                plan.billingPeriod(),
                request.priceAmount(),
                plan.currency(),
                plan.limitsJson(),
                plan.featuresJson(),
                plan.status())) {
            throw notFound(planId);
        }
        audit(admin, "SUBSCRIPTION_PLAN_UPDATE", planId, ipAddress, plan.code());
        return get(planId);
    }

    @Override
    @Transactional
    public SubscriptionPlanResponse updateStatus(
            String planId,
            String status,
            PlatformAdminPrincipal admin,
            String ipAddress
    ) {
        SubscriptionPlanDetails existing = findPlan(planId);
        String normalizedStatus = normalizeStatus(status);
        if (!planRepository.updateStatus(planId, normalizedStatus)) {
            throw notFound(planId);
        }
        audit(
                admin,
                "SUBSCRIPTION_PLAN_STATUS_UPDATE",
                planId,
                ipAddress,
                existing.code() + ":" + normalizedStatus);
        return get(planId);
    }

    private SubscriptionPlanDetails findPlan(String planId) {
        if (planId == null || planId.isBlank()) {
            throw new IllegalArgumentException("Subscription plan id is required");
        }
        return planRepository.findById(planId).orElseThrow(() -> notFound(planId));
    }

    private SubscriptionPlanNotFoundException notFound(String planId) {
        return new SubscriptionPlanNotFoundException(
                "Subscription plan was not found: " + planId);
    }

    private NormalizedPlan normalize(SubscriptionPlanRequest request) {
        String code = request.planCode().trim().toUpperCase(Locale.ROOT);
        String name = request.planName().trim();
        String billingPeriod = request.billingPeriod().trim().toUpperCase(Locale.ROOT);
        String currency = request.currency().trim().toUpperCase(Locale.ROOT);
        String status = normalizeStatus(request.status());

        if (!BILLING_PERIODS.contains(billingPeriod)) {
            throw new IllegalArgumentException("Invalid billing period");
        }
        try {
            Currency.getInstance(currency);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid ISO-4217 currency code");
        }

        String limitsJson = serializeConfig(request.limits(), "limits");
        String featuresJson = serializeConfig(request.features(), "features");
        return new NormalizedPlan(
                code,
                name,
                billingPeriod,
                currency,
                limitsJson,
                featuresJson,
                status);
    }

    private String normalizeFilterStatus(String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
            return "";
        }
        return normalizeStatus(status);
    }

    private String normalizeStatus(String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (!STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("Invalid subscription plan status");
        }
        return normalized;
    }

    private String serializeConfig(Map<String, Object> value, String fieldName) {
        try {
            String json = objectMapper.writeValueAsString(value);
            if (json.length() > MAX_JSON_LENGTH) {
                throw new IllegalArgumentException(fieldName + " configuration is too large");
            }
            return json;
        } catch (JacksonException exception) {
            throw new IllegalArgumentException(fieldName + " configuration is not valid JSON");
        }
    }

    private Map<String, Object> deserializeConfig(String value) {
        if (value == null || value.isBlank()) {
            return Map.of();
        }
        try {
            Object decoded = objectMapper.readValue(value, Object.class);
            Map<String, Object> result;
            if (decoded instanceof String nestedJson) {
                result = objectMapper.readValue(nestedJson, JSON_MAP_TYPE);
            } else if (decoded instanceof Map<?, ?> map) {
                result = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (!(entry.getKey() instanceof String key)) {
                        throw new IllegalStateException(
                                "Stored subscription plan JSON contains a non-string key");
                    }
                    result.put(key, entry.getValue());
                }
            } else {
                throw new IllegalStateException(
                        "Stored subscription plan JSON must be an object");
            }
            return result == null ? Map.of() : new LinkedHashMap<>(result);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Stored subscription plan JSON is invalid", exception);
        }
    }

    private SubscriptionPlanResponse toResponse(SubscriptionPlanDetails plan) {
        return new SubscriptionPlanResponse(
                plan.id(),
                plan.code(),
                plan.name(),
                plan.billingPeriod(),
                plan.priceAmount(),
                plan.currency(),
                deserializeConfig(plan.limitsJson()),
                deserializeConfig(plan.featuresJson()),
                plan.status(),
                plan.createdAt(),
                plan.updatedAt(),
                plan.activeTenantsCount());
    }

    private void audit(
            PlatformAdminPrincipal admin,
            String action,
            String planId,
            String ipAddress,
            String reason
    ) {
        adminRepository.insertAudit(
                admin.id(),
                action,
                "SUBSCRIPTION_PLAN",
                planId,
                "SUCCEEDED",
                sanitizeIp(ipAddress),
                reason);
    }

    private String sanitizeIp(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.substring(0, Math.min(value.length(), 45));
    }

    private record NormalizedPlan(
            String code,
            String name,
            String billingPeriod,
            String currency,
            String limitsJson,
            String featuresJson,
            String status
    ) {
    }
}
