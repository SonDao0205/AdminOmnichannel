package com.admin.service;

import com.admin.entity.AdminLoginResult;

public interface AdminAuthenticationService {

    AdminLoginResult login(String email, String password, String ipAddress, String userAgent);

    void logout(String sessionId, String adminId, String ipAddress);
}
