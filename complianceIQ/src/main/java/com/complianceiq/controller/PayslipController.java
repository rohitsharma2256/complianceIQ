package com.complianceiq.controller;

import com.complianceiq.service.PayslipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/payslips")
@RequiredArgsConstructor
public class PayslipController {

    private final PayslipService payslipService;

    @GetMapping("/{employeeId}")
    public ResponseEntity<byte[]> downloadPayslip(
            @PathVariable UUID employeeId,
            @RequestParam int month,
            @RequestParam int year) {

        byte[] pdf = payslipService.generatePayslip(employeeId, month, year);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "payslip.pdf");

        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    // NEW — bulk ZIP download
    @GetMapping("/bulk/{companyId}")
    public ResponseEntity<byte[]> downloadBulkPayslips(
            @PathVariable UUID companyId,
            @RequestParam int month,
            @RequestParam int year) {

        byte[] zip = payslipService.generateBulkPayslips(companyId, month, year);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "payslips.zip");

        return ResponseEntity.ok().headers(headers).body(zip);
    }
}