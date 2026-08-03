package com.complianceiq.repository;

import com.complianceiq.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {

    Optional<Attendance> findByEmployeeIdAndMonthAndYear(UUID employeeId, Integer month, Integer year);

    /** Ek company ka poora month ka attendance (N+1 se bachne ke liye JOIN FETCH) */
    @Query("SELECT a FROM Attendance a JOIN FETCH a.employee e " +
            "WHERE e.company.id = :companyId AND a.month = :month AND a.year = :year")
    List<Attendance> findByCompanyAndPeriod(@Param("companyId") UUID companyId,
                                            @Param("month") Integer month,
                                            @Param("year") Integer year);

    List<Attendance> findByEmployeeIdOrderByYearDescMonthDesc(UUID employeeId);
}