package ru.itmo.person_service.exception;

public class InvalidParameterException extends RuntimeException {
    private final String parameterName;
    private final Object parameterValue;

    public InvalidParameterException(String parameterName, Object parameterValue, String message) {
        super(String.format("Invalid parameter '%s' with value '%s': %s",
                parameterName, parameterValue, message));
        this.parameterName = parameterName;
        this.parameterValue = parameterValue;
    }

    public String getParameterName() { return parameterName; }
    public Object getParameterValue() { return parameterValue; }
}