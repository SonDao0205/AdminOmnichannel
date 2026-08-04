package com.admin.exception;

public class TenantEmailAlreadyExistsException extends TenantConflictException {

    public TenantEmailAlreadyExistsException(String message) {
        super(message);
    }
}
