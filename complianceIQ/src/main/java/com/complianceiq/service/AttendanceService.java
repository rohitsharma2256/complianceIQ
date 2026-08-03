package com.complianceiq.service;

import com.complianceiq.dto.AttendanceRequest;
import com.complianceiq.model.Attendance;
import com.complianceiq.model.Employee;
import com.complianceiq.repository.AttendanceRepository;
import com.complianceiq.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;

    /* ==================================================================
       Month ke working days auto-calculate (Sundays hata ke)
       ================================================================== */
    public int calculateWorkingDays(int month, int year) {
        YearMonth ym = YearMonth.of(year, month);
        int working = 0;
        for (int d = 1; d <= ym.lengthOfMonth(); d++) {
            LocalDate date = LocalDate.of(year, month, d);
            if (date.getDayOfWeek() != DayOfWeek.SUNDAY) working++;
        }
        return working;
    }

    /* ==================================================================
       Attendance save/update - IDEMPOTENT (dobara same month -> update)
       ================================================================== */
    @Transactional
    public Attendance saveAttendance(AttendanceRequest req) {
        Employee emp = employeeRepository.findById(req.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        int workingDays = req.getWorkingDays() != null
                ? req.getWorkingDays()
                : calculateWorkingDays(req.getMonth(), req.getYear());

        Attendance att = attendanceRepository
                .findByEmployeeIdAndMonthAndYear(req.getEmployeeId(), req.getMonth(), req.getYear())
                .orElse(Attendance.builder()
                        .employee(emp).month(req.getMonth()).year(req.getYear()).build());

        att.setWorkingDays(workingDays);
        att.setPresentDays(nz(req.getPresentDays()));
        att.setPaidLeaveDays(nz(req.getPaidLeaveDays()));
        att.setUnpaidLeaveDays(nz(req.getUnpaidLeaveDays()));
        att.setOvertimeHours(nz(req.getOvertimeHours()));
        att.setRemarks(req.getRemarks());

        if (!att.isValid()) {
            throw new RuntimeException(
                    "Invalid attendance: present + paid leave + unpaid leave (" +
                            att.getPresentDays().add(att.getPaidLeaveDays()).add(att.getUnpaidLeaveDays()) +
                            ") cannot exceed working days (" + workingDays + ")");
        }
        return attendanceRepository.save(att);
    }

    /* ==================================================================
       Bulk: poore company ka default full attendance (koi absent nahi)
       CA ka time bachta hai - phir sirf exceptions edit kare
       ================================================================== */
    @Transactional
    public int markFullAttendanceForCompany(UUID companyId, int month, int year) {
        List<Employee> employees =
                employeeRepository.findByCompanyIdAndIsActiveTrue(companyId);
        int workingDays = calculateWorkingDays(month, year);
        int count = 0;

        for (Employee emp : employees) {
            Attendance att = attendanceRepository
                    .findByEmployeeIdAndMonthAndYear(emp.getId(), month, year)
                    .orElse(Attendance.builder()
                            .employee(emp).month(month).year(year).build());
            att.setWorkingDays(workingDays);
            att.setPresentDays(new BigDecimal(workingDays));
            att.setPaidLeaveDays(BigDecimal.ZERO);
            att.setUnpaidLeaveDays(BigDecimal.ZERO);
            att.setRemarks("Auto-marked full attendance");
            attendanceRepository.save(att);
            count++;
        }
        log.info("Marked full attendance for {} employees ({}/{})", count, month, year);
        return count;
    }

    /* ==================================================================
       Attendance nikalo - na mile toh full attendance maano (safe default)
       ================================================================== */
    public Attendance getOrDefault(Employee emp, int month, int year) {
        return attendanceRepository
                .findByEmployeeIdAndMonthAndYear(emp.getId(), month, year)
                .orElseGet(() -> {
                    int wd = calculateWorkingDays(month, year);
                    return Attendance.builder()
                            .employee(emp).month(month).year(year)
                            .workingDays(wd)
                            .presentDays(new BigDecimal(wd))       // full attendance assume
                            .paidLeaveDays(BigDecimal.ZERO)
                            .unpaidLeaveDays(BigDecimal.ZERO)
                            .remarks("No attendance record - full attendance assumed")
                            .build();
                });
    }

    public List<Attendance> getByCompanyAndPeriod(UUID companyId, int month, int year) {
        return attendanceRepository.findByCompanyAndPeriod(companyId, month, year);
    }

    /* ==================================================================
       Summary - AI aur dashboard ke liye
       ================================================================== */
    public AttendanceSummary getSummary(UUID companyId, int month, int year) {
        List<Attendance> list = getByCompanyAndPeriod(companyId, month, year);
        List<String> lopEmployees = new ArrayList<>();
        BigDecimal totalLop = BigDecimal.ZERO;

        for (Attendance a : list) {
            if (a.getLopDays().compareTo(BigDecimal.ZERO) > 0) {
                lopEmployees.add(a.getEmployee().getFullName() + " (" + a.getLopDays() + " days)");
                totalLop = totalLop.add(a.getLopDays());
            }
        }
        int total = employeeRepository.findByCompanyIdAndIsActiveTrue(companyId).size();
        return new AttendanceSummary(total, list.size(), total - list.size(),
                lopEmployees.size(), totalLop, lopEmployees);
    }

    private BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    public record AttendanceSummary(int totalEmployees, int recordsEntered,
                                    int recordsMissing, int employeesWithLop,
                                    BigDecimal totalLopDays, List<String> lopDetails) {}
}