package ru.itmo.person_service.dto;

import ru.itmo.person_service.entity.Location;

public record LocationDTO(
        Integer x,
        Double y,
        Double z,
        String name
) {
    public static LocationDTO create(Location location) {
        return new LocationDTO(
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getName()
        );
    }

    public Location toLocation() {
        return new Location(
                x,
                y,
                z,
                name
        );
    }
}
