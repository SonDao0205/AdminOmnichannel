package com.admin.security;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class PasswordPolicy {

    private static final int MIN_LENGTH = 12;
    private static final int MAX_LENGTH = 128;
    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL = Pattern.compile("[^A-Za-z0-9]");

    public void validate(String password) {
        if (password == null
                || password.length() < MIN_LENGTH
                || password.length() > MAX_LENGTH
                || !UPPERCASE.matcher(password).find()
                || !LOWERCASE.matcher(password).find()
                || !DIGIT.matcher(password).find()
                || !SPECIAL.matcher(password).find()) {
            throw new IllegalArgumentException(
                    "Password must be 12-128 characters and contain uppercase, lowercase, number and special character");
        }
    }
}
