package com.admin.exception;

public class SubscriptionPlanNotFoundException extends RuntimeException {

    public SubscriptionPlanNotFoundException(String message) {
        super(message);
    }
}
