package ru.itmo.person_service.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.itmo.person_service.entity.Person;
import ru.itmo.person_service.entity.enums.Color;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonRepository extends JpaRepository<Person, Integer>, JpaSpecificationExecutor<Person> {
    Optional<Person> findByHairColor(Color hairColor);

    @Query("SELECT p FROM Person p WHERE p.name IS NOT NULL ORDER BY LENGTH(p.name) DESC")
    Optional<Person> findTopByNameIsNotNullOrderByLengthDesc();

    List<Person> findByNationalityBefore(String nationality);
}