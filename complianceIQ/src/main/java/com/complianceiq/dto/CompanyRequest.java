package com.complianceiq.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class CompanyRequest {

    @NotNull(message = "Tenant ID is required")
    private UUID tenantId;

    @NotBlank(message = "Company name is required")
    private String companyName;

    @NotBlank(message = "State is required")
    private String state;

    private String city;
    private String industryType;
    private String epfRegistrationNumber;
    private String esicRegistrationNumber;
    private String tanNumber;

    private String cin;
    private String gstin;
    private String pan;
    private String addressLine1;
    private String addressLine2;
    private String pincode;
    private String bankName;
    private String bankAccountNumber;
    private String bankIfsc;
    private String signatoryName;
    private String signatoryDesignation;
    private String logoUrl;
}