package com.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Map;

import jakarta.servlet.http.Cookie;
import jakarta.validation.Validator;

import com.admin.dto.CreateTenantRequest;
import com.admin.dto.SubscriptionPlanRequest;
import com.admin.exception.SubscriptionPlanConflictException;
import com.admin.exception.SubscriptionPlanNotFoundException;
import com.admin.exception.TenantConflictException;
import com.admin.exception.TenantEmailDeliveryException;
import com.admin.security.PlatformAdminPrincipal;
import com.admin.service.AdminAuthenticationService;
import com.admin.service.SubscriptionPlanService;
import com.admin.service.TenantProvisioningService;
import com.admin.service.TenantCredentialEmailService;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@AutoConfigureMockMvc
class AdminSecurityAndProvisioningIntegrationTests {

    private static final String ADMIN_ID = "00000000-0000-0000-0000-000000000001";
    private static final String SYSTEM_SCOPE = "00000000-0000-0000-0000-000000000000";

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    AdminAuthenticationService authenticationService;

    @Autowired
    TenantProvisioningService provisioningService;

    @Autowired
    SubscriptionPlanService planService;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    Validator validator;

    @MockitoBean
    TenantCredentialEmailService credentialEmailService;

    @BeforeEach
    void resetDatabase() {
        jdbcTemplate.update("DELETE FROM security_audit_logs");
        jdbcTemplate.update("DELETE FROM login_sessions");
        jdbcTemplate.update("DELETE FROM data_protection_policies");
        jdbcTemplate.update("DELETE FROM tenant_user_roles");
        jdbcTemplate.update("DELETE FROM tenant_user_credentials");
        jdbcTemplate.update("DELETE FROM tenant_users");
        jdbcTemplate.update("DELETE FROM tenant_subscriptions");
        jdbcTemplate.update("DELETE FROM tenants");
        jdbcTemplate.update("DELETE FROM roles");
        jdbcTemplate.update("DELETE FROM subscription_plans");
        jdbcTemplate.update("DELETE FROM platform_admins");

        jdbcTemplate.update(
                """
                INSERT INTO platform_admins (
                    id, email, password_hash, password_algorithm,
                    display_name, status, failed_login_count
                ) VALUES (?, 'owner@example.com', ?, 'ARGON2ID', 'Owner', 'ACTIVE', 0)
                """,
                ADMIN_ID,
                passwordEncoder.encode("Strong-owner-2026!"));
        jdbcTemplate.update(
                """
                INSERT INTO subscription_plans (
                    id, plan_code, plan_name, billing_period, price_amount,
                    currency, limits_json, features_json, status
                ) VALUES (?, 'DEMO', 'Demo plan', 'MONTHLY', 0, 'VND', '{}', '{}', 'ACTIVE')
                """,
                "10000000-0000-0000-0000-000000000001");
        jdbcTemplate.update(
                """
                INSERT INTO roles (
                    id, tenant_id, tenant_scope_key, role_code, is_system
                ) VALUES (?, NULL, ?, 'TENANT_MANAGER', TRUE)
                """,
                "40000000-0000-0000-0000-000000000001",
                SYSTEM_SCOPE);
    }

    @Test
    void loginStoresOnlyHashedOpaqueSessionAndRevokesPreviousSession() {
        var first = authenticationService.login(
                "OWNER@EXAMPLE.COM",
                "Strong-owner-2026!",
                "127.0.0.1",
                "JUnit");
        assertTrue(first.success());

        String storedHash = jdbcTemplate.queryForObject(
                "SELECT session_token_hash FROM login_sessions WHERE revoked_at IS NULL",
                String.class);
        assertNotEquals(first.rawToken(), storedHash);
        assertEquals(64, storedHash.length());

        var second = authenticationService.login(
                "owner@example.com",
                "Strong-owner-2026!",
                "127.0.0.1",
                "JUnit");
        assertTrue(second.success());
        assertEquals(
                1,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM login_sessions WHERE revoked_at IS NULL",
                        Integer.class));
    }

    @Test
    void httpLoginRequiresCsrfAndAuthenticatesWithHttpOnlyCookie() throws Exception {
        mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "owner@example.com",
                                  "password": "Strong-owner-2026!"
                                }
                                """))
                .andExpect(status().isForbidden());

        var csrfResponse = mockMvc.perform(get("/api/admin/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andReturn()
                .getResponse();
        Cookie csrfCookie = csrfResponse.getCookie("XSRF-TOKEN");
        String responseBody = csrfResponse.getContentAsString();
        String csrfToken = responseBody.replaceFirst(".*\"token\":\"([^\"]+)\".*", "$1");

        var loginResponse = mockMvc.perform(post("/api/admin/auth/login")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "owner@example.com",
                                  "password": "Strong-owner-2026!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(cookie().httpOnly("omni_admin_session", true))
                .andExpect(jsonPath("$.actorType").value("PLATFORM_ADMIN"))
                .andReturn()
                .getResponse();

        Cookie sessionCookie = loginResponse.getCookie("omni_admin_session");
        mockMvc.perform(get("/api/admin/auth/me").cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("owner@example.com"));
    }

    @Test
    void planApiRequiresAdminSessionAndCreatesThenListsPlan() throws Exception {
        mockMvc.perform(get("/api/admin/plans"))
                .andExpect(status().isUnauthorized());

        var csrfResponse = mockMvc.perform(get("/api/admin/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();
        Cookie csrfCookie = csrfResponse.getCookie("XSRF-TOKEN");
        String csrfToken = csrfResponse.getContentAsString()
                .replaceFirst(".*\"token\":\"([^\"]+)\".*", "$1");

        var loginResponse = mockMvc.perform(post("/api/admin/auth/login")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "owner@example.com",
                                  "password": "Strong-owner-2026!"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();
        Cookie sessionCookie = loginResponse.getCookie("omni_admin_session");

        mockMvc.perform(post("/api/admin/plans")
                        .cookie(csrfCookie, sessionCookie)
                        .header("X-XSRF-TOKEN", csrfToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "planCode": "starter",
                                  "planName": "Gói Starter",
                                  "billingPeriod": "MONTHLY",
                                  "priceAmount": 99000,
                                  "currency": "VND",
                                  "limits": {
                                    "marketplaceAccounts": 2,
                                    "tenantUsers": 5
                                  },
                                  "features": {
                                    "aiSale": true
                                  },
                                  "status": "ACTIVE"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.planCode").value("STARTER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(get("/api/admin/plans")
                        .cookie(sessionCookie)
                        .param("search", "starter")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].planCode").value("STARTER"));
    }

    @Test
    void repeatedInvalidPasswordsLockThePlatformOwner() {
        for (int attempt = 0; attempt < 5; attempt++) {
            assertFalse(authenticationService.login(
                    "owner@example.com",
                    "Wrong-password-2026!",
                    "127.0.0.1",
                    "JUnit").success());
        }

        assertEquals(
                "LOCKED",
                jdbcTemplate.queryForObject(
                        "SELECT status FROM platform_admins WHERE id = ?",
                        String.class,
                        ADMIN_ID));
        assertFalse(authenticationService.login(
                "owner@example.com",
                "Strong-owner-2026!",
                "127.0.0.1",
                "JUnit").success());
    }

    @Test
    void createsCompleteTenantGraphInOneTransaction() {
        PlatformAdminPrincipal principal =
                new PlatformAdminPrincipal(ADMIN_ID, "owner@example.com", "Owner", "session-id");
        CreateTenantRequest request = new CreateTenantRequest(
                "Cửa hàng 001",
                "Công ty TNHH Shop 001",
                "contact@shop001.example",
                "Asia/Ho_Chi_Minh",
                "vnd",
                "demo",
                14,
                "manager@shop001.example",
                "Quản lý Shop 001");

        var response = provisioningService.createTenant(request, principal, "127.0.0.1");

        assertTrue(response.tenantCode().matches("TEN_[A-F0-9]{12}"));
        assertEquals("TRIAL", response.tenantStatus());
        assertEquals("TENANT_MANAGER", response.assignedRole());
        assertTrue(response.credentialsEmailSent());
        assertTrue(response.mustChangePassword());
        assertEquals(1, count("tenants"));
        assertEquals(1, count("tenant_subscriptions"));
        assertEquals(1, count("tenant_users"));
        assertEquals(1, count("tenant_user_credentials"));
        assertEquals(1, count("tenant_user_roles"));
        assertEquals(8, count("data_protection_policies"));

        String storedPasswordHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM tenant_user_credentials WHERE tenant_user_id = ?",
                String.class,
                response.ownerUserId());
        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        verify(credentialEmailService).sendTemporaryPassword(
                org.mockito.ArgumentMatchers.eq("manager@shop001.example"),
                org.mockito.ArgumentMatchers.eq("Quản lý Shop 001"),
                org.mockito.ArgumentMatchers.eq("Cửa hàng 001"),
                org.mockito.ArgumentMatchers.eq(response.tenantCode()),
                passwordCaptor.capture());
        assertNotEquals(passwordCaptor.getValue(), storedPasswordHash);
        assertTrue(passwordEncoder.matches(passwordCaptor.getValue(), storedPasswordHash));

        assertThrows(
                TenantConflictException.class,
                () -> provisioningService.createTenant(request, principal, "127.0.0.1"));
        assertEquals(1, count("tenants"));

        var page = provisioningService.listTenants(response.tenantCode(), "TRIAL", 0, 20);
        assertEquals(1, page.totalElements());
        assertEquals(response.tenantCode(), page.items().get(0).tenantCode());
        assertEquals("manager@shop001.example", page.items().get(0).ownerEmail());
    }

    @Test
    void rejectsMalformedOwnerEmailBeforeTenantProvisioning() {
        CreateTenantRequest request = new CreateTenantRequest(
                "Invalid email tenant",
                null,
                null,
                "Asia/Ho_Chi_Minh",
                "VND",
                "DEMO",
                14,
                "not-an-email",
                "Invalid Email");

        assertTrue(validator.validate(request).stream().anyMatch(violation ->
                "ownerEmail".contentEquals(violation.getPropertyPath().toString())));
        assertEquals(0, count("tenants"));
    }

    @Test
    void locksAndUnlocksTenantAndRevokesActiveSessions() {
        PlatformAdminPrincipal principal =
                new PlatformAdminPrincipal(ADMIN_ID, "owner@example.com", "Owner", "session-id");
        CreateTenantRequest request = new CreateTenantRequest(
                "Tenant access test",
                null,
                null,
                "Asia/Ho_Chi_Minh",
                "VND",
                "DEMO",
                14,
                "access-test@example.com",
                "Access Test");
        var tenant = provisioningService.createTenant(request, principal, "127.0.0.1");
        jdbcTemplate.update(
                """
                INSERT INTO login_sessions (
                    id, actor_type, tenant_user_id, session_token_hash, auth_stage,
                    issued_at, expires_at
                ) VALUES (?, 'TENANT_USER', ?, ?, 'AUTHENTICATED', CURRENT_TIMESTAMP, ?)
                """,
                "90000000-0000-0000-0000-000000000001",
                tenant.ownerUserId(),
                "active-tenant-session-hash",
                java.sql.Timestamp.from(java.time.Instant.now().plusSeconds(3600)));

        var locked = provisioningService.setTenantLocked(
                tenant.tenantId(), true, principal, "127.0.0.1");
        assertEquals("SUSPENDED", locked.tenantStatus());
        assertTrue(locked.locked());
        assertEquals(1, locked.revokedSessions());
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM login_sessions WHERE revoked_at IS NOT NULL",
                Integer.class));

        var unlocked = provisioningService.setTenantLocked(
                tenant.tenantId(), false, principal, "127.0.0.1");
        assertEquals("TRIAL", unlocked.tenantStatus());
        assertFalse(unlocked.locked());
        assertEquals("TRIAL", jdbcTemplate.queryForObject(
                "SELECT status FROM tenants WHERE id = ?",
                String.class,
                tenant.tenantId()));
    }

    @Test
    void rollsBackEveryTenantRecordWhenCredentialEmailCannotBeSent() {
        PlatformAdminPrincipal principal =
                new PlatformAdminPrincipal(ADMIN_ID, "owner@example.com", "Owner", "session-id");
        CreateTenantRequest request = new CreateTenantRequest(
                "Tenant mail rollback",
                null,
                null,
                "Asia/Ho_Chi_Minh",
                "VND",
                "DEMO",
                14,
                "mail-failure@example.com",
                "Mail Failure");
        doThrow(new TenantEmailDeliveryException("Email delivery failed", new RuntimeException()))
                .when(credentialEmailService)
                .sendTemporaryPassword(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString());

        assertThrows(
                TenantEmailDeliveryException.class,
                () -> provisioningService.createTenant(request, principal, "127.0.0.1"));
        assertEquals(0, count("tenants"));
        assertEquals(0, count("tenant_users"));
        assertEquals(0, count("tenant_user_credentials"));
        assertEquals(0, count("security_audit_logs"));
    }

    @Test
    void managesSubscriptionPlanLifecycleWithoutPhysicalDeletion() {
        PlatformAdminPrincipal principal =
                new PlatformAdminPrincipal(ADMIN_ID, "owner@example.com", "Owner", "session-id");
        SubscriptionPlanRequest createRequest = new SubscriptionPlanRequest(
                "growth_monthly",
                "Gói Growth",
                "MONTHLY",
                new BigDecimal("199000.00"),
                "vnd",
                Map.of("marketplaceAccounts", 4, "tenantUsers", 25),
                Map.of("aiSale", true, "analytics", true),
                "ACTIVE");

        var created = planService.create(createRequest, principal, "127.0.0.1");
        assertEquals("GROWTH_MONTHLY", created.planCode());
        assertEquals("Gói Growth", created.planName());
        assertEquals("VND", created.currency());
        assertEquals(4, created.limits().get("marketplaceAccounts"));
        assertEquals(true, created.features().get("aiSale"));

        var page = planService.list("growth", "ACTIVE", 0, 20);
        assertEquals(1, page.totalElements());
        assertEquals(created.planId(), page.items().get(0).planId());

        SubscriptionPlanRequest updateRequest = new SubscriptionPlanRequest(
                "growth_yearly",
                "Gói Growth theo năm",
                "YEARLY",
                new BigDecimal("1990000.00"),
                "VND",
                Map.of("marketplaceAccounts", 8, "tenantUsers", 50),
                Map.of("aiSale", true, "analytics", true, "dailyEmail", true),
                "INACTIVE");
        var updated = planService.update(
                created.planId(),
                updateRequest,
                principal,
                "127.0.0.1");
        assertEquals("GROWTH_YEARLY", updated.planCode());
        assertEquals("YEARLY", updated.billingPeriod());
        assertEquals("INACTIVE", updated.status());

        var archived = planService.updateStatus(
                created.planId(),
                "ARCHIVED",
                principal,
                "127.0.0.1");
        assertEquals("ARCHIVED", archived.status());
        assertEquals(2, count("subscription_plans"));

        assertThrows(
                SubscriptionPlanConflictException.class,
                () -> planService.create(
                        new SubscriptionPlanRequest(
                                "demo",
                                "Duplicate demo",
                                "MONTHLY",
                                BigDecimal.ZERO,
                                "VND",
                                Map.of(),
                                Map.of(),
                                "ACTIVE"),
                        principal,
                        "127.0.0.1"));
        assertThrows(
                SubscriptionPlanNotFoundException.class,
                () -> planService.get("missing-plan-id"));

        Integer auditCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM security_audit_logs
                WHERE target_type = 'SUBSCRIPTION_PLAN'
                  AND target_id = ?
                """,
                Integer.class,
                created.planId());
        assertEquals(3, auditCount);
    }

    private int count(String tableName) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);
    }
}
