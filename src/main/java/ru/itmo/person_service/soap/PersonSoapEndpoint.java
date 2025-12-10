package ru.itmo.person_service.soap;

import jakarta.xml.bind.JAXBElement;
import lombok.RequiredArgsConstructor;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import ru.itmo.person_service.dto.CoordinatesDTO;
import ru.itmo.person_service.dto.LocationDTO;
import ru.itmo.person_service.dto.PersonRequestDTO;
import ru.itmo.person_service.entity.Person;
import ru.itmo.person_service.entity.enums.Color;
import ru.itmo.person_service.entity.enums.Country;
import ru.itmo.person_service.service.PersonService;
import ru.itmo.person_service.soap.dto.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

@Endpoint
@RequiredArgsConstructor
public class PersonSoapEndpoint {

    private static final String NAMESPACE_URI = "http://itmo.ru/person-service";

    private final PersonService personService;
    private final ObjectFactory objectFactory = new ObjectFactory();

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "getPersonByIdRequest")
    @ResponsePayload
    public JAXBElement<PersonResponse> getPersonById(@RequestPayload GetPersonByIdRequest request) {
        Person person = personService.findById(request.getId())
                .orElseThrow(() -> new RuntimeException("Person not found: " + request.getId()));
        return objectFactory.createGetPersonByIdResponse(toPersonResponse(person));
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "createPersonRequest")
    @ResponsePayload
    public JAXBElement<PersonResponse> createPerson(@RequestPayload CreatePersonRequest request) {
        PersonRequestDTO dto = toPersonRequestDTO(request.getPerson());
        Person saved = personService.save(dto);
        return objectFactory.createCreatePersonResponse(toPersonResponse(saved));
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "updatePersonRequest")
    @ResponsePayload
    public JAXBElement<PersonResponse> updatePerson(@RequestPayload UpdatePersonRequest request) {
        PersonRequestDTO dto = toPersonRequestDTO(request.getPerson());
        Person updated = personService.update(request.getId(), dto);
        return objectFactory.createUpdatePersonResponse(toPersonResponse(updated));
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "deletePersonRequest")
    @ResponsePayload
    public DeletePersonResponse deletePerson(@RequestPayload DeletePersonRequest request) {
        personService.deleteById(request.getId());
        DeletePersonResponse response = new DeletePersonResponse();
        response.setSuccess(true);
        return response;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "searchPersonsRequest")
    @ResponsePayload
    public JAXBElement<PagedResponse> searchPersons(@RequestPayload SearchCriteria request) {
        Map<String, String> filterMap = request.getFilters().stream()
                .collect(Collectors.toMap(
                        f -> f.getField() + "[" + f.getOperator() + "]",
                        FilterCondition::getValue
                ));

        PageRequest pageReq = request.getPageRequest();
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(
                pageReq != null ? pageReq.getPage() : 0,
                pageReq != null ? pageReq.getSize() : 4,
                pageReq != null ? buildSort(pageReq.getSort()) : org.springframework.data.domain.Sort.unsorted()
        );

        org.springframework.data.domain.Page<Person> page = personService.findAllWithFilters(filterMap, pageable);

        PagedResponse paged = new PagedResponse();
        paged.getContent().addAll(page.getContent().stream().map(this::toPersonResponse).toList());
        paged.setTotalElements(page.getTotalElements());
        paged.setTotalPages(page.getTotalPages());
        paged.setCurrentPage(page.getNumber());
        paged.setPageSize(page.getSize());
        paged.setHasNext(page.hasNext());
        paged.setHasPrevious(page.hasPrevious());

        return objectFactory.createSearchPersonsResponse(paged);
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "countPersonsRequest")
    @ResponsePayload
    public CountPersonsResponse countPersons() {
        CountPersonsResponse response = new CountPersonsResponse();
        response.setCount(personService.count());
        return response;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "personExistsRequest")
    @ResponsePayload
    public PersonExistsResponse personExists(@RequestPayload PersonExistsRequest request) {
        PersonExistsResponse response = new PersonExistsResponse();
        response.setExists(personService.existsById(request.getId()));
        return response;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "findPersonWithLongestNameRequest")
    @ResponsePayload
    public JAXBElement<PersonResponse> findPersonWithLongestName() {
        Person person = personService.findPersonWithMaxName()
                .orElseThrow(() -> new RuntimeException("No persons in DB"));
        return objectFactory.createFindPersonWithLongestNameResponse(toPersonResponse(person));
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "getHairColorStatsRequest")
    @ResponsePayload
    public JAXBElement<StatsResponse> getHairColorStats() {
        Map<Color, Long> stats = personService.getHairColorStatistics();
        return objectFactory.createGetHairColorStatsResponse(buildStatsResponse(stats));
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "getNationalityStatsRequest")
    @ResponsePayload
    public JAXBElement<StatsResponse> getNationalityStats() {
        Map<Country, Long> stats = personService.getNationalityStatistics();
        return objectFactory.createGetNationalityStatsResponse(buildStatsResponse(stats));
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "findPersonsByNationalityLessThanRequest")
    @ResponsePayload
    public FindPersonsByNationalityLessThanResponse findPersonsByNationalityLessThan(
            @RequestPayload FindPersonsByNationalityLessThanRequest request) {

        Country country = toCountry(request.getNationality());
        List<Person> persons = personService.findByNationalityLessThan(country);
        FindPersonsByNationalityLessThanResponse response = new FindPersonsByNationalityLessThanResponse();
        response.getPersons().addAll(persons.stream().map(this::toPersonResponse).toList());
        return response;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "deleteOneByHairColorRequest")
    @ResponsePayload
    public DeleteOneByHairColorResponse deleteOneByHairColor(
            @RequestPayload DeleteOneByHairColorRequest request) {

        Color color = toColor(request.getHairColor());
        Optional<Person> deleted = personService.deleteByHairColor(color);
        DeleteOneByHairColorResponse response = new DeleteOneByHairColorResponse();
        response.setDeleted(deleted.isPresent());
        return response;
    }

    private StatsResponse buildStatsResponse(Map<?, Long> stats) {
        StatsResponse res = new StatsResponse();
        stats.forEach((k, v) -> {
            MapEntry entry = new MapEntry();
            entry.setKey(k.toString());
            entry.setValue(v);
            res.getEntries().add(entry);
        });
        return res;
    }

    private PersonRequestDTO toPersonRequestDTO(PersonRequest request) {
        if (request == null) return null;

        Coordinates coordinates = request.getCoordinates();
        Location location = request.getLocation();

        return new PersonRequestDTO(
                request.getName(),
                coordinates != null ? new CoordinatesDTO(coordinates.getX(), coordinates.getY()) : null,
                request.getHeight(),
                request.getWeight(),
                toColor(request.getHairColor()),
                toColor(request.getEyeColor()),
                toCountry(request.getNationality()),
                location != null ? new LocationDTO(location.getX(), location.getY(), location.getZ(), location.getName()) : null
        );
    }

    private Color toColor(ColorType colorType) {
        if (colorType == null) return null;
        return Color.valueOf(colorType.value());
    }

    private Country toCountry(CountryType countryType) {
        if (countryType == null) return null;
        return Country.valueOf(countryType.value());
    }

    private PersonResponse toPersonResponse(Person person) {
        if (person == null) return null;

        ru.itmo.person_service.entity.Coordinates coordinates = person.getCoordinates();
        ru.itmo.person_service.entity.Location location = person.getLocation();

        PersonResponse response = new PersonResponse();
        response.setId(person.getId());
        response.setName(person.getName());
        response.setCoordinates(toSoapCoordinates(coordinates));
        response.setCreationDate(toXMLGregorianCalendar(person.getCreationDate()));
        response.setHeight(person.getHeight());
        response.setWeight(person.getWeight());
        response.setHairColor(ColorType.valueOf(person.getHairColor().name()));
        response.setEyeColor(ColorType.valueOf(person.getEyeColor().name()));
        response.setNationality(CountryType.valueOf(person.getNationality().name()));
        response.setLocation(toSoapLocation(location));

        return response;
    }

    private XMLGregorianCalendar toXMLGregorianCalendar(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        try {
            ZoneId zoneId = ZoneId.systemDefault();
            GregorianCalendar gcal = GregorianCalendar.from(dateTime.atZone(zoneId));

            return DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert LocalDateTime to XMLGregorianCalendar", e);
        }
    }

    private Coordinates toSoapCoordinates(ru.itmo.person_service.entity.Coordinates coordinates) {
        if (coordinates == null) return null;
        Coordinates res = new Coordinates();
        res.setX(coordinates.getX());
        res.setY(coordinates.getY());
        return res;
    }

    private Location toSoapLocation(ru.itmo.person_service.entity.Location location) {
        if (location == null) return null;
        Location res = new Location();
        res.setX(location.getX());
        res.setY(location.getY());
        res.setZ(location.getZ());
        res.setName(location.getName());
        return res;
    }

    private org.springframework.data.domain.Sort buildSort(List<Sort> sorts) {
        if (sorts == null || sorts.isEmpty()) return org.springframework.data.domain.Sort.unsorted();
        return org.springframework.data.domain.Sort.by(
                sorts.stream()
                        .map(s -> "DESC".equalsIgnoreCase(s.getDirection())
                                ? org.springframework.data.domain.Sort.Order.desc(s.getProperty())
                                : org.springframework.data.domain.Sort.Order.asc(s.getProperty()))
                        .toList()
        );
    }
}