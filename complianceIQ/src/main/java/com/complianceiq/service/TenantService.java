package com.complianceiq.service;

import com.complianceiq.model.Tenant;
import com.complianceiq.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TenantService {
    private final TenantRepository tenantRepository;

    public Tenant createTenant(String firmName, String email, String phone){
        if (tenantRepository.existsByEmail(email)){
            throw new RuntimeException("Tenant already exists with this email");
        }

        Tenant tenant = Tenant.builder()
                .firmName(firmName)
                .email(email)
                .phone(phone)
                .build();

        return tenantRepository.save(tenant);

    }
}
