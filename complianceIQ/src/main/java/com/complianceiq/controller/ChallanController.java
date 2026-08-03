package com.complianceiq.controller;

import com.complianceiq.service.ChallanService;
import com.complianceiq.service.ChallanService.ChallanType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/challans")
@RequiredArgsConstructor
public class ChallanController {

    private final ChallanService challanService;

    /** @param type EPF | ESI | TDS */
    @GetMapping("/{payrollRunId}")
    public ResponseEntity<byte[]> download(@PathVariable UUID payrollRunId,
                                           @RequestParam ChallanType type) {
        byte[] pdf = challanService.generateChallan(payrollRunId, type);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment",
                challanService.buildFileName(payrollRunId, type));
        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}