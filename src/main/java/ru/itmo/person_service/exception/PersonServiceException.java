package ru.itmo.person_service.exception;

public class PersonServiceException extends RuntimeException {

    private final String errorCode;

    public PersonServiceException(String message) {
        super(message);
        this.errorCode = "PERSON_SERVICE_ERROR";
    }

    public PersonServiceException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "PERSON_SERVICE_ERROR";
    }

    public PersonServiceException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public PersonServiceException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}