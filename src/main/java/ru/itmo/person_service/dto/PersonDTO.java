package ru.itmo.person_service.dto;

import ru.itmo.person_service.entity.Coordinates;
import ru.itmo.person_service.entity.Location;
import ru.itmo.person_service.entity.Person;
import ru.itmo.person_service.entity.enums.Color;
import ru.itmo.person_service.entity.enums.Country;

import java.time.LocalDateTime;

public record PersonDTO(
        Integer id,
        String name,
        CoordinatesDTO coordinates,
        LocalDateTime creationDate,
        Long height,
        Float weight,
        Color hairColor,
        Country nationality,
        LocationDTO location
) {
    public static PersonDTO create(Person person) {
        return new PersonDTO(
                person.getId(),
                person.getName(),
                CoordinatesDTO.create(person.getCoordinates()),
                person.getCreationDate(),
                person.getHeight(),
                person.getWeight(),
                person.getHairColor(),
                person.getNationality(),
                LocationDTO.create(person.getLocation())
        );
    }

    public Person toPerson() {
        return new Person(
                id,
                name,
                coordinates.toCoordinates(),
                creationDate,
                height,
                weight,
                hairColor,
                nationality,
                location.toLocation()
        );
    }
}