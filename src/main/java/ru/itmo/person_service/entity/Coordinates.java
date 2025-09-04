package ru.itmo.person_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Coordinates {

    @NotNull(message = "Coordinate X cannot be null")
    @Column(name = "coordinates_x", nullable = false)
    private Long x;

    @NotNull(message = "Coordinate Y cannot be null")
    @Max(value = 626, message = "Coordinate Y must not exceed 626")
    @Column(name = "coordinates_y", nullable = false)
    private Long y;

    @Override
    public String toString() {
        return String.format("Coordinates{x=%d, y=%d}", x, y);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Coordinates that = (Coordinates) obj;

        return Objects.equals(x, that.x) && Objects.equals(y, that.y);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}