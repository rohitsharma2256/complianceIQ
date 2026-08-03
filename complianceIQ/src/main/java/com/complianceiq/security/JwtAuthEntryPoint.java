package com.complianceiq.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Token missing/expired/invalid -> 401, na ki 403.
 * Frontend 401 pe hi session clear karta hai; 403 "permission nahi hai"
 * ke liye reserved hai (jaise admin-only endpoints).
 */
@Component
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);   // 401
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"error\":\"Session expired or invalid. Please log in again.\"}");
    }
}