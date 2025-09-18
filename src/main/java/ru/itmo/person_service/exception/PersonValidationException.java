package ru.itmo.person_service.exception;

import java.util.Map;
import java.util.HashMap;

public class PersonValidationException extends RuntimeException {

    private final Map<String, String> errors;

    public PersonValidationException(Map<String, String> errors) {
        super("Person validation failed: " + errors.toString());
        this.errors = new HashMap<>(errors);
    }

    public PersonValidationException(String field, String message) {
        super("Person validation failed: " + field + " - " + message);
        this.errors = new HashMap<>();
        this.errors.put(field, message);
    }

    public PersonValidationException(String message) {
        super(message);
        this.errors = null;
    }

    public Map<String, String> getErrors() {
        return errors;
    }

    public boolean hasFieldErrors() {
        return errors != null && !errors.isEmpty();
    }
}