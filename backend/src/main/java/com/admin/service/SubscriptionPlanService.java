package com.admin.service;

import com.admin.dto.SubscriptionPlanPageResponse;
import com.admin.dto.SubscriptionPlanRequest;
import com.admin.dto.SubscriptionPlanResponse;
import com.admin.security.PlatformAdminPrincipal;

public interface SubscriptionPlanService {

    SubscriptionPlanResponse create(
            SubscriptionPlanRequest request,
            PlatformAdminPrincipal admin,
            String ipAddress);

    SubscriptionPlanPageResponse list(String search, String status, int page, int size);

    SubscriptionPlanResponse get(String planId);

    SubscriptionPlanResponse update(
            String planId,
            SubscriptionPlanRequest request,
            PlatformAdminPrincipal admin,
            String ipAddress);

    SubscriptionPlanResponse updateStatus(
            String planId,
            String status,
            PlatformAdminPrincipal admin,
            String ipAddress);
}
