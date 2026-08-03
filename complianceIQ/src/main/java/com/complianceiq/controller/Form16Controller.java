package com.complianceiq.controller;

import com.complianceiq.service.Form16Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/form16")
@RequiredArgsConstructor
public class Form16Controller {

    private final Form16Service form16Service;

    /** @param fy financial year start, e.g. 2025 => FY 2025-26 */
    @GetMapping("/{employeeId}")
    public ResponseEntity<byte[]> download(@PathVariable UUID employeeId,
                                           @RequestParam int fy) {
        byte[] pdf = form16Service.generateForm16(employeeId, fy);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment",
                "form16_" + fy + "-" + (fy + 1) + ".pdf");
        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}