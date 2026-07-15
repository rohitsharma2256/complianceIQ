package com.complianceiq.controller;

import com.complianceiq.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/compliance/{payrollRunId}")
    public ResponseEntity<byte[]> downloadReport(
            @PathVariable UUID payrollRunId) {

        byte[] pdf = reportService.generateComplianceReport(payrollRunId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment",
                "compliance-report.pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdf);
    }
}