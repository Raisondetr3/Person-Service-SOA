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
                    Retrieve paginated list of persons with advanced filtering capabilities.
                    
                    ## Filtering
                    Filters can be applied using the format: `fieldName[operator]=value`
                    
                    ### Available Operators:
                    - `eq` - Equal (default if no operator specified)
                    - `ne` - Not equal
                    - `gt` - Greater than
                    - `gte` - Greater than or equal
                    - `lt` - Less than
                    - `lte` - Less than or equal
                    - `like` - Contains substring (case-insensitive)
                    
                    ### Supported Fields:
                    - `id` - Person ID (Integer)
                    - `name` - Person name (String)
                    - `coordinates.x` - X coordinate (Double, -180 to 180)
                    - `coordinates.y` - Y coordinate (Double, -90 to 90)
                    - `creationDate` - Creation date (ISO DateTime)
                    - `height` - Height in cm (Long, optional)
                    - `weight` - Weight in kg (Float)
                    - `hairColor` - Hair color (Enum: GREEN, BLUE, ORANGE, BROWN)
                    - `eyeColor` - Eye color (Enum: GREEN, BLUE, ORANGE, BROWN)
                    - `nationality` - Nationality (Enum: SPAIN, INDIA, VATICAN, SOUTH_KOREA, JAPAN)
                    - `location.x` - Location X coordinate (Float, optional)
                    - `location.y` - Location Y coordinate (Long, optional)
                    - `location.z` - Location Z coordinate (Double, optional)
                    - `location.name` - Location name (String, optional)
                    
                    ### Filter Examples:
                    - `?name[like]=John` - Find persons whose name contains "John"
                    - `?weight[gt]=70` - Find persons heavier than 70 kg
                    - `?hairColor[eq]=BLUE` - Find persons with blue hair
                    - `?coordinates.x[gte]=-50&coordinates.x[lte]=50` - Find persons with X between -50 and 50
                    - `?creationDate[gt]=2024-01-01T00:00:00` - Find persons created after Jan 1, 2024
                    - `?nationality[ne]=SPAIN` - Find persons who are not from Spain
                    
                    ### Multiple Filters:
                    Filters can be combined and will be applied with AND logic.
                    Example: `?name[like]=John&weight[gt]=70&hairColor[eq]=BLUE`
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

            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") int size,

            @Parameter(description = "Field to sort by (e.g., 'name', 'weight', 'creationDate')",
                    example = "name",
                    array = @ArraySchema(schema = @Schema(type = "string", example = "name")))
            @RequestParam(required = false) String[] sortBy,

            @Parameter(hidden = true)
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