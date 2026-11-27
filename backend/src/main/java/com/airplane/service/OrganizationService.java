package com.airplane.service;

import com.airplane.domain.Organization;
import com.airplane.domain.QaDocument;
import com.airplane.dto.OrganizationResponse;
import com.airplane.exception.OrganizationNotFoundException;
import com.airplane.repository.OrganizationRepository;
import com.airplane.repository.QaRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final QaRepository qaRepository;

    public OrganizationService(OrganizationRepository organizationRepository, QaRepository qaRepository) {
        this.organizationRepository = organizationRepository;
        this.qaRepository = qaRepository;
    }

    public List<OrganizationResponse> findAll() {
        return organizationRepository.findAll().stream().map(o -> {
            List<QaDocument> qa = qaRepository.findByOrg(o.name());
            return OrganizationResponse.of(o, qa.size(), qa.stream().limit(4).map(QaDocument::question).toList());
        }).toList();
    }

    public Organization getByCode(String code) {
        return organizationRepository.findByCode(code).orElseThrow(() -> new OrganizationNotFoundException(code));
    }
}
