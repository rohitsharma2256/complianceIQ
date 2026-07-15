package com.complianceiq.controller;

import com.complianceiq.dto.CompanyRequest;
import com.complianceiq.dto.CompanyUpdateRequest;
import com.complianceiq.model.Company;
import com.complianceiq.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping
    public ResponseEntity<Company> addCompany(
            @Valid @RequestBody CompanyRequest request) {

        Company company = companyService.createCompany(
                request.getTenantId(),
                request.getCompanyName(),
                request.getState(),
                request.getCity(),
                request.getIndustryType(),
                request.getEpfRegistrationNumber(),
                request.getEsicRegistrationNumber(),
                request.getTanNumber()
        );
        return ResponseEntity.ok(company);
    }

    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<List<Company>> getCompanies(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(companyService.getAllCompanies(tenantId));
    }

    @GetMapping("/{companyId}")
    public ResponseEntity<Company> getCompany(@PathVariable UUID companyId,
                                              @RequestParam UUID tenantId) {
        return ResponseEntity.ok(companyService.getCompanyById(companyId, tenantId));
    }

    // NEW — update
    @PutMapping("/{companyId}")
    public ResponseEntity<Company> updateCompany(
            @PathVariable UUID companyId,
            @RequestBody CompanyUpdateRequest updated) {
        return ResponseEntity.ok(companyService.updateCompany(companyId, updated));
    }

    // NEW — delete
    @DeleteMapping("/{companyId}")
    public ResponseEntity<Map<String, String>> deleteCompany(
            @PathVariable UUID companyId) {
        companyService.deleteCompany(companyId);
        return ResponseEntity.ok(Map.of("message", "Company removed"));
    }
}