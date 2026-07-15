package com.complianceiq.repository;

import com.complianceiq.model.PayrollRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PayrollRunRepository extends JpaRepository<PayrollRun, UUID> {
    List<PayrollRun> findByCompanyId(UUID companyId);
    Optional<PayrollRun> findByCompanyIdAndMonthAndYear(UUID companyId, Integer month, Integer year);
}