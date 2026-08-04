package com.complianceiq.controller;

import com.complianceiq.dto.AuthResponse;
import com.complianceiq.dto.LoginRequest;
import com.complianceiq.dto.RegisterRequest;
import com.complianceiq.model.Tenant;
import com.complianceiq.model.User;
import com.complianceiq.security.JwtService;
import com.complianceiq.service.CaptchaService;
import com.complianceiq.service.PasswordResetService;
import com.complianceiq.service.TenantService;
import com.complianceiq.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final TenantService tenantService;
    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetService passwordResetService;
    private final CaptchaService captchaService;


    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {

        Tenant tenant = tenantService.createTenant(
                request.getFirmName(),
                request.getEmail(),
                request.getPhone()
        );

        User user = userService.createUser(
                tenant,
                request.getEmail(),
                request.getPassword(),
                request.getFullName()
        );

        String token = jwtService.generateToken(user.getEmail());

        return ResponseEntity.ok(AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .firmName(tenant.getFirmName())
                .tenantId(tenant.getId())
                .build());
    }

    /**
     * Return type wildcard hai kyunki do alag shapes ja sakte hain:
     * success pe AuthResponse, deletion-pending pe error map.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        // Brute-force se bachav - Bucket4j rate limit ke saath
        if (!captchaService.verify(request.getCaptchaToken())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Captcha verification failed. Please try again."));
        }


        User user = userService.findByEmail(request.getEmail());

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid email or password."));
        }

        // Account grace period mein hai - frontend restore ka option dega
        if (Boolean.FALSE.equals(user.getIsActive())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "error", "This account is scheduled for deletion.",
                    "pendingDeletion", true));
        }

        String token = jwtService.generateToken(user.getEmail());

        return ResponseEntity.ok(AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .firmName(user.getTenant().getFirmName())
                .tenantId(user.getTenant().getId())
                .build());
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @RequestBody Map<String, String> request) {

        passwordResetService.requestReset(request.get("email"));

        // Hamesha same message - email exist kare ya na kare (enumeration se bachav)
        return ResponseEntity.ok(Map.of("message",
                "If an account exists with that email, a reset link has been sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @RequestBody Map<String, String> request) {
        try {
            passwordResetService.resetPassword(
                    request.get("token"), request.get("newPassword"));
            return ResponseEntity.ok(Map.of("message",
                    "Password updated successfully. You can log in now."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}