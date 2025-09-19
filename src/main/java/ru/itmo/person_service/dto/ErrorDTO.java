package ru.itmo.person_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Schema(description = "Standard error response")
@RequiredArgsConstructor
@AllArgsConstructor
public class ErrorDTO {
    @Schema(description = "Error code", example = "VALIDATION_FAILED")
    String error;

    @Schema(description = "Error message", example = "Required fields are missing or empty")
    String message;

    @Schema(description = "Timestamp when error occurred", example = "2025-09-19T09:32:19.479Z")
    LocalDateTime timestamp;

    @Schema(description = "Request path that caused the error", example = "/persons")
    String path;
}