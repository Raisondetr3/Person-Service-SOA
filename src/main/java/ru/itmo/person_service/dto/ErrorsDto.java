package ru.itmo.person_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Getter
@Schema(description = "Standard errors response")
@RequiredArgsConstructor
@AllArgsConstructor
public class ErrorsDto {
    @Schema(description = "Error code", example = "VALIDATION_FAILED")
    String error;

    @Schema(description = "Error message", example = "Required fields are missing or empty")
    String message;

    @Schema(description = "Errors", example = "{\n" +
            "    \"weight\": \"Weight must be greater than 0\",\n" +
            "    \"height\": \"Height must be greater than 0\"\n" +
            "  }")
    Map<String, String> errors;

    @Schema(description = "Timestamp when error occurred", example = "2025-09-19T09:32:19.479Z")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant timestamp;

    @Schema(description = "Request path that caused the error", example = "/persons")
    String path;
}
