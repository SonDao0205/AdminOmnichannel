package com.admin.service;

public interface TenantCredentialEmailService {

    void sendTemporaryPassword(
            String recipientEmail,
            String recipientName,
            String tenantName,
            String tenantCode,
            String temporaryPassword);
}
