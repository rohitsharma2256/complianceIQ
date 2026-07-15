package com.complianceiq.repository;

import com.complianceiq.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface CompanyRepository extends JpaRepository<Company, UUID> {
    List<Company> findByTenantId(UUID tenantId);
    List<Company> findByTenantIdAndIsActiveTrue(UUID tenantId);
}
