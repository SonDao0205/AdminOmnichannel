package com.admin.config;

import java.time.Duration;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.admin.security")
public class AdminSecurityProperties {

    private Duration sessionTtl = Duration.ofHours(8);
    private String cookieName = "omni_admin_session";
    private boolean cookieSecure;
    private String cookieSameSite = "Strict";
    private int maxFailedLogins = 5;
    private Duration lockDuration = Duration.ofMinutes(15);
    private int loginRateLimit = 20;
    private Duration loginRateWindow = Duration.ofMinutes(5);
    private List<String> allowedOrigins = new ArrayList<>(List.of("http://localhost:5173"));

    public Duration getSessionTtl() {
        return sessionTtl;
    }

    public void setSessionTtl(Duration sessionTtl) {
        this.sessionTtl = sessionTtl;
    }

    public String getCookieName() {
        return cookieName;
    }

    public void setCookieName(String cookieName) {
        this.cookieName = cookieName;
    }

    public boolean isCookieSecure() {
        return cookieSecure;
    }

    public void setCookieSecure(boolean cookieSecure) {
        this.cookieSecure = cookieSecure;
    }

    public String getCookieSameSite() {
        return cookieSameSite;
    }

    public void setCookieSameSite(String cookieSameSite) {
        this.cookieSameSite = cookieSameSite;
    }

    public int getMaxFailedLogins() {
        return maxFailedLogins;
    }

    public void setMaxFailedLogins(int maxFailedLogins) {
        this.maxFailedLogins = maxFailedLogins;
    }

    public Duration getLockDuration() {
        return lockDuration;
    }

    public void setLockDuration(Duration lockDuration) {
        this.lockDuration = lockDuration;
    }

    public int getLoginRateLimit() {
        return loginRateLimit;
    }

    public void setLoginRateLimit(int loginRateLimit) {
        this.loginRateLimit = loginRateLimit;
    }

    public Duration getLoginRateWindow() {
        return loginRateWindow;
    }

    public void setLoginRateWindow(Duration loginRateWindow) {
        this.loginRateWindow = loginRateWindow;
    }

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @PostConstruct
    void validate() {
        if (sessionTtl == null || sessionTtl.isZero() || sessionTtl.isNegative()) {
            throw new IllegalStateException("Admin session TTL must be positive");
        }
        if (lockDuration == null || lockDuration.isZero() || lockDuration.isNegative()) {
            throw new IllegalStateException("Admin lock duration must be positive");
        }
        if (loginRateWindow == null || loginRateWindow.isZero() || loginRateWindow.isNegative()) {
            throw new IllegalStateException("Admin login rate window must be positive");
        }
        if (maxFailedLogins < 1 || loginRateLimit < 1) {
            throw new IllegalStateException("Admin login limits must be at least one");
        }
        if (cookieName == null || !cookieName.matches("^[A-Za-z0-9_-]{1,64}$")) {
            throw new IllegalStateException("Admin cookie name is invalid");
        }
        if (!List.of("Strict", "Lax", "None").contains(cookieSameSite)) {
            throw new IllegalStateException("Admin cookie SameSite must be Strict, Lax or None");
        }
        if ("None".equals(cookieSameSite) && !cookieSecure) {
            throw new IllegalStateException("SameSite=None requires a Secure admin cookie");
        }
        if (allowedOrigins == null || allowedOrigins.isEmpty() || allowedOrigins.contains("*")) {
            throw new IllegalStateException("Admin CORS origins must be explicit");
        }
        for (String origin : allowedOrigins) {
            URI uri = URI.create(origin);
            if (uri.getHost() == null
                    || (!"http".equalsIgnoreCase(uri.getScheme())
                    && !"https".equalsIgnoreCase(uri.getScheme()))) {
                throw new IllegalStateException("Invalid admin CORS origin: " + origin);
            }
        }
    }
}
