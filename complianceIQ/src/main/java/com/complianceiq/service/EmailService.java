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

    public void sendPasswordResetEmail(String to, String fullName, String link) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("ComplianceIQ - Reset your password");
            message.setText(
                    "Hi " + (fullName == null ? "" : fullName) + ",\n\n" +
                            "We received a request to reset your ComplianceIQ password.\n\n" +
                            "Click the link below to set a new password:\n" +
                            link + "\n\n" +
                            "This link is valid for 1 hour and can be used only once.\n" +
                            "If you did not request this, you can safely ignore this email.\n\n" +
                            "- ComplianceIQ"
            );
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send reset email to {}: {}", to, e.getMessage());
        }
    }

    public void sendAccountDeletionEmail(String to, String name, String permanentDate) {
        try {
            SimpleMailMessage m = new SimpleMailMessage();
            m.setTo(to);
            m.setSubject("ComplianceIQ - Account deletion scheduled");
            m.setText("Hi " + (name == null ? "" : name) + ",\n\n"
                    + "Your ComplianceIQ account has been deactivated and is scheduled for "
                    + "permanent deletion on " + permanentDate + ".\n\n"
                    + "If this was a mistake, simply log in before that date and you will be "
                    + "offered the option to restore your account.\n\n"
                    + "After that date all firm data - companies, employees and payroll "
                    + "records - will be permanently removed and cannot be recovered.\n\n"
                    + "- ComplianceIQ");
            mailSender.send(m);
        } catch (Exception e) {
            log.error("Failed to send deletion email to {}: {}", to, e.getMessage());
        }
    }

    public void sendPasswordChangedEmail(String to, String name) {
        try {
            SimpleMailMessage m = new SimpleMailMessage();
            m.setTo(to);
            m.setSubject("ComplianceIQ - Your password was changed");
            m.setText("Hi " + (name == null ? "" : name) + ",\n\n"
                    + "Your ComplianceIQ password was changed just now.\n\n"
                    + "If this wasn't you, reset your password immediately using the "
                    + "'Forgot Password' link on the login page, and contact support.\n\n"
                    + "- ComplianceIQ");
            mailSender.send(m);
        } catch (Exception e) {
            log.error("Failed to send password-change email to {}: {}", to, e.getMessage());
        }
    }
}