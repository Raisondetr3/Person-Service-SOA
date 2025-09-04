package ru.itmo.person_service.service;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.person_service.entity.Person;
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

    public List<Person> findAll() {
        return personRepository.findAll();
    }

    public Page<Person> findAllWithFilters(Map<String, String> filterParams, Pageable pageable) {
        Map<String, String> cleanedFilters = removeSystemParams(filterParams);
        Specification<Person> spec = buildSpecification(cleanedFilters);
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

    private Specification<Person> buildSpecification(Map<String, String> filters) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            filters.forEach((field, value) -> {
                if (value == null || value.trim().isEmpty()) return;

                try {
                    Path<Object> path = getFieldPath(root, field);
                    if (path == null) {
                        log.warn("Field '{}' not found in Person entity", field);
                        return;
                    }

                    Class<?> type = path.getJavaType();

                    if (String.class == type) {
                        predicates.add(cb.like(
                                cb.lower(cb.function("CAST", String.class, path)),
                                "%" + value.toLowerCase() + "%"
                        ));
                    } else if (Enum.class.isAssignableFrom(type)) {
                        handleEnumFilter(predicates, cb, path, value, type);
                    } else if (Number.class.isAssignableFrom(type) || type.isPrimitive()) {
                        handleNumericFilter(predicates, cb, path, value, type);
                    } else {
                        Object convertedValue = convertValue(value, type);
                        if (convertedValue != null) {
                            predicates.add(cb.equal(path, convertedValue));
                        }
                    }
                } catch (Exception e) {
                    log.warn("Error processing filter field '{}' with value '{}': {}",
                            field, value, e.getMessage());
                }
            });

            return predicates.isEmpty() ?
                    cb.conjunction() :
                    cb.and(predicates.toArray(new Predicate[0]));
        };
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
            return null;
        }
    }

    private void handleEnumFilter(List<Predicate> predicates,
                                  CriteriaBuilder cb,
                                  Path<Object> path, String value, Class<?> type) {
        try {
            Enum<?> enumValue = Enum.valueOf((Class<Enum>) type, value.toUpperCase());
            predicates.add(cb.equal(path, enumValue));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid enum value '{}' for type {}", value, type.getSimpleName());
        }
    }

    private void handleNumericFilter(List<Predicate> predicates,
                                     CriteriaBuilder cb,
                                     Path<Object> path, String value, Class<?> type) {
        try {
            Object convertedValue = convertValue(value, type);
            if (convertedValue != null) {
                predicates.add(cb.equal(path, convertedValue));
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid numeric value '{}' for type {}", value, type.getSimpleName());
        }
    }

    private Object convertValue(String value, Class<?> targetType) {
        try {
            if (targetType == Long.class || targetType == long.class) {
                return Long.valueOf(value);
            } else if (targetType == Integer.class || targetType == int.class) {
                return Integer.valueOf(value);
            } else if (targetType == Double.class || targetType == double.class) {
                return Double.valueOf(value);
            } else if (targetType == Float.class || targetType == float.class) {
                return Float.valueOf(value);
            } else if (targetType == Boolean.class || targetType == boolean.class) {
                return Boolean.valueOf(value);
            }
            return value;
        } catch (NumberFormatException e) {
            log.warn("Failed to convert value '{}' to type {}", value, targetType.getSimpleName());
            return null;
        }
    }

    public Optional<Person> findById(Integer id) {
        validateId(id);
        return personRepository.findById(id);
    }

    @Transactional
    public Person save(Person person) {
        validatePerson(person);

        if (person.getId() == null || person.getId() == 0) {
            person.setCreationDate(LocalDateTime.now());
        }

        log.info("Saving person: {}", person.getName());
        return personRepository.save(person);
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
    public void deleteAll() {
        log.warn("Deleting all persons from database");
        personRepository.deleteAll();
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
            return Optional.empty();
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

    private void validateId(Integer id) {
        if (id == null || id <= 0) {
            throw new PersonValidationException("id", "ID must be a positive number");
        }
    }

    private void validatePerson(Person person) {
        if (person == null) {
            throw new InvalidPersonDataException("Person cannot be null");
        }

        Map<String, String> errors = new HashMap<>();

        if (person.getName() == null || person.getName().trim().isEmpty()) {
            errors.put("name", "Name cannot be null or empty");
        }

        if (person.getCoordinates() == null) {
            errors.put("coordinates", "Coordinates cannot be null");
        }

        if (person.getWeight() <= 0) {
            errors.put("weight", "Weight must be greater than 0");
        }

        if (person.getHeight() != null && person.getHeight() <= 0) {
            errors.put("height", "Height must be greater than 0");
        }

        if (person.getHairColor() == null) {
            errors.put("hairColor", "Hair color cannot be null");
        }

        if (person.getNationality() == null) {
            errors.put("nationality", "Nationality cannot be null");
        }

        if (!errors.isEmpty()) {
            throw new PersonValidationException(errors);
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