package com.admin.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import com.admin.dto.CreateTenantRequest;
import com.admin.dto.CreateTenantResponse;
import com.admin.dto.TenantPageResponse;
import com.admin.dto.TenantAccessRequest;
import com.admin.dto.TenantAccessResponse;
import com.admin.security.PlatformAdminPrincipal;
import com.admin.service.TenantProvisioningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Tenant Administration")
@SecurityRequirement(name = "adminSession")
@RestController
@RequestMapping("/api/admin/tenants")
public class TenantAdminController {

    private final TenantProvisioningService provisioningService;

    public TenantAdminController(TenantProvisioningService provisioningService) {
        this.provisioningService = provisioningService;
    }

    @Operation(
            summary = "Create a rented tenant account",
            description = """
                    Atomically creates the tenant, trial subscription, first tenant manager,
                    Argon2id credential, TENANT_MANAGER assignment and default PII policies.
                    The generated tenant code is returned and the temporary password is sent
                    to the validated owner email. No password is exposed in the API response.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tenant was provisioned"),
            @ApiResponse(responseCode = "400", description = "Request data is invalid", content = @Content),
            @ApiResponse(responseCode = "401", description = "Admin session is missing", content = @Content),
            @ApiResponse(responseCode = "403", description = "CSRF token or permission is invalid", content = @Content),
            @ApiResponse(responseCode = "409", description = "Owner email already exists", content = @Content),
            @ApiResponse(responseCode = "422", description = "Credential email could not be sent", content = @Content)
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PLATFORM_OWNER')")
    public CreateTenantResponse create(
            @Valid @RequestBody CreateTenantRequest request,
            @AuthenticationPrincipal PlatformAdminPrincipal admin,
            HttpServletRequest servletRequest
    ) {
        return provisioningService.createTenant(request, admin, servletRequest.getRemoteAddr());
    }

    @Operation(summary = "List rented tenant accounts")
    @GetMapping
    @PreAuthorize("hasRole('PLATFORM_OWNER')")
    public TenantPageResponse list(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return provisioningService.listTenants(search, status, page, size);
    }

    @Operation(summary = "Lock or unlock a tenant account")
    @PatchMapping("/{tenantId}/access")
    @PreAuthorize("hasRole('PLATFORM_OWNER')")
    public TenantAccessResponse setAccess(
            @PathVariable String tenantId,
            @Valid @RequestBody TenantAccessRequest request,
            @AuthenticationPrincipal PlatformAdminPrincipal admin,
            HttpServletRequest servletRequest
    ) {
        return provisioningService.setTenantLocked(
                tenantId,
                request.locked(),
                admin,
                servletRequest.getRemoteAddr());
    }
}
