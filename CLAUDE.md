# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Person Service is a Spring Boot REST and SOAP API for managing a person collection with advanced filtering, sorting, and pagination capabilities. The service uses PostgreSQL for persistence and runs on HTTPS (port 58123) with Jetty as the application server.

**Tech Stack:**
- Spring Boot 3.2.11 (Java 17)
- Spring Data JPA with Hibernate
- PostgreSQL database
- Jetty server (instead of Tomcat)
- Spring Security with HTTPS/SSL
- Spring Web Services (SOAP)
- Lombok for boilerplate reduction
- OpenAPI/Swagger documentation
- JAXB for SOAP XML binding

## Build & Development Commands

**Build the project:**
```bash
./mvnw clean package
```

**Run the application:**
```bash
./mvnw spring-boot:run
```

**Generate JAXB classes from XSD:**
```bash
./mvnw clean compile
```
This auto-generates SOAP DTOs in `ru.itmo.person_service.soap.dto` package from `src/main/resources/person-service.xsd`.

**Run with Docker Compose:**
```bash
docker network create checklist-network
docker-compose up -d
docker-compose logs -f person-service
```

**Access the APIs:**
- REST API (HTTPS): https://localhost:58123
- OpenAPI docs: https://localhost:58123/swagger-ui.html
- OpenAPI JSON: https://localhost:58123/v3/api-docs
- SOAP WSDL: https://localhost:58123/soap/PersonService.wsdl
- SOAP endpoint: https://localhost:58123/soap

## Architecture

### Package Structure

```
ru.itmo.person_service/
├── controller/        # REST endpoints (PersonController, FaviconController)
├── service/          # Business logic (PersonService with complex filtering)
├── repo/             # Spring Data JPA repositories
├── entity/           # JPA entities (Person, Coordinates, Location)
│   └── enums/        # Color and Country enums
├── dto/              # Data Transfer Objects (Request/Response DTOs)
├── exception/        # Custom exceptions and GlobalExceptionHandler
├── config/           # Configuration classes (Security, HTTPS, OpenAPI, SoapConfig)
└── soap/             # SOAP endpoint and auto-generated DTOs
    ├── PersonSoapEndpoint.java
    └── dto/          # JAXB-generated classes from XSD
```

### Core Domain Model

**Person Entity** (`entity/Person.java`)
- Main entity with auto-generated ID (sequence-based)
- Embedded objects: `Coordinates` (x, y) and `Location` (x, y, z, name)
- Enums: `Color` (hairColor, eyeColor) and `Country` (nationality)
- Validation via Jakarta Bean Validation annotations
- Auto-set `creationDate` on creation (never updated)

### Advanced Filtering System

The service implements a sophisticated filtering mechanism in `PersonService.java` using JPA Criteria API.

**Filter syntax:** `fieldName[operator]=value`

**Supported operators:**
- `eq` (default) - equal
- `ne` - not equal
- `gt`, `gte` - greater than (or equal)
- `lt`, `lte` - less than (or equal)
- `like` - substring match (case-insensitive)

**Nested field support:** Use dot notation (e.g., `coordinates.x[gte]=50`)

**Enum comparison:** The service supports ordinal-based enum comparisons using `buildEnumComparisonPredicate()` and `buildOrdinalComparisonPredicate()` methods. This allows filtering by enum order (e.g., `hairColor[lt]=ORANGE`).

**Implementation details:**
- Filter parsing: `parseFilterKey()` extracts field name and operator from `field[op]` syntax
- Specification building: `buildAdvancedSpecification()` creates JPA Criteria predicates
- Type conversion: `convertValueToType()` handles String to target type conversion
- Path resolution: `getFieldPath()` supports nested fields via dot notation

### Dual API Support (REST + SOAP)

The service exposes both REST and SOAP APIs with feature parity:

**REST API** (`controller/PersonController.java`)
- All endpoints under `/persons`
- Returns JSON with standard HTTP status codes
- Pagination headers: `X-Total-Count`, `X-Total-Pages`, `X-Current-Page`, etc.

**SOAP API** (`soap/PersonSoapEndpoint.java`)
- Endpoint: `/soap`
- WSDL contract at `/soap/PersonService.wsdl`
- Uses Spring Web Services framework
- Auto-generated DTOs from `person-service.xsd`
- Supports all operations: CRUD, filtering, statistics

**SOAP Implementation Notes:**
- XSD Schema: `src/main/resources/person-service.xsd` defines contract
- JAXB Generation: Maven plugin (`jaxb2-maven-plugin`) auto-generates Java classes
- Namespace: `http://itmo.ru/person-service`
- Message Factory: Uses SOAP 1.1 with `SaajSoapMessageFactory`
- Endpoint Mapping: `@PayloadRoot` annotations map operations to methods

### Exception Handling

Global exception handler (`exception/handler/GlobalExceptionHandler.java`) provides consistent error responses:
- `PersonNotFoundException` → 404
- `PersonValidationException` → 422 (validation errors with field-level details)
- `InvalidRequestParameterException` → 400
- `InvalidPersonDataException` → 422
- Type conversion errors → 400
- Generic exceptions → 500

All errors return standardized `ErrorDTO` or `ErrorsDto` with timestamp, path, and message.

## Configuration

**Environment Variables:**
The application uses a `.env` file (loaded manually, currently commented out in main class) for:
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `DB_DRIVER` - Database connection
- `KEYSTORE_PASSWORD`, `KEYSTORE_PATH`, `KEYSTORE_ALIAS` - SSL certificate

**SSL/HTTPS:**
- Configured via `config/HttpsConfig.java` and `config/SecurityConfig.java`
- Keystore: `src/main/resources/keystore.p12`
- HTTPS port: 58123, HTTP redirect port: 58080
- TLS 1.2 and 1.3 enabled

**JPA/Hibernate:**
- DDL auto mode: `update` (auto-create/update schema)
- Physical naming strategy: Standard (field names match column names)
- SQL logging enabled (`show-sql=true`)

**Database:**
- Local development: `jdbc:postgresql://localhost:5432/studs`
- Docker: `jdbc:postgresql://postgres:5432/persondb`
- Credentials stored in `application.properties` (local) or environment variables (Docker)

## Key API Endpoints

All REST endpoints are under `/persons`:

**CRUD Operations:**
- `GET /persons` - List with filtering, sorting, pagination (returns `X-Total-Count`, `X-Total-Pages`, etc. in headers)
- `GET /persons/{id}` - Get by ID
- `POST /persons` - Create new person
- `PUT /persons/{id}` - Update existing person
- `DELETE /persons/{id}` - Delete by ID

**Special Operations:**
- `GET /persons/count` - Total count
- `GET /persons/exists/{id}` - Check existence
- `GET /persons/max-name` - Person with longest name
- `GET /persons/nationality-less-than/{nationality}` - Filter by nationality ordinal
- `DELETE /persons/hair-color/{hairColor}` - Delete first person with specified hair color
- `GET /persons/statistics/hair-color` - Count by hair color (all enum values)
- `GET /persons/statistics/nationality` - Count by nationality (all enum values)

## Development Notes

**JAXB Code Generation:**
- XSD schema in `src/main/resources/person-service.xsd` is source of truth
- Run `./mvnw clean compile` to regenerate SOAP DTOs after XSD changes
- Generated classes go to `ru.itmo.person_service.soap.dto` package
- Never manually edit generated classes

**Database Migration:**
- Currently using Hibernate auto-update. For production, consider Flyway or Liquibase.
- Sequence name: `persons_id_seq`

**Lombok Usage:**
- All entities and services use Lombok annotations (`@Getter`, `@Setter`, `@RequiredArgsConstructor`, etc.)
- Ensure Lombok annotation processor is enabled in IDE

**Testing:**
- No test files currently exist (`src/test` is empty)
- When adding tests, use Spring Boot Test with `@WebMvcTest` for controllers and `@DataJpaTest` for repositories
- For SOAP testing, use Spring WS Test framework

**Security:**
- Basic Spring Security is configured but endpoints are currently permissive (permitAll)
- For production, implement proper authentication/authorization

**OpenAPI Documentation:**
- All endpoints have comprehensive `@Operation` and `@ApiResponse` annotations
- Example responses included for error scenarios
- Server URL configured in `PersonServiceApplication` (`@OpenAPIDefinition`)

## Common Patterns

**DTO Conversion:**
- Request: `PersonRequestDTO.toPerson()` converts DTO to entity
- Response: `PersonResponseDTO.create(Person)` converts entity to DTO
- SOAP: `PersonSoapEndpoint` has private methods for SOAP ↔ entity conversion

**Validation:**
- Request validation: `@Valid` annotation on controller parameters triggers Jakarta Validation
- Service-level validation: `validatePerson()` and `validateId()` methods provide additional business logic validation
- Custom exceptions thrown for specific validation failures

**Logging:**
- SLF4J with `@Slf4j` annotation throughout
- Log levels: info for normal operations, warn for validation issues, error for exceptions

**Transactional Boundaries:**
- Service methods that modify data are annotated with `@Transactional`
- Read-only operations use `@Transactional(readOnly = true)` at class level
