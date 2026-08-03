package com.complianceiq.controller;

import com.complianceiq.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    /** Soft delete - 30 din ka grace period shuru */
    @PostMapping("/delete")
    public ResponseEntity<?> requestDeletion(@RequestBody Map<String, String> body) {
        try {
            var result = accountService.requestDeletion(
                    body.get("password"), body.get("confirmation"));
            return ResponseEntity.ok(Map.of(
                    "message", "Your account has been deactivated. It will be permanently "
                            + "deleted on " + result.permanentDeletionDate()
                            + ". Log in before then to restore it.",
                    "permanentDeletionDate", result.permanentDeletionDate()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Grace period ke andar wapas laao - public endpoint (login se pehle) */
    @PostMapping("/reactivate")
    public ResponseEntity<?> reactivate(@RequestBody Map<String, String> body) {
        try {
            accountService.reactivate(body.get("email"), body.get("password"));
            return ResponseEntity.ok(Map.of(
                    "message", "Account restored. You can log in now."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    // Logged-in user apna password badle (purana password chahiye)
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> body) {
        try {
            accountService.changePassword(
                    body.get("currentPassword"),
                    body.get("newPassword"),
                    body.get("confirmPassword"));
            return ResponseEntity.ok(Map.of("message",
                    "Password changed successfully."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}