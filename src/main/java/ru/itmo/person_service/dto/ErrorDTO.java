package ru.itmo.person_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ErrorDTO {
    private final String error;
    private final String message;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime timestamp;

    private final String path;

    public ErrorDTO(String error, String message, LocalDateTime timestamp) {
        this(error, message, timestamp, null);
    }
}