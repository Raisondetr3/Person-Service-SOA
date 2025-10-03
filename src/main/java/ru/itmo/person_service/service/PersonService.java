package ru.itmo.person_service.service;

import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.person_service.dto.PersonRequestDTO;
import ru.itmo.person_service.entity.Coordinates;
import ru.itmo.person_service.entity.Person;
import ru.itmo.person_service.entity.Location;
import ru.itmo.person_service.entity.enums.Color;
import ru.itmo.person_service.entity.enums.Country;
import ru.itmo.person_service.exception.InvalidPersonDataException;
import ru.itmo.person_service.exception.PersonNotFoundException;
import ru.itmo.person_service.exception.PersonValidationException;
import ru.itmo.person_service.repo.PersonRepository;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PersonService {

    private final PersonRepository personRepository;

    // Поддерживаемые операторы фильтрации
    private static final Map<String, String> FILTER_OPERATORS = Map.of(
            "gt", ">",   // greater than
            "gte", ">=", // greater than or equal
            "lt", "<",   // less than
            "lte", "<=", // less than or equal
            "eq", "=",   // equal
            "ne", "!=",  // not equal
            "like", "~"  // contains substring
    );

    public Page<Person> findAllWithFilters(Map<String, String> filterParams, Pageable pageable) {
        Map<String, String> cleanedFilters = removeSystemParams(filterParams);
        Specification<Person> spec = buildAdvancedSpecification(cleanedFilters);
        return personRepository.findAll(spec, pageable);
    }

    private Map<String, String> removeSystemParams(Map<String, String> filterParams) {
        Map<String, String> cleaned = new HashMap<>(filterParams);
        cleaned.remove("page");
        cleaned.remove("size");
        cleaned.remove("sortBy");
        cleaned.remove("sortDirection");
        return cleaned;
    }

    private Specification<Person> buildAdvancedSpecification(Map<String, String> filters) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            filters.forEach((filterKey, value) -> {
                if (value == null || value.trim().isEmpty()) return;

                try {
                    FilterConfig filterConfig = parseFilterKey(filterKey);
                    Path<Object> path = getFieldPath(root, filterConfig.fieldName);

                    if (path == null) {
                        log.warn("Field '{}' not found in Person entity", filterConfig.fieldName);
                        return;
                    }

                    Predicate predicate = buildPredicateForOperator(
                            cb, path, filterConfig.operator, value, path.getJavaType());

                    if (predicate != null) {
                        predicates.add(predicate);
                    }

                } catch (Exception e) {
                    log.warn("Error processing filter '{}' with value '{}': {}",
                            filterKey, value, e.getMessage());
                }
            });

            return predicates.isEmpty() ?
                    cb.conjunction() :
                    cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private FilterConfig parseFilterKey(String filterKey) {
        if (!filterKey.contains("[")) {
            return new FilterConfig(filterKey, "eq");
        }

        int bracketStart = filterKey.indexOf('[');
        int bracketEnd = filterKey.indexOf(']');

        if (bracketStart == -1 || bracketEnd == -1 || bracketEnd <= bracketStart) {
            return new FilterConfig(filterKey, "eq");
        }

        String fieldName = filterKey.substring(0, bracketStart);
        String operator = filterKey.substring(bracketStart + 1, bracketEnd);

        if (!FILTER_OPERATORS.containsKey(operator)) {
            log.warn("Unsupported filter operator: {}", operator);
            return new FilterConfig(fieldName, "eq");
        }

        return new FilterConfig(fieldName, operator);
    }

    private Predicate buildPredicateForOperator(CriteriaBuilder cb, Path<Object> path,
                                                String operator, String value, Class<?> fieldType) {

        // Специальная обработка для enum-типов при операторах сравнения
        if (Enum.class.isAssignableFrom(fieldType) &&
                (operator.equals("lt") || operator.equals("lte") ||
                        operator.equals("gt") || operator.equals("gte"))) {
            return buildEnumComparisonPredicate(cb, path, operator, value, fieldType);
        }

        switch (operator) {
            case "like":
                return buildLikePredicate(cb, path, value, fieldType);
            case "eq":
                return buildEqualPredicate(cb, path, value, fieldType);
            case "ne":
                return buildNotEqualPredicate(cb, path, value, fieldType);
            case "gt":
                return buildGreaterThanPredicate(cb, path, value, fieldType);
            case "gte":
                return buildGreaterThanOrEqualPredicate(cb, path, value, fieldType);
            case "lt":
                return buildLessThanPredicate(cb, path, value, fieldType);
            case "lte":
                return buildLessThanOrEqualPredicate(cb, path, value, fieldType);
            default:
                log.warn("Unknown operator: {}", operator);
                return null;
        }
    }

    private Predicate buildEnumComparisonPredicate(CriteriaBuilder cb, Path<Object> path,
                                                   String operator, String value, Class<?> enumType) {
        try {
            Integer ordinalValue = null;

            // Пробуем преобразовать значение в число
            try {
                ordinalValue = Integer.valueOf(value);
            } catch (NumberFormatException e) {
                // Если не число, пробуем преобразовать в enum
                Enum<?> enumValue = convertToEnum(value, enumType);
                if (enumValue != null) {
                    ordinalValue = enumValue.ordinal();
                } else {
                    log.warn("Invalid enum value for comparison: {}", value);
                    return null;
                }
            }

            return buildOrdinalComparisonPredicate(cb, path, operator, ordinalValue, enumType);

        } catch (Exception e) {
            log.warn("Error comparing enum values: {}", e.getMessage());
            return null;
        }
    }

    private Predicate buildOrdinalComparisonPredicate(CriteriaBuilder cb, Path<Object> path,
                                                      String operator, Integer ordinal, Class<?> enumType) {
        Expression<String> stringPath = path.as(String.class);

        // Динамически создаем маппинг на основе типа enum
        CriteriaBuilder.SimpleCase<String, Integer> selectCase = cb.selectCase(stringPath);

        // Получаем все константы enum и создаем маппинг
        if (enumType.isEnum()) {
            Enum<?>[] enumConstants = (Enum<?>[]) enumType.getEnumConstants();
            for (Enum<?> enumConstant : enumConstants) {
                selectCase = selectCase.when(enumConstant.name(), enumConstant.ordinal());
            }
        }

        Expression<Integer> ordinalExpression = selectCase.otherwise(-1);

        switch (operator) {
            case "lt":
                return cb.lessThan(ordinalExpression, ordinal);
            case "lte":
                return cb.lessThanOrEqualTo(ordinalExpression, ordinal);
            case "gt":
                return cb.greaterThan(ordinalExpression, ordinal);
            case "gte":
                return cb.greaterThanOrEqualTo(ordinalExpression, ordinal);
            default:
                return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Predicate buildLikePredicate(CriteriaBuilder cb, Path<Object> path, String value, Class<?> fieldType) {
        if (String.class.isAssignableFrom(fieldType)) {
            Expression<String> stringPath = path.as(String.class);
            return cb.like(cb.lower(stringPath), "%" + value.toLowerCase() + "%");
        } else if (Enum.class.isAssignableFrom(fieldType)) {
            // Для enum делаем like по строковому представлению
            Expression<String> stringPath = path.as(String.class);
            return cb.like(cb.lower(stringPath), "%" + value.toUpperCase() + "%");
        } else {
            Expression<String> stringExpression = cb.function("CAST", String.class, path);
            return cb.like(cb.lower(stringExpression), "%" + value.toLowerCase() + "%");
        }
    }

    private Predicate buildEqualPredicate(CriteriaBuilder cb, Path<Object> path, String value, Class<?> fieldType) {
        Object convertedValue = convertValueToType(value, fieldType);
        return convertedValue != null ? cb.equal(path, convertedValue) : null;
    }

    private Predicate buildNotEqualPredicate(CriteriaBuilder cb, Path<Object> path, String value, Class<?> fieldType) {
        Object convertedValue = convertValueToType(value, fieldType);
        return convertedValue != null ? cb.notEqual(path, convertedValue) : null;
    }

    @SuppressWarnings("unchecked")
    private Predicate buildGreaterThanPredicate(CriteriaBuilder cb, Path<Object> path, String value, Class<?> fieldType) {
        // Enum обрабатываются отдельно через buildEnumComparisonPredicate
        if (isComparable(fieldType) && !Enum.class.isAssignableFrom(fieldType)) {
            Comparable<Object> convertedValue = (Comparable<Object>) convertValueToType(value, fieldType);
            if (convertedValue != null) {
                Expression<? extends Comparable> comparableExpression = path.as((Class<? extends Comparable>) fieldType);
                return cb.greaterThan((Expression<Comparable>) comparableExpression, convertedValue);
            }
        }
        log.warn("Cannot apply 'greater than' operator to field type: {}", fieldType.getSimpleName());
        return null;
    }

    @SuppressWarnings("unchecked")
    private Predicate buildGreaterThanOrEqualPredicate(CriteriaBuilder cb, Path<Object> path, String value, Class<?> fieldType) {
        // Enum обрабатываются отдельно через buildEnumComparisonPredicate
        if (isComparable(fieldType) && !Enum.class.isAssignableFrom(fieldType)) {
            Comparable<Object> convertedValue = (Comparable<Object>) convertValueToType(value, fieldType);
            if (convertedValue != null) {
                Expression<? extends Comparable> comparableExpression = path.as((Class<? extends Comparable>) fieldType);
                return cb.greaterThanOrEqualTo((Expression<Comparable>) comparableExpression, convertedValue);
            }
        }
        log.warn("Cannot apply 'greater than or equal' operator to field type: {}", fieldType.getSimpleName());
        return null;
    }

    @SuppressWarnings("unchecked")
    private Predicate buildLessThanPredicate(CriteriaBuilder cb, Path<Object> path, String value, Class<?> fieldType) {
        // Enum обрабатываются отдельно через buildEnumComparisonPredicate
        if (isComparable(fieldType) && !Enum.class.isAssignableFrom(fieldType)) {
            Comparable<Object> convertedValue = (Comparable<Object>) convertValueToType(value, fieldType);
            if (convertedValue != null) {
                Expression<? extends Comparable> comparableExpression = path.as((Class<? extends Comparable>) fieldType);
                return cb.lessThan((Expression<Comparable>) comparableExpression, convertedValue);
            }
        }
        log.warn("Cannot apply 'less than' operator to field type: {}", fieldType.getSimpleName());
        return null;
    }

    @SuppressWarnings("unchecked")
    private Predicate buildLessThanOrEqualPredicate(CriteriaBuilder cb, Path<Object> path, String value, Class<?> fieldType) {
        // Enum обрабатываются отдельно через buildEnumComparisonPredicate
        if (isComparable(fieldType) && !Enum.class.isAssignableFrom(fieldType)) {
            Comparable<Object> convertedValue = (Comparable<Object>) convertValueToType(value, fieldType);
            if (convertedValue != null) {
                Expression<? extends Comparable> comparableExpression = path.as((Class<? extends Comparable>) fieldType);
                return cb.lessThanOrEqualTo((Expression<Comparable>) comparableExpression, convertedValue);
            }
        }
        log.warn("Cannot apply 'less than or equal' operator to field type: {}", fieldType.getSimpleName());
        return null;
    }

    private boolean isComparable(Class<?> type) {
        return Number.class.isAssignableFrom(type) ||
                type.isPrimitive() ||
                String.class.isAssignableFrom(type) ||
                Enum.class.isAssignableFrom(type) ||
                Comparable.class.isAssignableFrom(type);
    }

    private Object convertValueToType(String value, Class<?> targetType) {
        try {
            if (String.class.isAssignableFrom(targetType)) {
                return value;
            } else if (targetType == Long.class || targetType == long.class) {
                return Long.valueOf(value);
            } else if (targetType == Integer.class || targetType == int.class) {
                return Integer.valueOf(value);
            } else if (targetType == Double.class || targetType == double.class) {
                return Double.valueOf(value);
            } else if (targetType == Float.class || targetType == float.class) {
                return Float.valueOf(value);
            } else if (targetType == Boolean.class || targetType == boolean.class) {
                return Boolean.valueOf(value);
            } else if (Enum.class.isAssignableFrom(targetType)) {
                return convertToEnum(value, targetType);
            } else if (targetType == LocalDateTime.class) {
                return LocalDateTime.parse(value);
            }

            return value;
        } catch (Exception e) {
            log.warn("Failed to convert value '{}' to type {}: {}", value, targetType.getSimpleName(), e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Enum<?> convertToEnum(String value, Class<?> enumType) {
        try {
            return Enum.valueOf((Class<Enum>) enumType, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid enum value '{}' for type {}", value, enumType.getSimpleName());
            return null;
        }
    }

    private Path<Object> getFieldPath(Root<Person> root, String field) {
        try {
            if (field.contains(".")) {
                String[] parts = field.split("\\.");
                Path<Object> path = root.get(parts[0]);
                for (int i = 1; i < parts.length; i++) {
                    path = path.get(parts[i]);
                }
                return path;
            } else {
                return root.get(field);
            }
        } catch (IllegalArgumentException e) {
            log.warn("Field '{}' not found in entity", field);
            return null;
        }
    }

    private static class FilterConfig {
        final String fieldName;
        final String operator;

        FilterConfig(String fieldName, String operator) {
            this.fieldName = fieldName;
            this.operator = operator;
        }
    }


    public Optional<Person> findById(Integer id) {
        validateId(id);
        return personRepository.findById(id);
    }

    @Transactional
    public Person save(PersonRequestDTO personDto) {
        validatePerson(personDto);
        Person person = personDto.toPerson();

        if (person.getId() == null || person.getId() == 0) {
            person.setCreationDate(LocalDateTime.now());
        }

        try {
            log.info("Saving person: {}", person.getName());
            return personRepository.save(person);
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while saving person: {}", e.getMessage());

            if (e.getMessage().contains("not-null constraint") || e.getMessage().contains("null value")) {
                throw new PersonValidationException("Required fields are missing or invalid");
            } else if (e.getMessage().contains("foreign key constraint")) {
                throw new PersonValidationException("Referenced entity does not exist");
            }

            throw new InvalidPersonDataException("Data integrity violation: " + extractUserFriendlyMessage(e));
        } catch (Exception e) {
            log.error("Unexpected error while saving person: {}", e.getMessage(), e);
            throw new InvalidPersonDataException("Unable to save person: " + e.getMessage());
        }
    }

    @Transactional
    public Person update(Integer id, PersonRequestDTO personDto) {
        validateId(id);

        Person existingPerson = personRepository.findById(id)
                .orElseThrow(() -> new PersonNotFoundException(id));

        validatePerson(personDto);

        Person personData = personDto.toPerson();

        personData.setId(id);
        personData.setCreationDate(existingPerson.getCreationDate());

        try {
            log.info("Updating person with ID {}: {}", id, personData.getName());
            return personRepository.save(personData);
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while updating person: {}", e.getMessage());

            if (e.getMessage().contains("duplicate") || e.getMessage().contains("unique")) {
                throw new PersonValidationException("Person with these attributes already exists");
            } else if (e.getMessage().contains("not-null constraint") || e.getMessage().contains("null value")) {
                throw new PersonValidationException("Required fields are missing or invalid");
            } else if (e.getMessage().contains("foreign key constraint")) {
                throw new PersonValidationException("Referenced entity does not exist");
            }

            throw new InvalidPersonDataException("Data integrity violation: " + extractUserFriendlyMessage(e));
        } catch (Exception e) {
            log.error("Unexpected error while updating person: {}", e.getMessage(), e);
            throw new InvalidPersonDataException("Unable to update person: " + e.getMessage());
        }
    }

    private String extractUserFriendlyMessage(DataIntegrityViolationException e) {
        String message = e.getMessage();

        if (message.contains("null value in column \"name\"")) {
            return "Name is required";
        } else if (message.contains("null value in column \"coordinates")) {
            return "Coordinates are required";
        } else if (message.contains("null value in column \"hair_color\"")) {
            return "Hair color is required";
        } else if (message.contains("null value in column \"eye_color\"")) {
            return "Eye color is required";
        } else if (message.contains("null value in column \"nationality\"")) {
            return "Nationality is required";
        } else if (message.contains("null value in column \"weight\"")) {
            return "Weight is required";
        }

        return "Invalid data provided";
    }

    @Transactional
    public void deleteById(Integer id) {
        validateId(id);

        if (!personRepository.existsById(id)) {
            throw new PersonNotFoundException(id);
        }

        log.info("Deleting person with ID: {}", id);
        personRepository.deleteById(id);
    }

    public boolean existsById(Integer id) {
        return id != null && id > 0 && personRepository.existsById(id);
    }

    public long count() {
        return personRepository.count();
    }

    @Transactional
    public Optional<Person> deleteByHairColor(Color hairColor) {
        if (hairColor == null) {
            throw new InvalidPersonDataException("Hair color cannot be null");
        }

        Optional<Person> person = personRepository.findFirstByHairColor(hairColor);
        if (person.isPresent()) {
            log.info("Deleting person with hair color {}: {}", hairColor, person.get().getName());
            personRepository.deleteById(person.get().getId());
            return person;
        } else {
            log.info("No person found with hair color: {}", hairColor);
            throw new PersonNotFoundException("Person with hair color " + hairColor + " not found");
        }
    }

    public Optional<Person> findPersonWithMaxName() {
        Optional<Person> result = personRepository.findPersonWithMaxName();
        if (result.isPresent()) {
            log.info("Found person with max name length: {} (length: {})",
                    result.get().getName(), result.get().getName().length());
        } else {
            log.info("No persons found with non-null names");
        }
        return result;
    }

    public List<Person> findByNationalityLessThan(Country nationality) {
        if (nationality == null) {
            throw new InvalidPersonDataException("Nationality cannot be null");
        }

        List<Person> result = personRepository.findByNationalityLessThan(nationality);
        log.info("Found {} persons with nationality less than {}", result.size(), nationality);
        return result;
    }

    public double calculateHairColorPercentage(Color hairColor) {
        if (hairColor == null) {
            throw new InvalidPersonDataException("Hair color cannot be null");
        }

        long totalCount = personRepository.countAllPersons();
        if (totalCount == 0) {
            log.info("No persons found in database");
            return 0.0;
        }

        long hairColorCount = personRepository.countByHairColor(hairColor);
        double percentage = ((double) hairColorCount / totalCount) * 100;

        log.info("Hair color {} statistics: {} out of {} persons ({}%)",
                hairColor, hairColorCount, totalCount, String.format("%.2f", percentage));

        return percentage;
    }

    public long calculateNationalityEyeColorCount(Country nationality, Color eyeColor) {
        if (nationality == null) {
            throw new InvalidPersonDataException("Nationality cannot be null");
        }
        if (eyeColor == null) {
            throw new InvalidPersonDataException("Eye color cannot be null");
        }

        long count = personRepository.countByEyeColorAndNationality(eyeColor, nationality);

        log.info("Found {} persons with nationality {} and eye color {}",
                count, nationality, eyeColor);

        return count;
    }

    private void validateId(Integer id) {
        if (id == null || id <= 0) {
            throw new PersonValidationException(
                    "id",
                    "ID must be a positive number",
                    PersonValidationException.ValidationSource.URL_PARAMETER
            );
        }
    }

    private void validatePerson(PersonRequestDTO person) {
        if (person == null) {
            throw new InvalidPersonDataException("Person cannot be null");
        }

        Map<String, String> errors = new HashMap<>();

        if (person.name() == null || person.name().trim().isEmpty()) {
            errors.put("name", "Name is required and cannot be empty");
        } else if (person.name().trim().length() > 255) {
            errors.put("name", "Name cannot exceed 255 characters");
        }

        if (person.weight() == null) {
            errors.put("weight", "Weight is required");
        }
        else if (person.weight() <= 0) {
            errors.put("weight", "Weight must be greater than 0");
        } else if (person.weight() > 1000) {
            errors.put("weight", "Weight cannot exceed 1000 kg");
        }

        if (person.height() != null) {
            if (person.height() <= 0) {
                errors.put("height", "Height must be greater than 0");
            } else if (person.height() > 300) {
                errors.put("height", "Height cannot exceed 300 cm");
            }
        }

        if (person.hairColor() == null) {
            errors.put("hairColor", "Hair color is required");
        }

        if (person.eyeColor() == null) {
            errors.put("eyeColor", "Eye color is required");
        }

        if (person.nationality() == null) {
            errors.put("nationality", "Nationality is required");
        }

        if (!errors.isEmpty()) {
            throw new PersonValidationException(errors);
        }
    }

    private void validateCoordinates(Coordinates coordinates, Map<String, String> errors) {
        if (coordinates.getX() == null) {
            errors.put("coordinates.x", "Coordinate X is required");
        }

        if (coordinates.getY() == null) {
            errors.put("coordinates.y", "Coordinate Y is required");
        }
    }

    private void validateLocation(Location location, Map<String, String> errors) {
        if (location.getName() != null && location.getName().trim().length() > 255) {
            errors.put("location.name", "Location name cannot exceed 255 characters");
        }
    }

    public Map<Color, Long> getHairColorStatistics() {
        List<Person> allPersons = personRepository.findAll();
        Map<Color, Long> stats = new EnumMap<>(Color.class);

        for (Color color : Color.values()) {
            long count = allPersons.stream()
                    .filter(p -> color.equals(p.getHairColor()))
                    .count();
            stats.put(color, count);
        }

        return stats;
    }

    public Map<Country, Long> getNationalityStatistics() {
        List<Person> allPersons = personRepository.findAll();
        Map<Country, Long> stats = new EnumMap<>(Country.class);

        for (Country country : Country.values()) {
            long count = allPersons.stream()
                    .filter(p -> country.equals(p.getNationality()))
                    .count();
            stats.put(country, count);
        }

        return stats;
    }
}