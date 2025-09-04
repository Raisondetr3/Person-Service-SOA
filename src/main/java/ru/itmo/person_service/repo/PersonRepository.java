package ru.itmo.person_service.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.itmo.person_service.entity.Person;
import ru.itmo.person_service.entity.enums.Color;
import ru.itmo.person_service.entity.enums.Country;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonRepository extends JpaRepository<Person, Integer>, JpaSpecificationExecutor<Person> {

    Optional<Person> findFirstByHairColor(Color hairColor);

    @Query("SELECT p FROM Person p WHERE p.name IS NOT NULL ORDER BY LENGTH(p.name) DESC LIMIT 1")
    Optional<Person> findPersonWithMaxName();

    @Query("SELECT p FROM Person p WHERE CAST(p.nationality AS int) < CAST(:nationality AS int)")
    List<Person> findByNationalityLessThan(@Param("nationality") Country nationality);
}