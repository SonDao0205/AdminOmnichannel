package com.admin.exception;

public class TenantEmailDeliveryException extends RuntimeException {

    public TenantEmailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
