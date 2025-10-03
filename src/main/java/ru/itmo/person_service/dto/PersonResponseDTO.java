package ru.itmo.person_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import ru.itmo.person_service.entity.Person;
import ru.itmo.person_service.entity.enums.Color;
import ru.itmo.person_service.entity.enums.Country;

import java.time.Instant;
import java.time.ZoneId;

public record PersonResponseDTO(
        Integer id,
        String name,
        CoordinatesDTO coordinates,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        Instant creationDate,
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
                person.getCreationDate().atZone(ZoneId.systemDefault()).toInstant(),
                person.getHeight(),
                person.getWeight(),
                person.getHairColor(),
                person.getEyeColor(),
                person.getNationality(),
                LocationDTO.create(person.getLocation())
        );
    }
}
