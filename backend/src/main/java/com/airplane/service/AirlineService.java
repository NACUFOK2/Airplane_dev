package com.airplane.service;

import com.airplane.domain.Airline;
import com.airplane.dto.AirlineResponse;
import com.airplane.exception.AirlineNotFoundException;
import com.airplane.repository.AirlineRepository;
import com.airplane.repository.KnowledgeRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AirlineService {

    private final AirlineRepository airlineRepository;
    private final KnowledgeRepository knowledgeRepository;

    public AirlineService(AirlineRepository airlineRepository, KnowledgeRepository knowledgeRepository) {
        this.airlineRepository = airlineRepository;
        this.knowledgeRepository = knowledgeRepository;
    }

    public List<AirlineResponse> findAll() {
        return airlineRepository.findAll().stream()
                .map(a -> AirlineResponse.of(a, suggestions(a.code())))
                .toList();
    }

    public Airline getByCode(String code) {
        return airlineRepository.findByCode(code).orElseThrow(() -> new AirlineNotFoundException(code));
    }

    private List<String> suggestions(String code) {
        return knowledgeRepository.findByAirline(code).stream()
                .limit(4)
                .map(c -> c.title() + " 알려주세요")
                .toList();
    }
}
