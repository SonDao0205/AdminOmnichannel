package com.admin.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.admin.entity.SubscriptionPlan;
import com.admin.entity.SystemRole;
import com.admin.entity.TenantSummary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TenantRepository {

    private final JdbcTemplate jdbcTemplate;

    public TenantRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean tenantCodeExists(String tenantCode) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tenants WHERE tenant_code = ?",
                Integer.class,
                tenantCode);
        return count != null && count > 0;
    }

    public Optional<SubscriptionPlan> findActivePlan(String planCode) {
        return jdbcTemplate.query(
                        """
                        SELECT id, plan_code
                        FROM subscription_plans
                        WHERE plan_code = ? AND status = 'ACTIVE'
                        LIMIT 1
                        """,
                        (resultSet, rowNumber) -> new SubscriptionPlan(
                                resultSet.getString("id"),
                                resultSet.getString("plan_code")),
                        planCode)
                .stream()
                .findFirst();
    }

    public Optional<SystemRole> findSystemRole(String roleCode) {
        return jdbcTemplate.query(
                        """
                        SELECT id, tenant_scope_key
                        FROM roles
                        WHERE role_code = ?
                          AND tenant_id IS NULL
                          AND is_system = 1
                        LIMIT 1
                        """,
                        (resultSet, rowNumber) -> new SystemRole(
                                resultSet.getString("id"),
                                resultSet.getString("tenant_scope_key")),
                        roleCode)
                .stream()
                .findFirst();
    }

    public List<TenantSummary> findTenantSummaries(
            String search,
            String status,
            int limit,
            int offset
    ) {
        String searchPattern = "%" + search + "%";
        return jdbcTemplate.query(
                """
                SELECT
                    t.id AS tenant_id,
                    t.tenant_code,
                    t.tenant_name,
                    t.status AS tenant_status,
                    t.contact_email,
                    t.timezone_name,
                    t.default_currency,
                    t.created_at,
                    owner_user.id AS owner_user_id,
                    owner_user.email AS owner_email,
                    owner_user.display_name AS owner_display_name,
                    sp.plan_code,
                    sp.plan_name,
                    ts.status AS subscription_status,
                    ts.trial_ends_at,
                    ts.current_period_ends_at
                FROM tenants t
                LEFT JOIN tenant_subscriptions ts
                    ON ts.id = (
                        SELECT ts2.id
                        FROM tenant_subscriptions ts2
                        WHERE ts2.tenant_id = t.id
                        ORDER BY ts2.created_at DESC
                        LIMIT 1
                    )
                LEFT JOIN subscription_plans sp ON sp.id = ts.subscription_plan_id
                LEFT JOIN tenant_users owner_user
                    ON owner_user.id = (
                        SELECT tu.id
                        FROM tenant_users tu
                        WHERE tu.tenant_id = t.id
                          AND tu.deleted_at IS NULL
                        ORDER BY tu.created_at ASC
                        LIMIT 1
                    )
                WHERE t.deleted_at IS NULL
                  AND (? = '' OR t.tenant_code LIKE ? OR t.tenant_name LIKE ?
                       OR owner_user.email LIKE ?)
                  AND (? = '' OR t.status = ?)
                ORDER BY t.created_at DESC
                LIMIT ? OFFSET ?
                """,
                (resultSet, rowNumber) -> new TenantSummary(
                        resultSet.getString("tenant_id"),
                        resultSet.getString("tenant_code"),
                        resultSet.getString("tenant_name"),
                        resultSet.getString("tenant_status"),
                        resultSet.getString("contact_email"),
                        resultSet.getString("timezone_name"),
                        resultSet.getString("default_currency"),
                        resultSet.getString("owner_user_id"),
                        resultSet.getString("owner_email"),
                        resultSet.getString("owner_display_name"),
                        resultSet.getString("plan_code"),
                        resultSet.getString("plan_name"),
                        resultSet.getString("subscription_status"),
                        toInstant(resultSet.getTimestamp("trial_ends_at")),
                        toInstant(resultSet.getTimestamp("current_period_ends_at")),
                        toInstant(resultSet.getTimestamp("created_at"))),
                search,
                searchPattern,
                searchPattern,
                searchPattern,
                status,
                status,
                limit,
                offset);
    }

    public long countTenantSummaries(String search, String status) {
        String searchPattern = "%" + search + "%";
        Long count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM tenants t
                LEFT JOIN tenant_users owner_user
                    ON owner_user.id = (
                        SELECT tu.id
                        FROM tenant_users tu
                        WHERE tu.tenant_id = t.id
                          AND tu.deleted_at IS NULL
                        ORDER BY tu.created_at ASC
                        LIMIT 1
                    )
                WHERE t.deleted_at IS NULL
                  AND (? = '' OR t.tenant_code LIKE ? OR t.tenant_name LIKE ?
                       OR owner_user.email LIKE ?)
                  AND (? = '' OR t.status = ?)
                """,
                Long.class,
                search,
                searchPattern,
                searchPattern,
                searchPattern,
                status,
                status);
        return count == null ? 0L : count;
    }

    public void insertTenant(
            String tenantId,
            String tenantCode,
            String tenantName,
            String legalName,
            String contactEmail,
            String timezone,
            String currency,
            String adminId,
            Instant now
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO tenants (
                    id, tenant_code, tenant_name, legal_name, contact_email,
                    status, timezone_name, default_currency, settings_json,
                    provisioned_by_admin_id, provisioned_at
                ) VALUES (
                    ?, ?, ?, ?, ?, 'TRIAL', ?, ?,
                    JSON_OBJECT('first_login_password_change_required', TRUE),
                    ?, ?
                )
                """,
                tenantId,
                tenantCode,
                tenantName,
                legalName,
                contactEmail,
                timezone,
                currency,
                adminId,
                Timestamp.from(now));
    }

    public void insertSubscription(
            String subscriptionId,
            String tenantId,
            String planId,
            Instant startsAt,
            Instant trialEndsAt,
            String adminId
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO tenant_subscriptions (
                    id, tenant_id, subscription_plan_id, status,
                    starts_at, trial_ends_at, current_period_ends_at,
                    created_by_admin_id
                ) VALUES (?, ?, ?, 'TRIAL', ?, ?, ?, ?)
                """,
                subscriptionId,
                tenantId,
                planId,
                Timestamp.from(startsAt),
                Timestamp.from(trialEndsAt),
                Timestamp.from(trialEndsAt),
                adminId);
    }

    public void insertTenantOwner(
            String ownerUserId,
            String tenantId,
            String ownerEmail,
            String ownerDisplayName,
            String adminId
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO tenant_users (
                    id, tenant_id, email, display_name, status,
                    provisioned_by_admin_id, provisioned_by_user_id
                ) VALUES (?, ?, ?, ?, 'ACTIVE', ?, NULL)
                """,
                ownerUserId,
                tenantId,
                ownerEmail,
                ownerDisplayName,
                adminId);
    }

    public void insertTenantCredential(String ownerUserId, String passwordHash) {
        jdbcTemplate.update(
                """
                INSERT INTO tenant_user_credentials (
                    tenant_user_id, password_hash, password_algorithm,
                    must_change_password, credential_version
                ) VALUES (?, ?, 'ARGON2ID', 1, 1)
                """,
                ownerUserId,
                passwordHash);
    }

    public void assignRole(
            String ownerUserId,
            String tenantId,
            String roleId,
            String scopeKey
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO tenant_user_roles (
                    tenant_user_id, tenant_id, role_id, role_scope_key, assigned_by_user_id
                ) VALUES (?, ?, ?, ?, NULL)
                """,
                ownerUserId,
                tenantId,
                roleId,
                scopeKey);
    }

    public void insertDataProtectionPolicy(
            String policyId,
            String tenantId,
            String category,
            int retentionDays,
            boolean allowAiProcessing
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO data_protection_policies (
                    id, tenant_id, data_category, retention_days,
                    encrypt_at_rest, redact_in_logs, allow_ai_processing,
                    purge_enabled, policy_version
                ) VALUES (?, ?, ?, ?, 1, 1, ?, 1, 'v1')
                """,
                policyId,
                tenantId,
                category,
                retentionDays,
                allowAiProcessing ? 1 : 0);
    }

    public void insertProvisioningAudit(
            String tenantId,
            String adminId,
            String ipAddress,
            String tenantCode,
            String planCode,
            String roleCode
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO security_audit_logs (
                    tenant_id, actor_type, actor_id, action_code,
                    target_type, target_id, result, ip_address, metadata_json
                ) VALUES (
                    ?, 'PLATFORM_ADMIN', ?, 'TENANT_PROVISION', 'TENANT', ?,
                    'SUCCEEDED', ?, JSON_OBJECT(
                        'tenant_code', ?,
                        'subscription_plan', ?,
                        'owner_role', ?
                    )
                )
                """,
                tenantId,
                adminId,
                tenantId,
                ipAddress,
                tenantCode,
                planCode,
                roleCode);
    }

    private Instant toInstant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }
}
