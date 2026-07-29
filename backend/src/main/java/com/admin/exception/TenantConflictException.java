package com.admin.exception;

public class TenantConflictException extends RuntimeException {

    public TenantConflictException(String message) {
        super(message);
    }
}
