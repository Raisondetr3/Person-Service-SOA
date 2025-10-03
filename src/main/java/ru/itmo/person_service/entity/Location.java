package ru.itmo.person_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
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
public class Location {

    @Column(name = "location_x")
    private Integer x;

    @Column(name = "location_y")
    private Double y;

    @Column(name = "location_z")
    private Double z;

    @NotNull(message = "Location name cannot be null")
    @Column(name = "location_name", nullable = false, columnDefinition = "TEXT")
    private String name;

    @Override
    public String toString() {
        return String.format("Location{x=%d, y=%.2f, z=%.2f, name='%s'}", x, y, z, name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Location location = (Location) obj;

        return Objects.equals(x, location.x) &&
                Objects.equals(y, location.y) &&
                Objects.equals(z, location.z) &&
                Objects.equals(name, location.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z, name);
    }
}
