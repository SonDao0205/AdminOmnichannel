package com.admin.exception;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "One or more request fields are invalid.");
        problem.setTitle("Validation failed");
        problem.setType(URI.create("urn:omnichannel:problem:validation"));

        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleIllegalArgument(IllegalArgumentException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                exception.getMessage());
        problem.setTitle("Invalid request");
        problem.setType(URI.create("urn:omnichannel:problem:invalid-request"));
        return problem;
    }

    @ExceptionHandler({TenantConflictException.class, DataIntegrityViolationException.class})
    ProblemDetail handleConflict(Exception exception) {
        String detail = exception instanceof TenantConflictException
                ? exception.getMessage()
                : "The requested record conflicts with existing data.";
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, detail);
        problem.setTitle("Conflict");
        problem.setType(URI.create("urn:omnichannel:problem:conflict"));
        return problem;
    }

    @ExceptionHandler(TenantProvisioningException.class)
    ProblemDetail handleProvisioning(TenantProvisioningException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_CONTENT,
                exception.getMessage());
        problem.setTitle("Tenant cannot be provisioned");
        problem.setType(URI.create("urn:omnichannel:problem:tenant-provisioning"));
        return problem;
    }
}
