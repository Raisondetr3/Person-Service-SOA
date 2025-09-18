package ru.itmo.person_service.dto;

import ru.itmo.person_service.entity.Person;
import ru.itmo.person_service.entity.enums.Color;
import ru.itmo.person_service.entity.enums.Country;

import java.time.LocalDateTime;

public record PersonResponseDTO(
        Integer id,
        String name,
        CoordinatesDTO coordinates,
        LocalDateTime creationDate,
        Long height,
        Float weight,
        Color hairColor,
        Color eyeColor,
        Country nationality,
        LocationDTO location
) {
    public static PersonResponseDTO create(Person person) {
        return new PersonResponseDTO(
                person.getId(),
                person.getName(),
                CoordinatesDTO.create(person.getCoordinates()),
                person.getCreationDate(),
                person.getHeight(),
                person.getWeight(),
                person.getHairColor(),
                person.getEyeColor(),
                person.getNationality(),
                LocationDTO.create(person.getLocation())
        );
    }
}
