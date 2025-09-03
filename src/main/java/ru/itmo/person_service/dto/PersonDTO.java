package ru.itmo.person_service.dto;

import ru.itmo.person_service.entity.Coordinates;
import ru.itmo.person_service.entity.Location;
import ru.itmo.person_service.entity.Person;
import ru.itmo.person_service.entity.enums.Color;
import ru.itmo.person_service.entity.enums.Country;

public record PersonDTO(
        int id,
        //Значение поля должно быть больше 0, Значение этого поля должно быть уникальным, Значение этого поля должно генерироваться автоматически
        String name, //Поле не может быть null, Строка не может быть пустой
        Coordinates coordinates, //Поле не может быть null
        java.time.LocalDateTime creationDate,
        //Поле не может быть null, Значение этого поля должно генерироваться автоматически
        Long height, //Поле может быть null, Значение поля должно быть больше 0
        float weight, //Значение поля должно быть больше 0
        Color hairColor, //Поле не может быть null
        Country nationality, //Поле не может быть null
        Location location //Поле может быть null
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
}