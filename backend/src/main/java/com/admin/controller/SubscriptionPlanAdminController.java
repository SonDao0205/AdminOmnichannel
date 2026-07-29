package com.admin.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import com.admin.dto.SubscriptionPlanPageResponse;
import com.admin.dto.SubscriptionPlanRequest;
import com.admin.dto.SubscriptionPlanResponse;
import com.admin.dto.SubscriptionPlanStatusRequest;
import com.admin.security.PlatformAdminPrincipal;
import com.admin.service.SubscriptionPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Subscription Plan Administration")
@SecurityRequirement(name = "adminSession")
@RestController
@RequestMapping("/api/admin/plans")
@PreAuthorize("hasRole('PLATFORM_OWNER')")
public class SubscriptionPlanAdminController {

    private final SubscriptionPlanService planService;

    public SubscriptionPlanAdminController(SubscriptionPlanService planService) {
        this.planService = planService;
    }

    @Operation(summary = "Create a subscription plan")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Plan was created"),
            @ApiResponse(responseCode = "400", description = "Request data is invalid", content = @Content),
            @ApiResponse(responseCode = "401", description = "Admin session is missing", content = @Content),
            @ApiResponse(responseCode = "403", description = "CSRF token or permission is invalid", content = @Content),
            @ApiResponse(responseCode = "409", description = "Plan code already exists", content = @Content)
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionPlanResponse create(
            @Valid @RequestBody SubscriptionPlanRequest request,
            @AuthenticationPrincipal PlatformAdminPrincipal admin,
            HttpServletRequest servletRequest
    ) {
        return planService.create(request, admin, servletRequest.getRemoteAddr());
    }

    @Operation(summary = "List subscription plans")
    @GetMapping
    public SubscriptionPlanPageResponse list(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return planService.list(search, status, page, size);
    }

    @Operation(summary = "Get one subscription plan")
    @GetMapping("/{planId}")
    public SubscriptionPlanResponse get(@PathVariable String planId) {
        return planService.get(planId);
    }

    @Operation(summary = "Replace a subscription plan configuration")
    @PutMapping("/{planId}")
    public SubscriptionPlanResponse update(
            @PathVariable String planId,
            @Valid @RequestBody SubscriptionPlanRequest request,
            @AuthenticationPrincipal PlatformAdminPrincipal admin,
            HttpServletRequest servletRequest
    ) {
        return planService.update(planId, request, admin, servletRequest.getRemoteAddr());
    }

    @Operation(
            summary = "Change subscription plan status",
            description = "Use ARCHIVED instead of physically deleting a referenced plan.")
    @PatchMapping("/{planId}/status")
    public SubscriptionPlanResponse updateStatus(
            @PathVariable String planId,
            @Valid @RequestBody SubscriptionPlanStatusRequest request,
            @AuthenticationPrincipal PlatformAdminPrincipal admin,
            HttpServletRequest servletRequest
    ) {
        return planService.updateStatus(
                planId,
                request.status(),
                admin,
                servletRequest.getRemoteAddr());
    }
}
