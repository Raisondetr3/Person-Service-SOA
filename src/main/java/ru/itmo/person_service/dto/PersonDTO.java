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
        Coordinates coordinates,
        LocalDateTime creationDate,
        Long height,
        Float weight,
        Color hairColor,
        Country nationality,
        Location location
) {
    public static PersonDTO create(Person person) {
        return new PersonDTO(
                person.getId(),
                person.getName(),
                person.getCoordinates(),
                person.getCreationDate(),
                person.getHeight(),
                person.getWeight(),
                person.getHairColor(),
                person.getNationality(),
                person.getLocation()
        );
    }

    public Person toPerson() {
        return new Person(
                id,
                name,
                coordinates,
                creationDate,
                height,
                weight,
                hairColor,
                nationality,
                location
        );
    }
}