package com.complianceiq.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            log.info("Email sent to {}", to);

        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    // Deadline alerts ka formatted email
    public void sendDeadlineAlert(String to, String firmName,
                                  List<String> deadlines) {
        if (deadlines.isEmpty()) return;

        StringBuilder body = new StringBuilder();
        body.append("Dear ").append(firmName).append(",\n\n");
        body.append("You have upcoming compliance deadlines:\n\n");

        for (String deadline : deadlines) {
            body.append("- ").append(deadline).append("\n");
        }

        body.append("\nPlease ensure timely filing to avoid penalties.\n\n");
        body.append("This is an automated reminder from ComplianceIQ.\n");
        body.append("Note: Please verify with your CA for final filings.");

        sendEmail(to, "Compliance Deadline Reminder - ComplianceIQ",
                body.toString());
    }
}