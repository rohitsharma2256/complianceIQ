package com.complianceiq.service;

import com.complianceiq.model.Tenant;
import com.complianceiq.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeadlineService {

    private final EmailService emailService;
    private final TenantRepository tenantRepository;

    // Automatic — har din subah 9 baje
    @Scheduled(cron = "0 0 9 * * *")
    public void dailyDeadlineCheck() {
        List<String> alerts = getUpcomingDeadlines();

        if (alerts.isEmpty()) {
            log.info("No upcoming deadlines today.");
            return;
        }

        // Log karo
        alerts.forEach(alert -> log.warn("DEADLINE ALERT: {}", alert));

        // Saare active tenants (CA firms) ko email bhejo
        List<Tenant> tenants = tenantRepository.findAll();
        for (Tenant tenant : tenants) {
            if (Boolean.TRUE.equals(tenant.getIsActive())) {
                emailService.sendDeadlineAlert(
                        tenant.getEmail(),
                        tenant.getFirmName(),
                        alerts);
            }
        }
    }

    public List<String> getUpcomingDeadlines() {
        List<String> alerts = new ArrayList<>();
        LocalDate today = LocalDate.now();
        int day = today.getDayOfMonth();

        String monthName = today.getMonth()
                .getDisplayName(java.time.format.TextStyle.FULL,
                        java.util.Locale.ENGLISH);

        if (day <= 7) {
            alerts.add(formatAlert("TDS Payment", 7, 7 - day, monthName));
        }
        if (day <= 15) {
            alerts.add(formatAlert("EPF Contribution", 15, 15 - day, monthName));
            alerts.add(formatAlert("ESI Contribution", 15, 15 - day, monthName));
        }

        return alerts;
    }

    private String formatAlert(String taskName, int dueDate,
                               int daysLeft, String month) {
        if (daysLeft == 0) {
            return String.format(
                    "URGENT: %s is due TODAY (%d %s). File immediately!",
                    taskName, dueDate, month);
        } else if (daysLeft <= 3) {
            return String.format(
                    "%s due on %d %s. Only %d day(s) left!",
                    taskName, dueDate, month, daysLeft);
        } else {
            return String.format(
                    "%s due on %d %s. %d days remaining.",
                    taskName, dueDate, month, daysLeft);
        }
    }

    public String getPenaltyInfo(String taskType) {
        return switch (taskType.toUpperCase()) {
            case "EPF" -> """
                    EPF Late Payment Penalty:
                    - Up to 2 months delay: 5% per annum
                    - 2 to 4 months: 10% per annum
                    - 4 to 6 months: 15% per annum
                    - Above 6 months: 25% per annum
                    """;
            case "ESI" -> """
                    ESI Late Payment Penalty:
                    - Interest: 12% per annum on delayed amount
                    - Additional damages up to 25%
                    """;
            case "TDS" -> """
                    TDS Late Payment Penalty:
                    - Interest: 1.5% per month on delayed amount
                    - Late filing fee: Rs 200 per day (Section 234E)
                    """;
            default -> "No penalty information available for: " + taskType;
        };
    }
}