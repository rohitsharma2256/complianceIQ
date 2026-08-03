package com.complianceiq.service;

import com.complianceiq.model.PasswordResetToken;
import com.complianceiq.model.User;
import com.complianceiq.repository.PasswordResetTokenRepository;
import com.complianceiq.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    /** Step 1 - token banao aur email bhejo */
    public void requestReset(String email) {
        if (email == null || email.isBlank()) return;

        // SECURITY: email exist kare ya na kare, response SAME hoga.
        // Warna attacker pata kar lega ki kaunse email registered hain.
        userRepository.findByEmail(email.trim()).ifPresent(user -> {

            String token = UUID.randomUUID().toString();

            tokenRepository.save(PasswordResetToken.builder()
                    .token(token)
                    .email(user.getEmail())
                    .expiryTime(LocalDateTime.now().plusHours(1))
                    .used(false)
                    .build());

            String link = frontendUrl + "/reset-password?token=" + token;
            emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), link);
            log.info("Password reset link sent to {}", user.getEmail());
        });
    }

    /** Step 2 - token verify karke naya password set karo */
    public void resetPassword(String token, String newPassword) {

        if (newPassword == null || newPassword.length() < 6)
            throw new RuntimeException("Password must be at least 6 characters");

        PasswordResetToken reset = tokenRepository
                .findByTokenAndUsedFalse(token)
                .orElseThrow(() -> new RuntimeException("This link is invalid or already used"));

        if (reset.getExpiryTime().isBefore(LocalDateTime.now()))
            throw new RuntimeException("This link has expired. Please request a new one.");

        User user = userRepository.findByEmail(reset.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));   // BCrypt
        userRepository.save(user);

        reset.setUsed(true);                                          // single use
        tokenRepository.save(reset);
        log.info("Password reset completed for {}", user.getEmail());
    }
}