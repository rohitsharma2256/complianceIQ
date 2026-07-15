package com.complianceiq.controller;

import com.complianceiq.dto.AuthResponse;
import com.complianceiq.dto.LoginRequest;
import com.complianceiq.dto.RegisterRequest;
import com.complianceiq.model.Tenant;
import com.complianceiq.model.User;
import com.complianceiq.security.JwtService;
import com.complianceiq.service.TenantService;
import com.complianceiq.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final TenantService tenantService;
    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

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

        AuthResponse response = AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .firmName(tenant.getFirmName())
                .tenantId(tenant.getId())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {

        User user = userService.findByEmail(request.getEmail());

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }

        String token = jwtService.generateToken(user.getEmail());

        AuthResponse response = AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .firmName(user.getTenant().getFirmName())
                .tenantId(user.getTenant().getId())
                .build();

        return ResponseEntity.ok(response);
    }
}