package ru.itmo.person_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kotlin.Pair;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.person_service.dto.ErrorDTO;
import ru.itmo.person_service.dto.PersonRequestDTO;
import ru.itmo.person_service.dto.PersonResponseDTO;
import ru.itmo.person_service.entity.Person;
import ru.itmo.person_service.entity.enums.Color;
import ru.itmo.person_service.entity.enums.Country;
import ru.itmo.person_service.service.PersonService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/persons")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Person Management", description = "REST API for managing person collection")
public class PersonController {

    private final PersonService personService;

    @Operation(
            summary = "Get all persons with advanced filtering, sorting and pagination",
            description = "Retrieve paginated list of persons with advanced filtering capabilities."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved persons"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination or filter parameters",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorDTO.class))
            ),
            @ApiResponse(responseCode = "422", description = "Invalid filter operator or value format",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorDTO.class))
            )
    })
    @GetMapping
    public ResponseEntity<List<PersonResponseDTO>> getAllPersons(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") int size,

            @Parameter(description = "Field to sort by", example = "name", array = @ArraySchema(schema = @Schema(type = "string", example = "name")))
            @RequestParam(required = false) String[] sortBy,

            @Parameter(description = "Filter fields", array = @ArraySchema(schema = @Schema(type = "string", example = "id[eq]=1")))
            @RequestParam(required = false) String[] filter,

            HttpServletRequest request) {

        Map<String, String> allParams = request.getParameterMap().entrySet().stream()
                .filter(entry -> entry.getValue().length > 0)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue()[0]
                ));

        Sort sort = Sort.unsorted();
        if (sortBy != null && !Arrays.stream(sortBy).toList().isEmpty()) {
            Sort.Direction direction = sortBy[0].equalsIgnoreCase("desc")
                    ? Sort.Direction.DESC
                    : Sort.Direction.ASC;
            sort = Sort.by(direction, sortBy[0]);
        }

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Person> personPage = personService.findAllWithFilters(allParams, pageable);
        Page<PersonResponseDTO> personDtoPage = personPage.map(PersonResponseDTO::create);

        return ResponseEntity.ok(personDtoPage.stream().toList());
    }

    @Operation(
            summary = "Get person by ID",
            description = "Retrieve a single person by their unique identifier"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Person found", content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PersonResponseDTO.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "Person not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorDTO.class),
                            examples = @ExampleObject(
                                    name = "Person Not Found",
                                    description = "No person exists with the specified ID",
                                    value = """
                                {
                                    "error": "PERSON_NOT_FOUND",
                                    "message": "Person with ID 999 not found",
                                    "timestamp": "2025-09-19T09:32:19.479Z",
                                    "path": "/persons/999"
                                }
                                """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid ID format",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorDTO.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Invalid ID - Not a Number",
                                            description = "ID parameter is not a valid integer",
                                            value = """
                                        {
                                            "error": "INVALID_PARAMETER_TYPE",
                                            "message": "Invalid value 'abc' for parameter 'id'. Expected type: Integer",
                                            "timestamp": "2025-09-19T09:32:19.479Z",
                                            "path": "/persons/abc"
                                        }
                                        """
                                    ),
                                    @ExampleObject(
                                            name = "Invalid ID - Negative Number",
                                            description = "ID parameter is negative or zero",
                                            value = """
                                        {
                                            "error": "INVALID_ARGUMENT",
                                            "message": "ID must be a positive number",
                                            "timestamp": "2025-09-19T09:32:19.479Z",
                                            "path": "/persons/-1"
                                        }
                                        """
                                    )
                            }
                    )
            ),
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> getPersonById(
            @Parameter(description = "Person ID", required = true)
            @PathVariable Integer id) {
        if (id <= 0) return ResponseEntity.badRequest().body("bad id");
        Optional<Person> person = personService.findById(id);
        return person.map(p -> ResponseEntity.ok(PersonResponseDTO.create(p)))
                .orElse(ResponseEntity.notFound().build());
    }


    @Operation(
            summary = "Create new person",
            description = "Add a new person to the collection"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Person created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid person data",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorDTO.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Invalid ID - Not a Number",
                                            description = "ID parameter is not a valid integer",
                                            value = """
                                        {
                                            "error": "INVALID_PARAMETER_TYPE",
                                            "message": "Invalid value 'abc' for parameter 'id'. Expected type: Integer",
                                            "timestamp": "2025-09-19T09:32:19.479Z",
                                            "path": "/persons/abc"
                                        }
                                        """
                                    ),
                                    @ExampleObject(
                                            name = "Invalid ID - Negative Number",
                                            description = "ID parameter is negative or zero",
                                            value = """
                                        {
                                            "error": "INVALID_ARGUMENT",
                                            "message": "ID must be a positive number",
                                            "timestamp": "2025-09-19T09:32:19.479Z",
                                            "path": "/persons/-1"
                                        }
                                        """
                                    )
                            }
                    )
            ),
            @ApiResponse(responseCode = "409", description = "Person already exists", content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorDTO.class)))
    })
    @PostMapping
    public ResponseEntity<PersonResponseDTO> createPerson(
            @Parameter(description = "Person data", required = true)
            @Valid @RequestBody PersonRequestDTO personDTO) {

        Person savedPerson = personService.save(personDTO.toPerson());
        return ResponseEntity.status(HttpStatus.CREATED).body(PersonResponseDTO.create(savedPerson));
    }

    @Operation(
            summary = "Update person",
            description = "Update an existing person by ID"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Person updated successfully"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Person not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorDTO.class),
                            examples = @ExampleObject(
                                    name = "Person Not Found",
                                    description = "No person exists with the specified ID",
                                    value = """
                                {
                                    "error": "PERSON_NOT_FOUND",
                                    "message": "Person with ID 999 not found",
                                    "timestamp": "2025-09-19T09:32:19.479Z",
                                    "path": "/persons/999"
                                }
                                """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid ID format",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorDTO.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Invalid ID - Not a Number",
                                            description = "ID parameter is not a valid integer",
                                            value = """
                                        {
                                            "error": "INVALID_PARAMETER_TYPE",
                                            "message": "Invalid value 'abc' for parameter 'id'. Expected type: Integer",
                                            "timestamp": "2025-09-19T09:32:19.479Z",
                                            "path": "/persons/abc"
                                        }
                                        """
                                    ),
                                    @ExampleObject(
                                            name = "Invalid ID - Negative Number",
                                            description = "ID parameter is negative or zero",
                                            value = """
                                        {
                                            "error": "INVALID_ARGUMENT",
                                            "message": "ID must be a positive number",
                                            "timestamp": "2025-09-19T09:32:19.479Z",
                                            "path": "/persons/-1"
                                        }
                                        """
                                    )
                            }
                    )
            ),
    })
    @PutMapping("/{id}")
    public ResponseEntity<PersonResponseDTO> updatePerson(
            @Parameter(description = "Person ID", required = true)
            @PathVariable Integer id,
            @Parameter(description = "Updated person data", required = true)
            @Valid @RequestBody PersonRequestDTO personDTO) {

        if (!personService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        Optional<Person> existingPerson = personService.findById(id);
        if (existingPerson.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Person updatedPerson = personDTO.toPerson();
        updatedPerson.setId(id);
        updatedPerson.setCreationDate(existingPerson.get().getCreationDate());

        Person savedPerson = personService.save(updatedPerson);
        return ResponseEntity.ok(PersonResponseDTO.create(savedPerson));
    }

    @Operation(
            summary = "Delete person by ID",
            description = "Remove a person from the collection by ID"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Person deleted successfully"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Person not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorDTO.class),
                            examples = @ExampleObject(
                                    name = "Person Not Found",
                                    description = "No person exists with the specified ID",
                                    value = """
                                {
                                    "error": "PERSON_NOT_FOUND",
                                    "message": "Person with ID 999 not found",
                                    "timestamp": "2025-09-19T09:32:19.479Z",
                                    "path": "/persons/999"
                                }
                                """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid ID format",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorDTO.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Invalid ID - Not a Number",
                                            description = "ID parameter is not a valid integer",
                                            value = """
                                        {
                                            "error": "INVALID_PARAMETER_TYPE",
                                            "message": "Invalid value 'abc' for parameter 'id'. Expected type: Integer",
                                            "timestamp": "2025-09-19T09:32:19.479Z",
                                            "path": "/persons/abc"
                                        }
                                        """
                                    ),
                                    @ExampleObject(
                                            name = "Invalid ID - Negative Number",
                                            description = "ID parameter is negative or zero",
                                            value = """
                                        {
                                            "error": "INVALID_ARGUMENT",
                                            "message": "ID must be a positive number",
                                            "timestamp": "2025-09-19T09:32:19.479Z",
                                            "path": "/persons/-1"
                                        }
                                        """
                                    )
                            }
                    )
            ),
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
            @ApiResponse(
                    responseCode = "204",
                    description = "Person deleted successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid hair color",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorDTO.class),
                            examples = @ExampleObject(
                                    name = "Invalid Hair Color",
                                    description = "Hair color parameter is invalid",
                                    value = """
                            {
                                "error": "INVALID_PARAMETER_TYPE",
                                "message": "Invalid value 'PURPLE' for parameter 'hairColor'. Expected one of: [GREEN, BLUE, ORANGE, BROWN]",
                                "timestamp": "2025-09-19T09:32:19.479Z",
                                "path": "/persons/hair-color/PURPLE"
                            }
                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No person found with specified hair color",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorDTO.class),
                            examples = @ExampleObject(
                                    name = "No Person Found",
                                    description = "No person exists with the specified hair color",
                                    value = """
                            {
                                "error": "PERSON_NOT_FOUND",
                                "message": "No person found with hair color ORANGE",
                                "timestamp": "2025-09-19T09:32:19.479Z",
                                "path": "/persons/hair-color/ORANGE"
                            }
                            """
                            )
                    )
            )
    })
    @DeleteMapping("/hair-color/{hairColor}")
    public ResponseEntity<Void> deleteByHairColor(
            @Parameter(description = "Hair color to delete", required = true)
            @PathVariable Color hairColor) {

        personService.deleteByHairColor(hairColor);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Get person with longest name",
            description = "Return one person whose name has the maximum length (most characters)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Person with longest name found"),
            @ApiResponse(responseCode = "404", description = "No persons found")
    })
    @GetMapping("/max-name")
    public ResponseEntity<PersonResponseDTO> getPersonWithMaxName() {
        Optional<Person> person = personService.findPersonWithMaxName();
        return person.map(p -> ResponseEntity.ok(PersonResponseDTO.create(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Get persons by nationality less than",
            description = "Return array of persons whose nationality field value is less than specified"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Persons found"),
            @ApiResponse(responseCode = "400", description = "Invalid nationality", content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = String.class)))
    })
    @GetMapping("/nationality-less-than/{nationality}")
    public ResponseEntity<List<PersonResponseDTO>> getPersonsByNationalityLessThan(
            @Parameter(description = "Nationality to compare", required = true)
            @PathVariable Country nationality) {

        List<Person> persons = personService.findByNationalityLessThan(nationality);
        List<PersonResponseDTO> personDtos = persons.stream()
                .map(PersonResponseDTO::create)
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