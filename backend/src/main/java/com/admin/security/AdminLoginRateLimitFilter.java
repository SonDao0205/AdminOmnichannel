package com.admin.security;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.admin.config.AdminSecurityProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AdminLoginRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/admin/auth/login";

    private final AdminSecurityProperties properties;
    private final Clock clock;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    @Autowired
    public AdminLoginRateLimitFilter(AdminSecurityProperties properties) {
        this(properties, Clock.systemUTC());
    }

    AdminLoginRateLimitFilter(AdminSecurityProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) || !LOGIN_PATH.equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Instant now = clock.instant();
        String key = request.getRemoteAddr();
        Window result = windows.compute(key, (ignored, current) -> {
            if (current == null || !now.isBefore(current.startedAt().plus(properties.getLoginRateWindow()))) {
                return new Window(now, 1);
            }
            return new Window(current.startedAt(), current.count() + 1);
        });

        if (result.count() > properties.getLoginRateLimit()) {
            long retryAfter = Math.max(
                    1,
                    result.startedAt().plus(properties.getLoginRateWindow()).getEpochSecond() - now.getEpochSecond());
            response.setStatus(429);
            response.setHeader("Retry-After", Long.toString(retryAfter));
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.getWriter().write(
                    "{\"type\":\"about:blank\",\"title\":\"Too Many Requests\","
                            + "\"status\":429,\"detail\":\"Too many login attempts. Try again later.\"}");
            return;
        }

        if (windows.size() > 10_000) {
            windows.entrySet().removeIf(entry ->
                    !now.isBefore(entry.getValue().startedAt().plus(properties.getLoginRateWindow())));
        }
        filterChain.doFilter(request, response);
    }

    private record Window(Instant startedAt, int count) {
    }
}
