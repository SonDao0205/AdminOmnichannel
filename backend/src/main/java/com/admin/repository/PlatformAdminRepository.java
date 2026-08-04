package com.admin.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import com.admin.entity.PlatformAdmin;
import com.admin.security.PlatformAdminPrincipal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Repository
public class PlatformAdminRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public PlatformAdminRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public Optional<PlatformAdmin> findByEmailForUpdate(String email) {
        return jdbcTemplate.query(
                        """
                        SELECT id, email, password_hash, display_name, status,
                               failed_login_count, locked_until
                        FROM platform_admins
                        WHERE email = ?
                        LIMIT 1
                        FOR UPDATE
                        """,
                        (resultSet, rowNumber) -> new PlatformAdmin(
                                resultSet.getString("id"),
                                resultSet.getString("email"),
                                resultSet.getString("password_hash"),
                                resultSet.getString("display_name"),
                                resultSet.getString("status"),
                                resultSet.getInt("failed_login_count"),
                                resultSet.getTimestamp("locked_until") == null
                                        ? null
                                        : resultSet.getTimestamp("locked_until").toInstant()),
                        email)
                .stream()
                .findFirst();
    }

    public Optional<PlatformAdminPrincipal> findActiveSession(String tokenHash, Instant now) {
        return jdbcTemplate.query(
                        """
                        SELECT s.id AS session_id, a.id AS admin_id, a.email, a.display_name
                        FROM login_sessions s
                        JOIN platform_admins a ON a.id = s.platform_admin_id
                        WHERE s.session_token_hash = ?
                          AND s.actor_type = 'PLATFORM_ADMIN'
                          AND s.auth_stage = 'AUTHENTICATED'
                          AND s.revoked_at IS NULL
                          AND s.expires_at > ?
                          AND a.status = 'ACTIVE'
                          AND (a.locked_until IS NULL OR a.locked_until <= ?)
                        LIMIT 1
                        """,
                        (resultSet, rowNumber) -> new PlatformAdminPrincipal(
                                resultSet.getString("admin_id"),
                                resultSet.getString("email"),
                                resultSet.getString("display_name"),
                                resultSet.getString("session_id")),
                        tokenHash,
                        Timestamp.from(now),
                        Timestamp.from(now))
                .stream()
                .findFirst();
    }

    public void reactivateAfterTemporaryLock(String adminId) {
        jdbcTemplate.update(
                """
                UPDATE platform_admins
                SET status = 'ACTIVE', failed_login_count = 0, locked_until = NULL
                WHERE id = ?
                """,
                adminId);
    }

    public void recordFailedLogin(String adminId, int failureCount) {
        jdbcTemplate.update(
                "UPDATE platform_admins SET failed_login_count = ? WHERE id = ?",
                failureCount,
                adminId);
    }

    public void lockAfterFailedLogin(String adminId, int failureCount, Instant lockedUntil) {
        jdbcTemplate.update(
                """
                UPDATE platform_admins
                SET failed_login_count = ?, status = 'LOCKED', locked_until = ?
                WHERE id = ?
                """,
                failureCount,
                Timestamp.from(lockedUntil),
                adminId);
    }

    public void recordSuccessfulLogin(String adminId, Instant now) {
        jdbcTemplate.update(
                """
                UPDATE platform_admins
                SET failed_login_count = 0, locked_until = NULL,
                    status = 'ACTIVE', last_login_at = ?
                WHERE id = ?
                """,
                Timestamp.from(now),
                adminId);
    }

    public void revokeActiveSessions(String adminId, Instant revokedAt) {
        jdbcTemplate.update(
                """
                UPDATE login_sessions
                SET revoked_at = ?
                WHERE platform_admin_id = ?
                  AND actor_type = 'PLATFORM_ADMIN'
                  AND revoked_at IS NULL
                """,
                Timestamp.from(revokedAt),
                adminId);
    }

    public void createSession(
            String sessionId,
            String adminId,
            String tokenHash,
            String ipAddress,
            String userAgent,
            Instant issuedAt,
            Instant expiresAt
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO login_sessions (
                    id, actor_type, platform_admin_id, tenant_user_id,
                    session_token_hash, auth_stage, ip_address, user_agent,
                    issued_at, expires_at, revoked_at
                ) VALUES (?, 'PLATFORM_ADMIN', ?, NULL, ?, 'AUTHENTICATED', ?, ?, ?, ?, NULL)
                """,
                sessionId,
                adminId,
                tokenHash,
                ipAddress,
                userAgent,
                Timestamp.from(issuedAt),
                Timestamp.from(expiresAt));
    }

    public boolean revokeSession(String sessionId, String adminId, Instant revokedAt) {
        return jdbcTemplate.update(
                """
                UPDATE login_sessions
                SET revoked_at = ?
                WHERE id = ?
                  AND platform_admin_id = ?
                  AND revoked_at IS NULL
                """,
                Timestamp.from(revokedAt),
                sessionId,
                adminId) > 0;
    }

    public void insertAudit(
            String actorId,
            String action,
            String targetType,
            String targetId,
            String result,
            String ipAddress,
            String reason
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO security_audit_logs (
                    tenant_id, actor_type, actor_id, action_code,
                    target_type, target_id, result, ip_address, metadata_json
                ) VALUES (
                    NULL, 'PLATFORM_ADMIN', ?, ?, ?, ?, ?, ?, CAST(? AS jsonb)
                )
                """,
                actorId,
                action,
                targetType,
                targetId,
                result,
                ipAddress,
                toJson(Map.of("reason", reason == null ? "" : reason)));
    }

    public Optional<BootstrapOwner> findBootstrapOwnerByEmailForUpdate(String email) {
        return jdbcTemplate.query(
                        "SELECT id, email, password_hash FROM platform_admins WHERE email = ? LIMIT 1 FOR UPDATE",
                        (resultSet, rowNumber) -> new BootstrapOwner(
                                resultSet.getString("id"),
                                resultSet.getString("email"),
                                resultSet.getString("password_hash")),
                        email)
                .stream()
                .findFirst();
    }

    public Optional<BootstrapOwner> findBootstrapOwnerByIdForUpdate(String id) {
        return jdbcTemplate.query(
                        "SELECT id, email, password_hash FROM platform_admins WHERE id = ? LIMIT 1 FOR UPDATE",
                        (resultSet, rowNumber) -> new BootstrapOwner(
                                resultSet.getString("id"),
                                resultSet.getString("email"),
                                resultSet.getString("password_hash")),
                        id)
                .stream()
                .findFirst();
    }

    public int countPlatformAdmins() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM platform_admins", Integer.class);
        return count == null ? 0 : count;
    }

    public void updateBootstrapOwner(
            String ownerId,
            String email,
            String passwordHash,
            String displayName
    ) {
        jdbcTemplate.update(
                """
                UPDATE platform_admins
                SET email = ?, password_hash = ?, password_algorithm = 'ARGON2ID',
                    display_name = ?, status = 'ACTIVE',
                    failed_login_count = 0, locked_until = NULL
                WHERE id = ?
                """,
                email,
                passwordHash,
                displayName,
                ownerId);
    }

    public void updateOwnerDisplayName(String ownerId, String displayName) {
        jdbcTemplate.update(
                "UPDATE platform_admins SET display_name = ?, status = 'ACTIVE' WHERE id = ?",
                displayName,
                ownerId);
    }

    public void insertBootstrapOwner(
            String ownerId,
            String email,
            String passwordHash,
            String displayName
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO platform_admins (
                    id, email, password_hash, password_algorithm,
                    display_name, status, failed_login_count, locked_until
                ) VALUES (?, ?, ?, 'ARGON2ID', ?, 'ACTIVE', 0, NULL)
                """,
                ownerId,
                email,
                passwordHash,
                displayName);
    }

    public record BootstrapOwner(String id, String email, String passwordHash) {
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Could not serialize audit metadata", exception);
        }
    }
}
