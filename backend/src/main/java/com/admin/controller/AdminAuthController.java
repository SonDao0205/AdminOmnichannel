package com.admin.controller;

import java.time.Duration;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import com.admin.config.AdminSecurityProperties;
import com.admin.dto.AdminLoginRequest;
import com.admin.dto.AdminLoginResponse;
import com.admin.dto.AdminMeResponse;
import com.admin.dto.CsrfResponse;
import com.admin.security.PlatformAdminPrincipal;
import com.admin.service.AdminAuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "Platform Admin Authentication")
@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final AdminAuthenticationService authenticationService;
    private final AdminSecurityProperties properties;

    public AdminAuthController(
            AdminAuthenticationService authenticationService,
            AdminSecurityProperties properties
    ) {
        this.authenticationService = authenticationService;
        this.properties = properties;
    }

    @Operation(
            summary = "Platform-owner login",
            description = "Creates a database-backed opaque session and sends it only as an HttpOnly cookie.")
    @PostMapping("/login")
    public AdminLoginResponse login(
            @Valid @RequestBody AdminLoginRequest body,
            @Parameter(
                    name = "X-XSRF-TOKEN",
                    in = ParameterIn.HEADER,
                    required = true,
                    description = "Token returned by GET /api/admin/auth/csrf")
            @org.springframework.web.bind.annotation.RequestHeader("X-XSRF-TOKEN")
            String csrfToken,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        var result = authenticationService.login(
                body.email(),
                body.password(),
                request.getRemoteAddr(),
                request.getHeader(HttpHeaders.USER_AGENT));

        if (!result.success()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        ResponseCookie cookie = ResponseCookie.from(properties.getCookieName(), result.rawToken())
                .httpOnly(true)
                .secure(properties.isCookieSecure())
                .sameSite(properties.getCookieSameSite())
                .path("/")
                .maxAge(properties.getSessionTtl())
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return new AdminLoginResponse(
                result.adminId(),
                result.email(),
                result.displayName(),
                "PLATFORM_ADMIN",
                result.expiresAt());
    }

    @Operation(summary = "Get a CSRF token for state-changing admin requests")
    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken csrfToken) {
        return new CsrfResponse(
                csrfToken.getHeaderName(),
                csrfToken.getParameterName(),
                csrfToken.getToken());
    }

    @Operation(summary = "Get the authenticated platform owner")
    @SecurityRequirement(name = "adminSession")
    @GetMapping("/me")
    public AdminMeResponse me(@AuthenticationPrincipal PlatformAdminPrincipal principal) {
        return new AdminMeResponse(
                principal.id(),
                principal.email(),
                principal.displayName(),
                "PLATFORM_ADMIN");
    }

    @Operation(summary = "Revoke the current platform-owner session")
    @SecurityRequirement(name = "adminSession")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            @AuthenticationPrincipal PlatformAdminPrincipal principal,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        authenticationService.logout(principal.sessionId(), principal.id(), request.getRemoteAddr());

        ResponseCookie expiredCookie = ResponseCookie.from(properties.getCookieName(), "")
                .httpOnly(true)
                .secure(properties.isCookieSecure())
                .sameSite(properties.getCookieSameSite())
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, expiredCookie.toString());
    }
}
