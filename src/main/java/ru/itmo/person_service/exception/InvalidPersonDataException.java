package ru.itmo.person_service.exception;

public class InvalidPersonDataException extends RuntimeException {

    public InvalidPersonDataException(String message) {
        super(message);
    }

    public InvalidPersonDataException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidPersonDataException(String field, Object value) {
        super("Invalid value for field '" + field + "': " + value);
    }
}