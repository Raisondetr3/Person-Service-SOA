package ru.itmo.person_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.person_service.dto.PersonDTO;
import ru.itmo.person_service.entity.Person;
import ru.itmo.person_service.entity.enums.Color;
import ru.itmo.person_service.entity.enums.Country;
import ru.itmo.person_service.service.PersonService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/persons")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Person Management", description = "REST API for managing person collection")
public class PersonController {

    private final PersonService personService;

    @Operation(
            summary = "Get all persons with filtering, sorting and pagination",
            description = "Retrieve paginated list of persons with optional filtering by any field and sorting"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved persons"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination or filter parameters")
    })
    @GetMapping
    public ResponseEntity<List<PersonDTO>> getAllPersons(
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Field to sort by")
            @RequestParam(required = false) String sortBy,  //TODO arrays
            @Parameter(description = "Sort direction (asc/desc)")
            @RequestParam(required = false, defaultValue = "asc") String sortDirection,
            @Parameter(description = "Filter by name")
            @RequestParam(required = false) String name,
            @Parameter(description = "Filter by hair color")
            @RequestParam(required = false) Color hairColor,
            @Parameter(description = "Filter by nationality")
            @RequestParam(required = false) Country nationality,
            @Parameter(description = "Filter by height")
            @RequestParam(required = false) Long height,
            @Parameter(description = "Filter by weight")
            @RequestParam(required = false) Float weight,
            @Parameter(description = "Filter by coordinates.x")
            @RequestParam(required = false) Long coordinatesX,
            @Parameter(description = "Filter by coordinates.y")
            @RequestParam(required = false) Long coordinatesY,
            @Parameter(description = "Filter by location.name")
            @RequestParam(required = false) String locationName) {

        Map<String, String> filterParams = new HashMap<>();
        if (name != null) filterParams.put("name", name);
        if (hairColor != null) filterParams.put("hairColor", hairColor.toString());
        if (nationality != null) filterParams.put("nationality", nationality.toString());
        if (height != null) filterParams.put("height", height.toString());
        if (weight != null) filterParams.put("weight", weight.toString());
        if (coordinatesX != null) filterParams.put("coordinates.x", coordinatesX.toString());
        if (coordinatesY != null) filterParams.put("coordinates.y", coordinatesY.toString());
        if (locationName != null) filterParams.put("location.name", locationName);

        Sort sort = Sort.unsorted();
        if (sortBy != null && !sortBy.isEmpty()) {
            Sort.Direction direction = sortDirection.equalsIgnoreCase("desc")
                    ? Sort.Direction.DESC
                    : Sort.Direction.ASC;
            sort = Sort.by(direction, sortBy);
        }

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Person> personPage = personService.findAllWithFilters(filterParams, pageable);

        Page<PersonDTO> personDtoPage = personPage.map(PersonDTO::create);

        return ResponseEntity.ok(personDtoPage.stream().toList());
    }

//    @GetMapping
//    public ResponseEntity<List<PersonDTO>> getAllPersons() {
//        List<Person> persons = personService.findAll();
//        return ResponseEntity.ok(persons.stream().map(PersonDTO::create).toList());
//    }

    @Operation(
            summary = "Get person by ID",
            description = "Retrieve a single person by their unique identifier"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Person found"),
            @ApiResponse(responseCode = "404", description = "Person not found"),
            @ApiResponse(responseCode = "400", description = "Invalid ID format")
    })
    @GetMapping("/{id}")
    public ResponseEntity<PersonDTO> getPersonById(
            @Parameter(description = "Person ID", required = true)
            @PathVariable Integer id) {

        Optional<Person> person = personService.findById(id);
        return person.map(p -> ResponseEntity.ok(PersonDTO.create(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Create new person",
            description = "Add a new person to the collection"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Person created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid person data"),
            @ApiResponse(responseCode = "409", description = "Person already exists")
    })
    @PostMapping
    public ResponseEntity<PersonDTO> createPerson(
            @Parameter(description = "Person data", required = true)
            @Valid @RequestBody PersonDTO personDTO) {

        Person savedPerson = personService.save(personDTO.toPerson());
        return ResponseEntity.status(HttpStatus.CREATED).body(PersonDTO.create(savedPerson));
    }

    @Operation(
            summary = "Update person",
            description = "Update an existing person by ID"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Person updated successfully"),
            @ApiResponse(responseCode = "404", description = "Person not found"),
            @ApiResponse(responseCode = "400", description = "Invalid person data")
    })
    @PutMapping("/{id}")
    public ResponseEntity<PersonDTO> updatePerson(
            @Parameter(description = "Person ID", required = true)
            @PathVariable Integer id,
            @Parameter(description = "Updated person data", required = true)
            @Valid @RequestBody Person person) {

        if (!personService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        person.setId(id);
        Person updatedPerson = personService.save(person);
        return ResponseEntity.ok(PersonDTO.create(updatedPerson));
    }

    @Operation(
            summary = "Delete person by ID",
            description = "Remove a person from the collection by ID"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Person deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Person not found"),
            @ApiResponse(responseCode = "400", description = "Invalid ID format")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePerson(
            @Parameter(description = "Person ID", required = true)
            @PathVariable Integer id) {

        personService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Get total count of persons",
            description = "Retrieve the total number of persons in the collection"
    )
    @GetMapping("/count")
    public ResponseEntity<Long> getPersonsCount() {
        long count = personService.count();
        return ResponseEntity.ok(count);
    }

    @Operation(
            summary = "Check if person exists",
            description = "Check whether a person with given ID exists in the collection"
    )
    @GetMapping("/exists/{id}")
    public ResponseEntity<Boolean> existsById(
            @Parameter(description = "Person ID", required = true)
            @PathVariable Integer id) {

        boolean exists = personService.existsById(id);
        return ResponseEntity.ok(exists);
    }

    @Operation(
            summary = "Delete person by hair color",
            description = "Delete one (any) person with the specified hair color"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Person deleted successfully"),
            @ApiResponse(responseCode = "404", description = "No person found with specified hair color"),
            @ApiResponse(responseCode = "400", description = "Invalid hair color")
    })
    @DeleteMapping("/hair-color/{hairColor}")
    public ResponseEntity<Void> deleteByHairColor(
            @Parameter(description = "Hair color to delete", required = true)
            @PathVariable Color hairColor) {

        personService.deleteByHairColor(hairColor);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Get person with maximum name",
            description = "Return one (any) person whose name field value is maximum"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Person with max name found"),
            @ApiResponse(responseCode = "404", description = "No persons found")
    })
    @GetMapping("/max-name")
    public ResponseEntity<PersonDTO> getPersonWithMaxName() {
        Optional<Person> person = personService.findPersonWithMaxName();
        return person.map(p -> ResponseEntity.ok(PersonDTO.create(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Get persons by nationality less than",
            description = "Return array of persons whose nationality field value is less than specified"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Persons found"),
            @ApiResponse(responseCode = "400", description = "Invalid nationality")
    })
    @GetMapping("/nationality-less-than/{nationality}")
    public ResponseEntity<List<PersonDTO>> getPersonsByNationalityLessThan(
            @Parameter(description = "Nationality to compare", required = true)
            @PathVariable Country nationality) {

        List<Person> persons = personService.findByNationalityLessThan(nationality);
        List<PersonDTO> personDtos = persons.stream()
                .map(PersonDTO::create)
                .collect(Collectors.toList());

        return ResponseEntity.ok(personDtos);
    }

    @Operation(
            summary = "Get hair color statistics",
            description = "Get statistics of person count by hair color"
    )
    @GetMapping("/statistics/hair-color")
    public ResponseEntity<Map<Color, Long>> getHairColorStatistics() {
        Map<Color, Long> stats = personService.getHairColorStatistics();
        return ResponseEntity.ok(stats);
    }

    @Operation(
            summary = "Get nationality statistics",
            description = "Get statistics of person count by nationality"
    )
    @GetMapping("/statistics/nationality")
    public ResponseEntity<Map<Country, Long>> getNationalityStatistics() {
        Map<Country, Long> stats = personService.getNationalityStatistics();
        return ResponseEntity.ok(stats);
    }
}