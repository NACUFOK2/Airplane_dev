package com.airplane.dto;

import com.airplane.domain.Airline;
import java.util.List;

public record AirlineResponse(String code, String name, String nameEn, String brandColor, String customerCenter, List<String> suggestedQuestions) {
    public static AirlineResponse of(Airline a, List<String> suggestions) {
        return new AirlineResponse(a.code(), a.name(), a.nameEn(), a.brandColor(), a.customerCenter(), suggestions);
    }
}
