package com.complianceiq.controller;

import com.complianceiq.dto.AttendanceRequest;
import com.complianceiq.model.Attendance;
import com.complianceiq.service.AttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    /** Ek employee ka attendance save/update */
    @PostMapping
    public ResponseEntity<Attendance> save(@Valid @RequestBody AttendanceRequest req) {
        return ResponseEntity.ok(attendanceService.saveAttendance(req));
    }

    /** Company ka poora month */
    @GetMapping("/company/{companyId}")
    public List<Attendance> getByCompany(@PathVariable UUID companyId,
                                         @RequestParam int month,
                                         @RequestParam int year) {
        return attendanceService.getByCompanyAndPeriod(companyId, month, year);
    }

    /** Sabko full attendance mark karo (CA ka time bachta hai) */
    @PostMapping("/company/{companyId}/mark-full")
    public Map<String, Object> markFull(@PathVariable UUID companyId,
                                        @RequestParam int month,
                                        @RequestParam int year) {
        int n = attendanceService.markFullAttendanceForCompany(companyId, month, year);
        return Map.of("message", "Full attendance marked for " + n + " employees",
                "count", n);
    }

    /** Summary - kitne LOP pe hain */
    @GetMapping("/company/{companyId}/summary")
    public AttendanceService.AttendanceSummary summary(@PathVariable UUID companyId, @RequestParam int month,
                                                       @RequestParam int year) {
        return attendanceService.getSummary(companyId, month, year);
    }

    /** Working days calculator */
    @GetMapping("/working-days")
    public Map<String, Object> workingDays(@RequestParam int month, @RequestParam int year) {
        return Map.of("month", month, "year", year,
                "workingDays", attendanceService.calculateWorkingDays(month, year),
                "note", "Sundays excluded. Adjust manually for holidays.");
    }
}