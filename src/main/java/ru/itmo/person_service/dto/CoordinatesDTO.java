package ru.itmo.person_service.dto;

import ru.itmo.person_service.entity.Coordinates;

public record CoordinatesDTO(
        Long x,
        Long y
) {
    public static CoordinatesDTO create(Coordinates coordinates) {
        return new CoordinatesDTO(
                coordinates.getX(),
                coordinates.getY()
        );
    }

    public Coordinates toCoordinates() {
        return new Coordinates(
                x,
                y
        );
    }
}