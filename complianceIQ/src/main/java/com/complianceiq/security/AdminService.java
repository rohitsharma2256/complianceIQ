package com.complianceiq.security;

import com.complianceiq.model.Company;
import com.complianceiq.model.Employee;
import com.complianceiq.model.User;
import com.complianceiq.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

/**
 * Authorisation ka SINGLE SOURCE OF TRUTH.
 *
 * Teen level - real multi-tenant SaaS ka standard model:
 *   PLATFORM ADMIN  - product owner, saare tenants pe asar (law updates)
 *   TENANT ADMIN    - CA firm ka owner, apni firm ke andar sab kuch
 *   TENANT MEMBER   - staff, roz ka kaam
 *
 * Platform admin CONFIG se aata hai (hardcode nahi) - deploy pe badal
 * sakta hai bina code change ke.
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;

    @Value("${app.platform-admins:}")
    private String platformAdmins;

    /* ==================================================================
       CURRENT USER
       ================================================================== */
    public String currentEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth == null || auth.getName() == null) ? null : auth.getName();
    }

    public Optional<User> currentUser() {
        String email = currentEmail();
        return email == null ? Optional.empty() : userRepository.findByEmail(email);
    }

    public UUID currentTenantId() {
        return currentUser()
                .map(u -> u.getTenant().getId())
                .orElseThrow(() -> new AccessDeniedException("No tenant context."));
    }

    /* ==================================================================
       LEVEL 1 - PLATFORM ADMIN (product owner)
       ================================================================== */
    public boolean isPlatformAdmin() {
        String email = currentEmail();
        if (email == null || platformAdmins == null || platformAdmins.isBlank()) return false;

        return Arrays.stream(platformAdmins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .anyMatch(s -> s.equalsIgnoreCase(email.trim()));
    }

    public void requirePlatformAdmin() {
        if (!isPlatformAdmin())
            throw new AccessDeniedException(
                    "This action is restricted to the platform administrator.");
    }

    /* ==================================================================
       LEVEL 2 - TENANT ADMIN (firm owner)
       ================================================================== */
    public boolean isTenantAdmin() {
        return currentUser().map(u -> u.getRole() == User.Role.CA_ADMIN).orElse(false);
    }

    public void requireTenantAdmin() {
        if (isPlatformAdmin() || isTenantAdmin()) return;
        throw new AccessDeniedException(
                "Only the firm administrator can perform this action.");
    }

    /* ==================================================================
       TENANT ISOLATION - ek firm dusri ka data na dekhe
       Yeh service layer pe hai, controller pe nahi - taaki koi bhi
       entry point miss na ho jaaye.
       ================================================================== */

    /** Company is firm ki hai ya nahi - nahi toh 403 */
    public void requireOwnCompany(Company company) {
        if (isPlatformAdmin()) return;              // platform admin sab dekh sakta hai
        if (company == null || company.getTenant() == null
                || !company.getTenant().getId().equals(currentTenantId()))
            throw new AccessDeniedException("This company belongs to another firm.");
    }

    /** Employee is firm ka hai ya nahi */
    public void requireOwnEmployee(Employee employee) {
        if (isPlatformAdmin()) return;
        if (employee == null || employee.getCompany() == null)
            throw new AccessDeniedException("Invalid employee record.");
        requireOwnCompany(employee.getCompany());
    }

    /** Generic - jahan sirf tenantId available ho */
    public void requireSameTenant(UUID tenantId) {
        if (isPlatformAdmin()) return;
        if (tenantId == null || !currentTenantId().equals(tenantId))
            throw new AccessDeniedException("This record belongs to another firm.");
    }
}