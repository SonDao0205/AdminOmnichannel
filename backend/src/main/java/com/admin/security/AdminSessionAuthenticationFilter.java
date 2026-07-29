package com.admin.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.admin.config.AdminSecurityProperties;
import com.admin.repository.PlatformAdminRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AdminSessionAuthenticationFilter extends OncePerRequestFilter {

    private static final List<SimpleGrantedAuthority> AUTHORITIES =
            List.of(new SimpleGrantedAuthority("ROLE_PLATFORM_OWNER"));

    private final PlatformAdminRepository repository;
    private final SecureTokenService tokenService;
    private final AdminSecurityProperties properties;

    public AdminSessionAuthenticationFilter(
            PlatformAdminRepository repository,
            SecureTokenService tokenService,
            AdminSecurityProperties properties
    ) {
        this.repository = repository;
        this.tokenService = tokenService;
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            readSessionCookie(request)
                    .flatMap(this::findActiveAdmin)
                    .ifPresent(principal -> {
                        var authentication =
                                new UsernamePasswordAuthenticationToken(principal, null, AUTHORITIES);
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    });
        }
        filterChain.doFilter(request, response);
    }

    private Optional<String> readSessionCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> properties.getCookieName().equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> !value.isBlank() && value.length() <= 256)
                .findFirst();
    }

    private Optional<PlatformAdminPrincipal> findActiveAdmin(String rawToken) {
        String tokenHash = tokenService.sha256(rawToken);
        return repository.findActiveSession(tokenHash, java.time.Instant.now());
    }
}
