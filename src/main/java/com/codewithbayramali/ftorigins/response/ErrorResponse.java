package com.codewithbayramali.ftorigins.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private final boolean success = false;
    private final int status;
    private final String error;
    private final String code;
    private final String message;
    private final String path;
    private final List<FieldError> fieldErrors;
    private final Instant timestamp;

    public ErrorResponse(int status, String error, String code, String message, String path) {
        this(status, error, code, message, path, null);
    }

    public ErrorResponse(int status, String error, String code, String message, String path,
                         List<FieldError> fieldErrors) {
        this.status = status;
        this.error = error;
        this.code = code;
        this.message = message;
        this.path = path;
        this.fieldErrors = fieldErrors;
        this.timestamp = Instant.now();
    }

    public boolean isSuccess() {
        return success;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }

    public List<FieldError> getFieldErrors() {
        return fieldErrors;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public static class FieldError {
        private final String field;
        private final String message;

        public FieldError(String field, String message) {
            this.field = field;
            this.message = message;
        }

        public String getField() {
            return field;
        }

        public String getMessage() {
            return message;
        }
    }
}
