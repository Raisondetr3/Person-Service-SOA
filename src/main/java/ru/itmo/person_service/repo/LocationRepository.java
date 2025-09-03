package ru.itmo.person_service.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.itmo.person_service.entity.Location;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {

}