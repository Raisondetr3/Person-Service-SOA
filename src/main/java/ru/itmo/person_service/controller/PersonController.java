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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
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
import ru.itmo.person_service.exception.PersonNotFoundException;
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
            description = """
        Retrieve a paginated list of persons with support for advanced filtering, sorting, and pagination.

        ## Filtering
        Use the `filter` parameter to apply conditions. Each filter is a string in the format:
        `fieldName[operator]=value`

        ### Available Operators:
        - `eq` – Equal (default if no operator specified)
        - `ne` – Not equal
        - `gt` – Greater than
        - `gte` – Greater than or equal
        - `lt` – Less than
        - `lte` – Less than or equal
        - `like` – Contains substring (case-insensitive)

        ### Supported Fields:
        - `name` (String)
        - `coordinates.x`, `coordinates.y` (Double)
        - `creationDate` (ISO DateTime)
        - `height` (Long, optional)
        - `weight` (Float)
        - `hairColor`, `eyeColor` (Enum: GREEN, BLUE, ORANGE, BROWN)
        - `nationality` (Enum: SPAIN, INDIA, VATICAN, SOUTH_KOREA, JAPAN)
        - `location.x` (Float, optional), `location.y` (Long, optional), `location.z` (Double, optional), `location.name` (String, optional)

        ### Examples:
        - `filter=name[like]=John`
        - `filter=weight[gt]=70`
        - `filter=hairColor=BLUE` (eq is default)
        - `filter=coordinates.x[gte]=-50&filter=coordinates.x[lte]=50`

        ## Sorting
        Use `sortBy=name` for ascending or `sortBy=-weight` for descending. Multiple sort fields allowed.

        ## Pagination
        Zero-based `page` and `size` control the result window.
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved persons"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters (page, size, sort)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorDTO.class),
                            examples = @ExampleObject(
                                    name = "Invalid Parameter",
                                    value = """
                                            {
                                                "error": "INVALID_REQUEST_PARAMETER",
                                                "message": "Page number must be non-negative",
                                                "timestamp": "2025-09-19T09:32:19.479Z",
                                                "path": "/persons"
                                            }
                                            """
                            ))
            )
    })
    @GetMapping
    public ResponseEntity<List<PersonResponseDTO>> getAllPersons(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size", example = "4")
            @RequestParam(defaultValue = "4") int size,

            @Parameter(
                    description = "Field(s) to sort by (e.g., 'name', '-weight', 'creationDate'). " +
                            "Prefix with '-' for descending order. Multiple fields supported.",
                    example = "name",
                    array = @ArraySchema(schema = @Schema(type = "string", example = "name"))
            )
            @RequestParam(required = false) String[] sortBy,

            @Parameter(
                    description = "Filter conditions in format: fieldName[operator]=value",
                    array = @ArraySchema(schema = @Schema(type = "string", example = "name[like]=John"))
            )
            @RequestParam(required = false) String[] filter) {

        Sort sort = buildSort(sortBy);

        Pageable pageable = PageRequest.of(page, size, sort);

        Map<String, String> filterParams = parseFilters(filter);

        Page<Person> personPage = personService.findAllWithFilters(filterParams, pageable);
        List<PersonResponseDTO> dtoList = personPage.getContent().stream()
                .map(PersonResponseDTO::create)
                .toList();

        System.out.println("=== PAGINATION INFO ===");
        System.out.println("Requested page: " + page + ", size: " + size);
        System.out.println("Total elements: " + personPage.getTotalElements());
        System.out.println("Total pages: " + personPage.getTotalPages());
        System.out.println("Current page: " + personPage.getNumber());
        System.out.println("Content size: " + dtoList.size());
        System.out.println("Has next: " + personPage.hasNext());
        System.out.println("Has previous: " + personPage.hasPrevious());
        System.out.println("=====================");

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", String.valueOf(personPage.getTotalElements()));
        headers.add("X-Total-Pages", String.valueOf(personPage.getTotalPages()));
        headers.add("X-Current-Page", String.valueOf(personPage.getNumber()));
        headers.add("X-Page-Size", String.valueOf(personPage.getSize()));
        headers.add("X-Has-Next", String.valueOf(personPage.hasNext()));
        headers.add("X-Has-Previous", String.valueOf(personPage.hasPrevious()));

        return ResponseEntity.ok().headers(headers).body(dtoList);
    }

    private Sort buildSort(String[] sortBy) {
        if (sortBy == null || sortBy.length == 0) {
            return Sort.unsorted();
        }
        List<Sort.Order> orders = new ArrayList<>();
        for (String field : sortBy) {
            if (field == null || field.isBlank()) continue;
            field = field.trim();
            if (field.startsWith("-")) {
                String fieldName = field.substring(1);
                orders.add(new Sort.Order(Sort.Direction.DESC, fieldName));
            } else {
                orders.add(new Sort.Order(Sort.Direction.ASC, field));
            }
        }
        return orders.isEmpty() ? Sort.unsorted() : Sort.by(orders);
    }

    private Map<String, String> parseFilters(String[] filter) {
        Map<String, String> filters = new HashMap<>();
        if (filter == null) {
            return filters;
        }
        for (String expr : filter) {
            if (expr == null || expr.isEmpty()) continue;

            int eqIndex = expr.indexOf('=');
            if (eqIndex <= 0) continue; // ключ должен быть до '=', и не пустой

            String key = expr.substring(0, eqIndex);
            String value = expr.substring(eqIndex + 1);
            filters.put(key, value);
        }
        return filters;
    }

    @Operation(
            summary = "Get person by ID",
            description = "Retrieve a single person by their unique identifier"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Person found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PersonResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Person not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorDTO.class),
                            examples = @ExampleObject(
                                    name = "Person Not Found",
                                    value = """
                                            {
                                                "error": "PERSON_NOT_FOUND",
                                                "message": "Person with ID 999 not found",
                                                "timestamp": "2025-09-19T09:32:19.479Z",
                                                "path": "/persons/999"
                                            }
                                            """
                            ))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid ID parameter",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorDTO.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Invalid ID Type",
                                            description = "ID is not a valid integer",
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
                                            name = "Invalid ID Value",
                                            description = "ID is negative or zero",
                                            value = """
                                                    {
                                                        "error": "INVALID_REQUEST_PARAMETER",
                                                        "message": "ID must be a positive number",
                                                        "timestamp": "2025-09-19T09:32:19.479Z",
                                                        "path": "/persons/-1"
                                                    }
                                                    """
                                    )
                            })
            )
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> getPersonById(
            @Parameter(description = "Person ID", required = true)
            @PathVariable Integer id) {
        Optional<Person> person = personService.findById(id);
        return person.map(p -> ResponseEntity.ok(PersonResponseDTO.create(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Create new person",
            description = "Add a new person to the collection"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Person created successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PersonResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Missing required fields",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorDTO.class),
                            examples = @ExampleObject(
                                    name = "Missing Required Fields",
                                    value = """
                                            {
                                                "error": "MISSING_REQUEST_BODY",
                                                "message": "Request body is required but missing",
                                                "timestamp": "2025-09-19T09:32:19.479Z",
                                                "path": "/persons"
                                            }
                                            """
                            ))
            ),
            @ApiResponse(responseCode = "422", description = "Invalid person data",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorDTO.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Validation Failed",
                                            description = "One or more fields are invalid",
                                            value = """
                                                    {
                                                        "status": 422,
                                                        "error": "VALIDATION_FAILED",
                                                        "message": "Request body validation failed",
                                                        "errors": {
                                                            "name": "Name cannot be null or empty",
                                                            "weight": "Weight must be greater than 0"
                                                        },
                                                        "timestamp": "2025-09-19T09:32:19.479Z",
                                                        "path": "/persons"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Invalid Enum Value",
                                            value = """
                                                    {
                                                        "error": "INVALID_ENUM_VALUE",
                                                        "message": "Invalid enum value provided. Check allowed values for enum fields.",
                                                        "timestamp": "2025-09-19T09:32:19.479Z",
                                                        "path": "/persons"
                                                    }
                                                    """
                                    )
                            })
            )
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
            @ApiResponse(responseCode = "200", description = "Person updated successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PersonResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Person not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorDTO.class),
                            examples = @ExampleObject(
                                    name = "Person Not Found",
                                    value = """
                                            {
                                                "error": "PERSON_NOT_FOUND",
                                                "message": "Person with ID 999 not found",
                                                "timestamp": "2025-09-19T09:32:19.479Z",
                                                "path": "/persons/999"
                                            }
                                            """
                            ))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid ID parameter",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorDTO.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Invalid ID Type",
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
                                            name = "Invalid ID Value",
                                            value = """
                                                    {
                                                        "error": "INVALID_REQUEST_PARAMETER",
                                                        "message": "ID must be a positive number",
                                                        "timestamp": "2025-09-19T09:32:19.479Z",
                                                        "path": "/persons/-1"
                                                    }
                                                    """
                                    )
                            })
            ),
            @ApiResponse(responseCode = "422", description = "Invalid person data in request body",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorDTO.class),
                            examples = @ExampleObject(
                                    name = "Validation Failed",
                                    value = """
                                            {
                                                "status": 422,
                                                "error": "VALIDATION_FAILED",
                                                "message": "Request body validation failed",
                                                "errors": {
                                                    "coordinates.x": "Coordinate X must be between -180 and 180",
                                                    "height": "Height must be greater than 0"
                                                },
                                                "timestamp": "2025-09-19T09:32:19.479Z",
                                                "path": "/persons/123"
                                            }
                                            """
                            ))
            )
    })
    @PutMapping("/{id}")
    public ResponseEntity<PersonResponseDTO> updatePerson(
            @Parameter(description = "Person ID", required = true)
            @PathVariable Integer id,
            @Parameter(description = "Updated person data", required = true)
            @Valid @RequestBody PersonRequestDTO personDTO) {

        try {
            Person updatedPerson = personService.update(id, personDTO.toPerson());
            return ResponseEntity.ok(PersonResponseDTO.create(updatedPerson));
        } catch (PersonNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
            summary = "Delete person by ID",
            description = "Remove a person from the collection by ID"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Person deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Person not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorDTO.class),
                            examples = @ExampleObject(
                                    name = "Person Not Found",
                                    value = """
                                            {
                                                "error": "PERSON_NOT_FOUND",
                                                "message": "Person with ID 999 not found",
                                                "timestamp": "2025-09-19T09:32:19.479Z",
                                                "path": "/persons/999"
                                            }
                                            """
                            ))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid ID parameter",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorDTO.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Invalid ID Type",
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
                                            name = "Invalid ID Value",
                                            value = """
                                                    {
                                                        "error": "INVALID_REQUEST_PARAMETER",
                                                        "message": "ID must be a positive number",
                                                        "timestamp": "2025-09-19T09:32:19.479Z",
                                                        "path": "/persons/0"
                                                    }
                                                    """
                                    )
                            })
            )
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
    @ApiResponse(responseCode = "200", description = "Count retrieved successfully")
    @GetMapping("/count")
    public ResponseEntity<Long> getPersonsCount() {
        long count = personService.count();
        return ResponseEntity.ok(count);
    }

    @Operation(
            summary = "Check if person exists",
            description = "Check whether a person with given ID exists in the collection"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Existence check completed"),
            @ApiResponse(responseCode = "400", description = "Invalid ID parameter",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorDTO.class),
                            examples = @ExampleObject(
                                    name = "Invalid ID Type",
                                    value = """
                                            {
                                                "error": "INVALID_PARAMETER_TYPE",
                                                "message": "Invalid value 'xyz' for parameter 'id'. Expected type: Integer",
                                                "timestamp": "2025-09-19T09:32:19.479Z",
                                                "path": "/persons/exists/xyz"
                                            }
                                            """
                            ))
            )
    })
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
            @ApiResponse(responseCode = "400", description = "Invalid hair color parameter",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorDTO.class),
                            examples = @ExampleObject(
                                    name = "Invalid Hair Color",
                                    value = """
                                            {
                                                "error": "INVALID_PARAMETER_TYPE",
                                                "message": "Invalid value 'PURPLE' for parameter 'hairColor'. Expected one of: [GREEN, BLUE, ORANGE, BROWN]",
                                                "timestamp": "2025-09-19T09:32:19.479Z",
                                                "path": "/persons/hair-color/PURPLE"
                                            }
                                            """
                            ))
            ),
            @ApiResponse(responseCode = "404", description = "No person found with specified hair color",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorDTO.class),
                            examples = @ExampleObject(
                                    name = "No Person Found",
                                    value = """
                                            {
                                                "error": "PERSON_NOT_FOUND",
                                                "message": "No person found with hair color ORANGE",
                                                "timestamp": "2025-09-19T09:32:19.479Z",
                                                "path": "/persons/hair-color/ORANGE"
                                            }
                                            """
                            ))
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
            @ApiResponse(responseCode = "200", description = "Person with longest name found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PersonResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "No persons found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorDTO.class),
                            examples = @ExampleObject(
                                    name = "No Persons in Database",
                                    value = """
                                            {
                                                "error": "NO_CONTENT",
                                                "message": "No persons found in the database",
                                                "timestamp": "2025-09-19T09:32:19.479Z",
                                                "path": "/persons/max-name"
                                            }
                                            """
                            ))
            )
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
            @ApiResponse(responseCode = "400", description = "Invalid nationality parameter",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorDTO.class),
                            examples = @ExampleObject(
                                    name = "Invalid Nationality",
                                    value = """
                                            {
                                                "error": "INVALID_PARAMETER_TYPE",
                                                "message": "Invalid value 'ATLANTIS' for parameter 'nationality'. Expected one of: [SPAIN, INDIA, VATICAN, SOUTH_KOREA, JAPAN]",
                                                "timestamp": "2025-09-19T09:32:19.479Z",
                                                "path": "/persons/nationality-less-than/ATLANTIS"
                                            }
                                            """
                            ))
            )
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
            description = "Get statistics of person count by hair color. Returns a map with all colors and their counts (0 if no persons with that color)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Hair Color Statistics",
                                    value = """
                                            {
                                                "GREEN": 5,
                                                "BLUE": 12,
                                                "ORANGE": 0,
                                                "BROWN": 8
                                            }
                                            """
                            ))
            )
    })
    @GetMapping("/statistics/hair-color")
    public ResponseEntity<Map<Color, Long>> getHairColorStatistics() {
        Map<Color, Long> stats = personService.getHairColorStatistics();
        return ResponseEntity.ok(stats);
    }

    @Operation(
            summary = "Get nationality statistics",
            description = "Get statistics of person count by nationality. Returns a map with all nationalities and their counts (0 if no persons with that nationality)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Nationality Statistics",
                                    value = """
                                            {
                                                "SPAIN": 3,
                                                "INDIA": 7,
                                                "VATICAN": 0,
                                                "SOUTH_KOREA": 2,
                                                "JAPAN": 10
                                            }
                                            """
                            ))
            )
    })
    @GetMapping("/statistics/nationality")
    public ResponseEntity<Map<Country, Long>> getNationalityStatistics() {
        Map<Country, Long> stats = personService.getNationalityStatistics();
        return ResponseEntity.ok(stats);
    }
}