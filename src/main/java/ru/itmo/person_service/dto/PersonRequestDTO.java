package ru.itmo.person_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.itmo.person_service.entity.Person;
import ru.itmo.person_service.entity.enums.Color;
import ru.itmo.person_service.entity.enums.Country;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record PersonRequestDTO(
        String name,
        CoordinatesDTO coordinates,
        Long height,
        Float weight,
        Color hairColor,
        Color eyeColor,
        Country nationality,
        LocationDTO location
) {
    public Person toPerson() {
        return new Person(
                null,
                name,
                coordinates != null ? coordinates.toCoordinates() : null,
                null,
                height,
                weight,
                hairColor,
                eyeColor,
                nationality,
                location != null ? location.toLocation() : null
        );
    }
}