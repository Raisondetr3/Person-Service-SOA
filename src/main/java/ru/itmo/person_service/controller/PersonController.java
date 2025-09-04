package ru.itmo.person_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.person_service.dto.PersonDTO;
import ru.itmo.person_service.entity.Person;
import ru.itmo.person_service.entity.enums.Color;
import ru.itmo.person_service.service.PersonService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/persons")
@RequiredArgsConstructor
public class PersonController {
    private final PersonService personService;
    
    @GetMapping
    public ResponseEntity<List<PersonDTO>> getAllPersons() {
        List<Person> persons = personService.findAll();
        return ResponseEntity.ok(persons.stream().map(PersonDTO::create).toList());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<PersonDTO> getPersonById(@PathVariable Integer id) {
        Optional<Person> person = personService.findById(id);
        return person.map(p -> ResponseEntity.ok(PersonDTO.create(p)))
                    .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<PersonDTO> createPerson(@RequestBody Person person) {
        Person savedPerson = personService.save(person);
        return ResponseEntity.ok(PersonDTO.create(savedPerson));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<PersonDTO> updatePerson(@PathVariable Integer id, @RequestBody Person person) {
        if (!personService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        person.setId(id);
        Person updatedPerson = personService.save(person);
        return ResponseEntity.ok(PersonDTO.create(updatedPerson));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePerson(@PathVariable Integer id) {
        if (!personService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        personService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
    
    @DeleteMapping
    public ResponseEntity<Void> deleteAllPersons() {
        personService.deleteAll();
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/count")
    public ResponseEntity<Long> getPersonsCount() {
        long count = personService.count();
        return ResponseEntity.ok(count);
    }
    
    @GetMapping("/exists/{id}")
    public ResponseEntity<Boolean> existsById(@PathVariable Integer id) {
        boolean exists = personService.existsById(id);
        return ResponseEntity.ok(exists);
    }

    @DeleteMapping("/hair-color/{hairColor}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteByHairColor(@PathVariable Color hairColor) {
        personService.deleteByHairColor(hairColor);
    }


    @GetMapping("/max-name")
    public ResponseEntity<PersonDTO> getPersonWithMaxName() {
        Optional<Person> person = personService.findPersonWithMaxName();
        return person.map(p -> ResponseEntity.ok(PersonDTO.create(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/nationality-less-than/{nationality}")
    public ResponseEntity<List<Person>> getPersonsByNationalityLessThan(@PathVariable String nationality) {
        List<Person> persons = personService.findByNationalityLessThan(nationality);
        return ResponseEntity.ok(persons);
    }
}