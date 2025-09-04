package ru.itmo.person_service.exception;

import java.util.Map;
import java.util.HashMap;

public class PersonValidationException extends RuntimeException {

    private final Map<String, String> errors;

    public PersonValidationException(Map<String, String> errors) {
        super("Validation failed: " + errors.toString());
        this.errors = new HashMap<>(errors);
    }

    public PersonValidationException(String field, String message) {
        super("Validation failed for field '" + field + "': " + message);
        this.errors = Map.of(field, message);
    }

    public PersonValidationException(String message) {
        super(message);
        this.errors = new HashMap<>();
    }

    public Map<String, String> getErrors() {
        return new HashMap<>(errors);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public void addError(String field, String message) {
        errors.put(field, message);
    }
}
