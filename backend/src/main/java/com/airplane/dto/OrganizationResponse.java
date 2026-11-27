package com.airplane.dto;

import com.airplane.domain.Organization;
import java.util.List;

public record OrganizationResponse(String code, String name, String type, String brandColor, int qaCount, List<String> suggestedQuestions) {
    public static OrganizationResponse of(Organization o, int qaCount, List<String> suggestions) {
        return new OrganizationResponse(o.code(), o.name(), o.type(), o.brandColor(), qaCount, suggestions);
    }
}
