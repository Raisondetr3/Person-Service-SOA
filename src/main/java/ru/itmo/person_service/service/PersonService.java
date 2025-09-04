package ru.itmo.person_service.service;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import ru.itmo.person_service.entity.Person;
import ru.itmo.person_service.entity.enums.Color;
import ru.itmo.person_service.repo.PersonRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PersonService {
    private final PersonRepository personRepository;

    public List<Person> findAll() {
        return personRepository.findAll();
    }

    public Page<Person> findAllWithFilters(Map<String, String> filterParams, Pageable pageable) {
        Specification<Person> spec = buildSpecification(filterParams);
        return personRepository.findAll(spec, pageable);
    }

    private Specification<Person> buildSpecification(Map<String, String> filters) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            filters.forEach((field, value) -> {
                if (value == null || value.trim().isEmpty()) return;

                Path<Object> path = root.get(field);
                if (path == null) return;

                Class<?> type = path.getJavaType();

                if (String.class == type) {
                    predicates.add(cb.like(
                            cb.lower(cb.function("CAST", String.class, path)),
                            "%" + value.toLowerCase() + "%"
                    ));
                } else if (Enum.class.isAssignableFrom(type)) {
                    try {
                        Enum<?> enumValue = Enum.valueOf((Class<Enum>) type, value.toUpperCase());
                        predicates.add(cb.equal(path, enumValue));
                    } catch (IllegalArgumentException e) {
                        // Некорректное значение enum - пропускаем фильтр
                    }
                } else {
                    try {
                        Object convertedValue = convertValue(value, type);
                        if (convertedValue != null) {
                            predicates.add(cb.equal(path, convertedValue));
                        }
                    } catch (Exception e) {
                        // Некорректный формат значения - пропускаем фильтр
                    }
                }
            });

            return predicates.isEmpty() ?
                    cb.conjunction() :
                    cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Object convertValue(String value, Class<?> targetType) {
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

    public void deleteByHairColor(Color hairColor) {
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
