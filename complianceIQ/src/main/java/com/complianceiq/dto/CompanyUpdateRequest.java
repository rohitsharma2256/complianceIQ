package com.complianceiq.dto;

import lombok.Data;

@Data
public class CompanyUpdateRequest {
    private String companyName;
    private String state;
    private String city;
    private String industryType;
    private String epfRegistrationNumber;
    private String esicRegistrationNumber;
    private String tanNumber;
}