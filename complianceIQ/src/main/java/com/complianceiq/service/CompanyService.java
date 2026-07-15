package com.complianceiq.service;

import com.complianceiq.dto.CompanyUpdateRequest;
import com.complianceiq.model.Company;
import com.complianceiq.model.Tenant;
import com.complianceiq.repository.CompanyRepository;
import com.complianceiq.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final TenantRepository tenantRepository;

    public Company createCompany(UUID tenantId, String companyName,
                                 String state, String city,
                                 String industryType,
                                 String epfNumber, String esicNumber,
                                 String tanNumber) {

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        Company company = Company.builder()
                .tenant(tenant)
                .companyName(companyName)
                .state(state)
                .city(city)
                .industryType(industryType)
                .epfRegistrationNumber(epfNumber)
                .esicRegistrationNumber(esicNumber)
                .tanNumber(tanNumber)
                .build();

        return companyRepository.save(company);
    }

    public List<Company> getAllCompanies(UUID tenantId) {
        return companyRepository.findByTenantIdAndIsActiveTrue(tenantId);
    }
    public Company getCompanyById(UUID companyId, UUID tenantId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        if (!company.getTenant().getId().equals(tenantId)) {
            throw new RuntimeException("Access denied");
        }

        return company;
    }

    // NEW — update company
    public Company updateCompany(UUID companyId, CompanyUpdateRequest req) {
        Company c = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        c.setCompanyName(req.getCompanyName());
        c.setState(req.getState());
        c.setCity(req.getCity());
        c.setIndustryType(req.getIndustryType());
        c.setEpfRegistrationNumber(req.getEpfRegistrationNumber());
        c.setEsicRegistrationNumber(req.getEsicRegistrationNumber());
        c.setTanNumber(req.getTanNumber());

        return companyRepository.save(c);
    }

    // NEW — soft delete
    public void deleteCompany(UUID companyId) {
        Company c = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));
        c.setIsActive(false);
        companyRepository.save(c);
    }
}