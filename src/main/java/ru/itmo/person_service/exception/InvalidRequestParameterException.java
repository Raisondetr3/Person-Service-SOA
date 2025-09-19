package ru.itmo.person_service.exception;

import lombok.Getter;

@Getter
public class InvalidRequestParameterException extends RuntimeException {
    private final String parameterName;
    private final String parameterValue;

    public InvalidRequestParameterException(String parameterName, String parameterValue, String message) {
        super(message);
        this.parameterName = parameterName;
        this.parameterValue = parameterValue;
    }
}