package ru.itmo.person_service.exception.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import ru.itmo.person_service.dto.ErrorDTO;
import ru.itmo.person_service.exception.InvalidPersonDataException;
import ru.itmo.person_service.exception.PersonNotFoundException;
import ru.itmo.person_service.exception.PersonValidationException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(PersonNotFoundException.class)
    public ResponseEntity<ErrorDTO> handlePersonNotFound(
            PersonNotFoundException e, HttpServletRequest request) {

        log.warn("Person not found: {}", e.getMessage());

        ErrorDTO error = new ErrorDTO(
                "PERSON_NOT_FOUND",
                e.getMessage(),
                LocalDateTime.now(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(PersonValidationException.class)
    public ResponseEntity<Map<String, Object>> handlePersonValidation(
            PersonValidationException e, HttpServletRequest request) {

        log.warn("Person validation failed: {}", e.getErrors());

        HttpStatus status = determineValidationStatus(e);
        String errorCode = determineValidationErrorCode(e);

        Map<String, Object> response = new HashMap<>();
        response.put("status", status.value());
        response.put("error", errorCode);
        response.put("message", determineValidationMessage(e));

        if (e.getErrors() != null) {
            response.put("errors", e.getErrors());
        }

        response.put("timestamp", LocalDateTime.now());
        response.put("path", request.getRequestURI());

        return ResponseEntity.status(status).body(response);
    }

    private HttpStatus determineValidationStatus(PersonValidationException e) {
        if (e.getErrors() != null) {
            boolean hasRequiredFieldErrors = e.getErrors().entrySet().stream()
                    .anyMatch(entry -> entry.getValue().toLowerCase().contains("required") ||
                            entry.getValue().toLowerCase().contains("cannot be null") ||
                            entry.getValue().toLowerCase().contains("cannot be empty"));

            boolean hasFormatErrors = e.getErrors().entrySet().stream()
                    .anyMatch(entry -> entry.getValue().toLowerCase().contains("invalid") ||
                            entry.getValue().toLowerCase().contains("must be between") ||
                            entry.getValue().toLowerCase().contains("cannot exceed"));

            if (hasFormatErrors && !hasRequiredFieldErrors) {
                return HttpStatus.UNPROCESSABLE_ENTITY;
            }

            if (hasRequiredFieldErrors) {
                return HttpStatus.BAD_REQUEST;
            }
        }

        return HttpStatus.BAD_REQUEST;
    }

    private String determineValidationErrorCode(PersonValidationException e) {
        if (e.getErrors() != null) {
            boolean hasRequiredFieldErrors = e.getErrors().entrySet().stream()
                    .anyMatch(entry -> entry.getValue().toLowerCase().contains("required"));

            if (hasRequiredFieldErrors) {
                return "MISSING_REQUIRED_FIELDS";
            }
        }

        return "VALIDATION_FAILED";
    }

    private String determineValidationMessage(PersonValidationException e) {
        if (e.getErrors() != null) {
            boolean hasRequiredFieldErrors = e.getErrors().entrySet().stream()
                    .anyMatch(entry -> entry.getValue().toLowerCase().contains("required"));

            if (hasRequiredFieldErrors) {
                return "Required fields are missing or empty";
            }
        }

        return "Request data validation failed";
    }

    @ExceptionHandler(InvalidPersonDataException.class)
    public ResponseEntity<ErrorDTO> handleInvalidPersonData(
            InvalidPersonDataException e, HttpServletRequest request) {

        log.warn("Invalid person data: {}", e.getMessage());

        ErrorDTO error = new ErrorDTO(
                "UNPROCESSABLE_ENTITY",
                e.getMessage(),
                LocalDateTime.now(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException e, HttpServletRequest request) {

        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        log.warn("Validation errors: {}", errors);

        boolean hasRequiredFieldErrors = errors.values().stream()
                .anyMatch(msg -> msg.toLowerCase().contains("must not be null") ||
                        msg.toLowerCase().contains("must not be blank") ||
                        msg.toLowerCase().contains("is required"));

        HttpStatus status = hasRequiredFieldErrors ?
                HttpStatus.BAD_REQUEST :
                HttpStatus.UNPROCESSABLE_ENTITY;

        Map<String, Object> response = new HashMap<>();
        response.put("status", status.value());
        response.put("error", hasRequiredFieldErrors ? "MISSING_REQUIRED_FIELDS" : "VALIDATION_FAILED");
        response.put("message", hasRequiredFieldErrors ?
                "Required fields are missing" :
                "Request validation failed");
        response.put("errors", errors);
        response.put("timestamp", LocalDateTime.now());
        response.put("path", request.getRequestURI());

        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorDTO> handleConstraintViolation(
            ConstraintViolationException e, HttpServletRequest request) {

        log.warn("Constraint violation: {}", e.getMessage());

        ErrorDTO error = new ErrorDTO(
                "CONSTRAINT_VIOLATION",
                "Business rule constraint violation: " + e.getMessage(),
                LocalDateTime.now(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorDTO> handleDataIntegrityViolation(
            DataIntegrityViolationException e, HttpServletRequest request) {

        log.error("Data integrity violation: {}", e.getMessage());

        HttpStatus status;
        String errorCode;
        String message;

        if (e.getMessage().contains("duplicate") || e.getMessage().contains("unique")) {
            status = HttpStatus.CONFLICT;
            errorCode = "DUPLICATE_ENTRY";
            message = "Resource already exists with these attributes";
        } else if (e.getMessage().contains("not-null constraint") || e.getMessage().contains("null value")) {
            status = HttpStatus.BAD_REQUEST;
            errorCode = "MISSING_REQUIRED_FIELD";
            message = "Required field is missing or null";
        } else if (e.getMessage().contains("foreign key constraint")) {
            status = HttpStatus.UNPROCESSABLE_ENTITY;
            errorCode = "INVALID_REFERENCE";
            message = "Referenced entity does not exist";
        } else if (e.getMessage().contains("check constraint")) {
            status = HttpStatus.UNPROCESSABLE_ENTITY;
            errorCode = "CONSTRAINT_VIOLATION";
            message = "Data violates database constraints";
        } else {
            status = HttpStatus.CONFLICT;
            errorCode = "DATA_INTEGRITY_VIOLATION";
            message = "Data integrity constraint violation";
        }

        ErrorDTO error = new ErrorDTO(
                errorCode,
                message,
                LocalDateTime.now(),
                request.getRequestURI()
        );

        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorDTO> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException e, HttpServletRequest request) {

        log.warn("Method argument type mismatch: parameter={}, value={}, expectedType={}",
                e.getName(), e.getValue(), e.getRequiredType().getSimpleName());

        String message;
        if (e.getRequiredType().isEnum()) {
            message = String.format("Invalid value '%s' for parameter '%s'. Expected one of: %s",
                    e.getValue(), e.getName(), Arrays.toString(e.getRequiredType().getEnumConstants()));
        } else {
            message = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s",
                    e.getValue(), e.getName(), e.getRequiredType().getSimpleName());
        }

        ErrorDTO error = new ErrorDTO(
                "INVALID_PARAMETER_TYPE",
                message,
                LocalDateTime.now(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorDTO> handleHttpMessageNotReadable(
            HttpMessageNotReadableException e, HttpServletRequest request) {

        log.warn("HTTP message not readable: {}", e.getMessage());

        HttpStatus status;
        String errorCode;
        String message;

        if (e.getMessage().contains("Required request body is missing")) {
            status = HttpStatus.BAD_REQUEST;
            errorCode = "MISSING_REQUEST_BODY";
            message = "Request body is required but missing";
        } else if (e.getMessage().contains("Cannot deserialize value")) {
            if (e.getMessage().contains("not one of the values accepted for Enum")) {
                status = HttpStatus.UNPROCESSABLE_ENTITY;
                errorCode = "INVALID_ENUM_VALUE";
                message = "Invalid enum value provided. Check allowed values for enum fields.";
            } else {
                status = HttpStatus.UNPROCESSABLE_ENTITY;
                errorCode = "INVALID_VALUE_FORMAT";
                message = "Invalid value format in request body";
            }
        } else {
            status = HttpStatus.BAD_REQUEST;
            errorCode = "MALFORMED_JSON";
            message = "Invalid JSON format in request body";
        }

        ErrorDTO error = new ErrorDTO(
                errorCode,
                message,
                LocalDateTime.now(),
                request.getRequestURI()
        );

        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorDTO> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException e, HttpServletRequest request) {

        log.warn("Method not supported: {} for {}", e.getMethod(), request.getRequestURI());

        String message = String.format("Method %s not allowed for %s. Supported methods: %s",
                e.getMethod(), request.getRequestURI(),
                e.getSupportedMethods() != null ? String.join(", ", e.getSupportedMethods()) : "none");

        ErrorDTO error = new ErrorDTO(
                "METHOD_NOT_ALLOWED",
                message,
                LocalDateTime.now(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(error);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorDTO> handleNoHandlerFound(
            NoHandlerFoundException e, HttpServletRequest request) {

        log.warn("No handler found: {} {}", e.getHttpMethod(), e.getRequestURL());

        ErrorDTO error = new ErrorDTO(
                "ENDPOINT_NOT_FOUND",
                String.format("Endpoint %s %s not found", e.getHttpMethod(), e.getRequestURL()),
                LocalDateTime.now(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorDTO> handleIllegalArgumentException(
            IllegalArgumentException e, HttpServletRequest request) {

        log.warn("Illegal argument: {}", e.getMessage());

        ErrorDTO error = new ErrorDTO(
                "INVALID_ARGUMENT",
                e.getMessage(),
                LocalDateTime.now(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
    }

    @ExceptionHandler(MissingPathVariableException.class)
    public ResponseEntity<ErrorDTO> handleMissingPathVariable(
            MissingPathVariableException e, HttpServletRequest request) {

        log.warn("Missing path variable: {}", e.getVariableName());

        ErrorDTO error = new ErrorDTO(
                "MISSING_PATH_VARIABLE",
                String.format("Missing required path variable: %s", e.getVariableName()),
                LocalDateTime.now(),
                request.getRequestURI()
        );

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorDTO> handleMissingServletRequestParameter(
            MissingServletRequestParameterException e, HttpServletRequest request) {

        log.warn("Missing request parameter: {} of type {}", e.getParameterName(), e.getParameterType());

        ErrorDTO error = new ErrorDTO(
                "MISSING_REQUEST_PARAMETER",
                String.format("Missing required request parameter: %s", e.getParameterName()),
                LocalDateTime.now(),
                request.getRequestURI()
        );

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDTO> handleGenericException(
            Exception e, HttpServletRequest request) {

        log.error("Unexpected exception: {}", e.getMessage(), e);

        ErrorDTO error = new ErrorDTO(
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred",
                LocalDateTime.now(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}