package com.complianceiq.controller;

import com.complianceiq.service.StatutoryFormsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/forms")
@RequiredArgsConstructor
public class FormsController {

    private final StatutoryFormsService formsService;

    @GetMapping("/form16/{employeeId}")
    public ResponseEntity<byte[]> form16(
            @PathVariable UUID employeeId,
            @RequestParam int fy) {

        byte[] pdf = formsService.generateForm16(employeeId, fy);
        return pdfResponse(pdf, "form16.pdf");
    }

    @GetMapping("/ecr/{companyId}")
    public ResponseEntity<Map<String, String>> ecr(
            @PathVariable UUID companyId,
            @RequestParam int month,
            @RequestParam int year) {

        String ecrData = formsService.generateECR(companyId, month, year);
        return ResponseEntity.ok(Map.of("ecrFile", ecrData));
    }

    @GetMapping("/challan/{payrollRunId}")
    public ResponseEntity<byte[]> challan(
            @PathVariable UUID payrollRunId) {

        byte[] pdf = formsService.generateChallan(payrollRunId);
        return pdfResponse(pdf, "challan.pdf");
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] pdf, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}