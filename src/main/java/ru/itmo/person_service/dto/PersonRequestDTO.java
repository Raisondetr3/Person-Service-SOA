package ru.itmo.person_service.dto;

import ru.itmo.person_service.entity.Person;
import ru.itmo.person_service.entity.enums.Color;
import ru.itmo.person_service.entity.enums.Country;

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
        if (weight == null) {

        }
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