package com.airplane.repository;

import com.airplane.config.RagProperties;
import com.airplane.domain.Airline;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class AirlineRepository {

    private final Map<String, Airline> airlines = new LinkedHashMap<>();

    public AirlineRepository(RagProperties properties) {
        for (RagProperties.AirlineDef d : properties.airlines()) {
            airlines.put(d.code(), new Airline(d.code(), d.name(), d.nameEn(), d.brandColor(), d.customerCenter()));
        }
    }

    public List<Airline> findAll() {
        return List.copyOf(airlines.values());
    }

    public Optional<Airline> findByCode(String code) {
        return Optional.ofNullable(airlines.get(code));
    }
}
