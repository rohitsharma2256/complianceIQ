package com.complianceiq.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Google reCAPTCHA v2 verification.
 *
 * Secret key configure nahi hai (local dev) toh verification SKIP -
 * warna developer har login pe captcha bharta rahega.
 * Production mein key set hoti hai, toh verify hota hai.
 */
@Service
@Slf4j
public class CaptchaService {

    @Value("${recaptcha.secret:}")
    private String secretKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public boolean verify(String token) {
        // Key nahi hai -> dev environment -> skip
        if (secretKey == null || secretKey.isBlank()) return true;

        if (token == null || token.isBlank()) return false;

        try {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("secret", secretKey);
            params.add("response", token);

            Map<?, ?> response = restTemplate.postForObject(
                    "https://www.google.com/recaptcha/api/siteverify", params, Map.class);

            boolean ok = response != null && Boolean.TRUE.equals(response.get("success"));
            if (!ok) log.warn("Captcha verification failed: {}", response);
            return ok;

        } catch (Exception e) {
            log.error("Captcha service error: {}", e.getMessage());
            return false;      // fail closed - verify na ho toh reject
        }
    }
}