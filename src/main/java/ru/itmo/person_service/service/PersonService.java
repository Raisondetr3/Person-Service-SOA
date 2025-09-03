package ru.itmo.person_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.itmo.person_service.entity.Person;
import ru.itmo.person_service.repo.PersonRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PersonService {
    private final PersonRepository personRepository;

    public List<Person> findAll() {
        return personRepository.findAll();
    }

    public Optional<Person> findById(Integer id) {
        return personRepository.findById(id);
    }

    public Person save(Person person) {
        return personRepository.save(person);
    }

    public void deleteById(Integer id) {
        personRepository.deleteById(id);
    }

    public boolean existsById(Integer id) {
        return personRepository.existsById(id);
    }

    public long count() {
        return personRepository.count();
    }

    public void deleteAll() {
        personRepository.deleteAll();
    }

    public void deleteByHairColor(String hairColor) {
        Optional<Person> person = personRepository.findByHairColor(hairColor);
        person.ifPresent(p -> personRepository.deleteById(p.getId()));
    }

    public Optional<Person> findPersonWithMaxName() {
        return personRepository.findTopByNameIsNotNullOrderByLengthDesc();
    }

    public List<Person> findByNationalityLessThan(String nationality) {
        return personRepository.findByNationalityBefore(nationality);
    }
}
