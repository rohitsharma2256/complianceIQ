package com.complianceiq.service;

import com.complianceiq.model.AuditLog;
import com.complianceiq.model.Tenant;
import com.complianceiq.model.User;
import com.complianceiq.repository.*;
import com.complianceiq.security.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Account deletion - soft delete + 30 day grace period.
 *
 * Turant hard delete nahi karte kyunki:
 *   1. Galti se dabaya ho toh recovery chahiye
 *   2. Statutory records 7 saal rakhne hote hain (Income Tax Act)
 *   3. Grace period industry standard hai (Google, Slack, GitHub sab)
 *
 * Grace period ke baad scheduled job permanently hata deta hai.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final CompanyRepository companyRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminService adminService;
    private final EmailService emailService;
    private final AuditService auditService;

    private static final int GRACE_DAYS = 30;

    /* ==================================================================
       CHANGE PASSWORD - logged-in user apna password badle
       (Forgot-password se alag: yahan purana password pata hona chahiye)
       ================================================================== */
    @Transactional
    public void changePassword(String currentPassword, String newPassword,
                               String confirmPassword) {

        User user = adminService.currentUser()
                .orElseThrow(() -> new RuntimeException("Not signed in"));

        // Purana password verify - session hijack se bachav
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash()))
            throw new RuntimeException("Your current password is incorrect.");

        if (newPassword == null || newPassword.length() < 6)
            throw new RuntimeException("The new password must be at least 6 characters.");

        if (!newPassword.equals(confirmPassword))
            throw new RuntimeException("The new passwords do not match.");

        if (passwordEncoder.matches(newPassword, user.getPasswordHash()))
            throw new RuntimeException("The new password must be different from the current one.");

        user.setPasswordHash(passwordEncoder.encode(newPassword));   // BCrypt
        userRepository.save(user);

        try {
            emailService.sendPasswordChangedEmail(user.getEmail(), user.getFullName());
        } catch (Exception e) {
            log.warn("Could not send password-change notice to {}", user.getEmail());
        }

        log.info("Password changed for {}", user.getEmail());

        auditService.log(AuditLog.Action.PASSWORD_CHANGE, "USER", user.getId(),
                "Password changed");
    }

    /* ==================================================================
       STEP 1 - Deletion request (soft delete)
       ================================================================== */
    @Transactional
    public DeletionResult requestDeletion(String password, String confirmation) {

        User user = adminService.currentUser()
                .orElseThrow(() -> new RuntimeException("Not signed in"));

        // Password confirm - session hijack se bachav
        if (!passwordEncoder.matches(password, user.getPasswordHash()))
            throw new RuntimeException("Incorrect password.");

        // Typed confirmation - accidental click se bachav
        if (!"DELETE".equals(confirmation))
            throw new RuntimeException("Type DELETE to confirm.");

        // Platform admin apna account delete na kar de
        if (adminService.isPlatformAdmin())
            throw new RuntimeException(
                    "The platform administrator account cannot be deleted from here.");

        Tenant tenant = user.getTenant();
        LocalDateTime now = LocalDateTime.now();

        user.setIsActive(false);
        user.setDeletionRequestedAt(now);
        userRepository.save(user);

        tenant.setIsActive(false);
        tenant.setDeletionRequestedAt(now);
        tenantRepository.save(tenant);

        LocalDateTime permanentAt = now.plusDays(GRACE_DAYS);

        try {
            emailService.sendAccountDeletionEmail(
                    user.getEmail(), user.getFullName(), permanentAt.toLocalDate().toString());
        } catch (Exception e) {
            log.warn("Could not send deletion email to {}", user.getEmail());
        }

        log.info("Deletion requested for tenant {} - permanent on {}",
                tenant.getId(), permanentAt);

        auditService.log(AuditLog.Action.ACCOUNT_DELETION_REQUESTED, "TENANT",
                tenant.getId(), "Account deletion requested - permanent on " + permanentAt);

        return new DeletionResult(permanentAt.toLocalDate().toString(), GRACE_DAYS);
    }

    /* ==================================================================
       STEP 2 - Reactivate (grace period ke andar)
       ================================================================== */
    @Transactional
    public void reactivate(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!passwordEncoder.matches(password, user.getPasswordHash()))
            throw new RuntimeException("Incorrect password.");

        if (Boolean.TRUE.equals(user.getIsActive()))
            throw new RuntimeException("This account is already active.");

        if (user.getDeletionRequestedAt() == null
                || user.getDeletionRequestedAt().plusDays(GRACE_DAYS)
                .isBefore(LocalDateTime.now()))
            throw new RuntimeException(
                    "The grace period has ended and this account cannot be restored.");

        user.setIsActive(true);
        user.setDeletionRequestedAt(null);
        userRepository.save(user);

        Tenant tenant = user.getTenant();
        tenant.setIsActive(true);
        tenant.setDeletionRequestedAt(null);
        tenantRepository.save(tenant);

        log.info("Account reactivated: {}", email);
    }

    /* ==================================================================
       STEP 3 - Permanent delete (roz 3 AM)
       ================================================================== */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpiredAccounts() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(GRACE_DAYS);
        List<Tenant> expired =
                tenantRepository.findByIsActiveFalseAndDeletionRequestedAtBefore(cutoff);

        for (Tenant t : expired) {
            try {
                // Order matters - child pehle, parent baad mein (FK constraints)
                companyRepository.findByTenantId(t.getId()).forEach(c ->
                        employeeRepository.deleteAll(
                                employeeRepository.findByCompanyIdAndIsActiveTrue(c.getId())));
                companyRepository.deleteAll(companyRepository.findByTenantId(t.getId()));
                userRepository.deleteAll(userRepository.findByTenantId(t.getId()));
                tenantRepository.delete(t);

                log.info("Permanently deleted tenant {} after grace period", t.getId());
            } catch (Exception e) {
                log.error("Could not purge tenant {}: {}", t.getId(), e.getMessage());
            }
        }
        if (!expired.isEmpty())
            log.info("Purged {} expired account(s)", expired.size());
    }

    /** Grace period mein hai ya nahi - login pe check karne ke liye */
    public boolean isPendingDeletion(User user) {
        return Boolean.FALSE.equals(user.getIsActive())
                && user.getDeletionRequestedAt() != null;
    }

    public record DeletionResult(String permanentDeletionDate, int graceDays) {}
}