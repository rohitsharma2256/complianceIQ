package com.complianceiq.controller;

import com.complianceiq.security.AdminService;
import com.complianceiq.service.LawUpdateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/law-updates")
@RequiredArgsConstructor
public class LawUpdateController {

    private final LawUpdateService lawUpdateService;
    private final AdminService adminService;

    /**
     * Law update saare tenants ke RAG answers badal deta hai, isliye
     * sirf platform admin. Admin list config se aati hai (AdminService) -
     * hardcode nahi, taaki badalne ke liye deploy na karna pade.
     */
    @PostMapping("/add")
    public ResponseEntity<?> addUpdate(@RequestBody Map<String, String> request) {

        adminService.requirePlatformAdmin();   // fail hua toh 403 (GlobalExceptionHandler se)

        Map<String, Object> result = lawUpdateService.addLawUpdate(
                request.get("title"),
                request.get("content"),
                request.get("source"),
                request.get("effectiveDate")
        );
        return ResponseEntity.ok(result);
    }

    /** Frontend ko batao kya-kya dikhana hai (sidebar link, banner) */
    @GetMapping("/is-admin")
    public Map<String, Boolean> isAdmin() {
        System.out.println("EMAIL FROM TOKEN: [" + adminService.currentEmail() + "]");
        return Map.of("isAdmin",adminService.isPlatformAdmin(), "isTenantAdmin", adminService.isTenantAdmin()
        );
    }
}