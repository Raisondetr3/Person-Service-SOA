package ru.itmo.person_service.exception;

public class PersonNotFoundException extends RuntimeException {

    public PersonNotFoundException(String message) {
        super(message);
    }

    public PersonNotFoundException(Integer id) {
        super("Person with ID " + id + " not found");
    }

    public PersonNotFoundException(String field, String value) {
        super("Person with " + field + " '" + value + "' not found");
    }
}