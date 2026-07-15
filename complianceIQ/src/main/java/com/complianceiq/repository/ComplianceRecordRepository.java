package com.complianceiq.repository;

import com.complianceiq.model.ComplianceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ComplianceRecordRepository extends JpaRepository<ComplianceRecord, UUID> {
    List<ComplianceRecord> findByPayrollRunId(UUID payrollRunId);
    List<ComplianceRecord> findByPayrollRunIdAndIsResolvedFalse(UUID payrollRunId);
}
