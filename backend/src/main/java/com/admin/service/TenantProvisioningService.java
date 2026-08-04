package com.admin.service;

import com.admin.dto.CreateTenantRequest;
import com.admin.dto.CreateTenantResponse;
import com.admin.dto.TenantPageResponse;
import com.admin.dto.TenantAccessResponse;
import com.admin.security.PlatformAdminPrincipal;

public interface TenantProvisioningService {

    CreateTenantResponse createTenant(
            CreateTenantRequest request,
            PlatformAdminPrincipal admin,
            String ipAddress);

    TenantPageResponse listTenants(String search, String status, int page, int size);

    TenantAccessResponse setTenantLocked(
            String tenantId,
            boolean locked,
            PlatformAdminPrincipal admin,
            String ipAddress);
}
