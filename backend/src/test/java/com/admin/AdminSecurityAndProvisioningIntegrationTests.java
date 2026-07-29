package com.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;

import com.admin.dto.CreateTenantRequest;
import com.admin.exception.TenantConflictException;
import com.admin.security.PlatformAdminPrincipal;
import com.admin.service.AdminAuthenticationService;
import com.admin.service.TenantProvisioningService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

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
    MockMvc mockMvc;

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
                "INSERT INTO subscription_plans (id, plan_code, status) VALUES (?, 'DEMO', 'ACTIVE')",
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
                "shop_001",
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

        assertEquals("SHOP_001", response.tenantCode());
        assertEquals("TRIAL", response.tenantStatus());
        assertEquals("TENANT_MANAGER", response.assignedRole());
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
        assertNotEquals(response.temporaryPassword(), storedPasswordHash);
        assertTrue(passwordEncoder.matches(response.temporaryPassword(), storedPasswordHash));

        assertThrows(
                TenantConflictException.class,
                () -> provisioningService.createTenant(request, principal, "127.0.0.1"));
        assertEquals(1, count("tenants"));

        var page = provisioningService.listTenants("SHOP_001", "TRIAL", 0, 20);
        assertEquals(1, page.totalElements());
        assertEquals("SHOP_001", page.items().get(0).tenantCode());
        assertEquals("manager@shop001.example", page.items().get(0).ownerEmail());
    }

    private int count(String tableName) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);
    }
}
