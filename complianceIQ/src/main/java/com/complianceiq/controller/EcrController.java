package com.complianceiq.controller;

import com.complianceiq.service.EcrService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/ecr")
@RequiredArgsConstructor
public class EcrController {

    private final EcrService ecrService;

    /** Export se PEHLE check - CA ko problems pehle hi dikha do */
    @GetMapping("/validate/{companyId}")
    public EcrService.EcrValidation validate(@PathVariable UUID companyId,
                                             @RequestParam int month,
                                             @RequestParam int year) {
        return ecrService.validate(companyId, month, year);
    }

    @GetMapping("/download/{companyId}")
    public ResponseEntity<byte[]> download(@PathVariable UUID companyId,
                                           @RequestParam int month,
                                           @RequestParam int year) {
        byte[] file = ecrService.generateEcrFile(companyId, month, year);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.setContentDispositionFormData("attachment",
                ecrService.buildFileName(month, year));
        return ResponseEntity.ok().headers(headers).body(file);
    }
}