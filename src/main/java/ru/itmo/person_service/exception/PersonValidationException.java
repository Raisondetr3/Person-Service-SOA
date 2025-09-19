package ru.itmo.person_service.exception;

import java.util.Map;
import java.util.HashMap;

public class PersonValidationException extends RuntimeException {

    private final Map<String, String> errors;
    private final String fieldName;
    private final ValidationSource source;

    public enum ValidationSource {
        URL_PARAMETER,
        REQUEST_BODY
    }

    public PersonValidationException(Map<String, String> errors) {
        super("Person validation failed: " + errors.toString());
        this.errors = new HashMap<>(errors);
        this.fieldName = null;
        this.source = ValidationSource.REQUEST_BODY;
    }

    public PersonValidationException(String field, String message, ValidationSource source) {
        super("Person validation failed: " + field + " - " + message);
        this.errors = new HashMap<>();
        this.errors.put(field, message);
        this.fieldName = field;
        this.source = source;
    }

    public PersonValidationException(String field, String message) {
        this(field, message, ValidationSource.REQUEST_BODY);
    }

    public PersonValidationException(String message) {
        super(message);
        this.errors = null;
        this.fieldName = null;
        this.source = ValidationSource.REQUEST_BODY;
    }

    public Map<String, String> getErrors() {
        return errors;
    }

    public String getFieldName() {
        return fieldName;
    }

    public ValidationSource getSource() {
        return source;
    }

    public boolean hasFieldErrors() {
        return errors != null && !errors.isEmpty();
    }

    public boolean isUrlParameterError() {
        return source == ValidationSource.URL_PARAMETER;
    }

    public boolean isRequestBodyError() {
        return source == ValidationSource.REQUEST_BODY;
    }
}