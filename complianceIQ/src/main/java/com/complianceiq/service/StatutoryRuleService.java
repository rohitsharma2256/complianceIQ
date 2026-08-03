package com.complianceiq.service;

import com.complianceiq.model.StatutoryRule;
import com.complianceiq.model.StatutoryRule.RuleType;
import com.complianceiq.repository.StatutoryRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatutoryRuleService {

    private final StatutoryRuleRepository ruleRepository;

    /* ==================================================================
       1. PROFESSIONAL TAX  -  state-wise slab
          Haryana/Delhi/UP -> applicable=false -> ZERO   (BLOCKER FIX)
       ================================================================== */
    public BigDecimal calculateProfessionalTax(String state, BigDecimal monthlyGross) {
        if (state == null || monthlyGross == null) return BigDecimal.ZERO;

        List<StatutoryRule> slabs =
                ruleRepository.findByRuleTypeAndStateIgnoreCase(RuleType.PT, state.trim());

        // Is state ka rule hi nahi -> PT nahi lagta
        if (slabs.isEmpty()) return BigDecimal.ZERO;

        // State mein PT applicable hi nahi (Haryana, Delhi, UP...)
        boolean applicable = slabs.stream().anyMatch(StatutoryRule::getApplicable);
        if (!applicable) {
            log.debug("PT not applicable in {} -> 0", state);
            return BigDecimal.ZERO;
        }

        // Gross kis slab mein aata hai
        return slabs.stream()
                .filter(StatutoryRule::getApplicable)
                .filter(r -> monthlyGross.compareTo(nz(r.getSlabMin())) >= 0)
                .filter(r -> r.getSlabMax() == null                    // top slab open-ended
                        || monthlyGross.compareTo(r.getSlabMax()) <= 0)
                .findFirst()
                .map(r -> nz(r.getAmount()))
                .orElse(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /* ==================================================================
       2. PF  -  Rs 15,000 wage ceiling                  (BLOCKER FIX)
          Basic 50,000 ho tab bhi PF = 15,000 x 12% = 1,800
       ================================================================== */
    public PfResult calculatePf(BigDecimal basicPlusDa) {
        StatutoryRule rule = ruleRepository
                .findFirstByRuleTypeAndStateIgnoreCase(RuleType.PF, "ALL")
                .orElse(null);

        if (rule == null || basicPlusDa == null)
            return new PfResult(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false);

        BigDecimal ceiling = nz(rule.getWageCeiling());        // 15000

        // *** YEH LINE HI ASLI STATUTORY CORRECTNESS HAI ***
        BigDecimal pfWage = (ceiling.compareTo(BigDecimal.ZERO) > 0
                && basicPlusDa.compareTo(ceiling) > 0)
                ? ceiling : basicPlusDa;

        return new PfResult(
                pct(pfWage, rule.getEmployeeRate()),            // 12%
                pct(pfWage, rule.getEmployerRate()),            // 12%
                pfWage,
                basicPlusDa.compareTo(ceiling) > 0);            // capped hua?
    }

    /* ==================================================================
       3. ESI  -  threshold GROSS wages pe (CTC pe NAHI)  (BLOCKER FIX)
       ================================================================== */
    public EsiResult calculateEsi(BigDecimal monthlyGross) {
        StatutoryRule rule = ruleRepository
                .findFirstByRuleTypeAndStateIgnoreCase(RuleType.ESI, "ALL")
                .orElse(null);

        if (rule == null || monthlyGross == null)
            return new EsiResult(BigDecimal.ZERO, BigDecimal.ZERO, false);

        BigDecimal threshold = nz(rule.getWageCeiling());      // 21000

        // Gross threshold se upar -> ESI nahi lagta
        if (monthlyGross.compareTo(threshold) > 0)
            return new EsiResult(BigDecimal.ZERO, BigDecimal.ZERO, false);

        return new EsiResult(
                pct(monthlyGross, rule.getEmployeeRate()),      // 0.75%
                pct(monthlyGross, rule.getEmployerRate()),      // 3.25%
                true);
    }

    /* ==================================================================
       4. LWF  -  sirf applicable states mein
       ================================================================== */
    public LwfResult calculateLwf(String state) {
        if (state == null) return new LwfResult(BigDecimal.ZERO, BigDecimal.ZERO, false);

        StatutoryRule rule = ruleRepository
                .findByRuleTypeAndStateIgnoreCase(RuleType.LWF, state.trim())
                .stream().filter(StatutoryRule::getApplicable)
                .findFirst().orElse(null);

        if (rule == null) return new LwfResult(BigDecimal.ZERO, BigDecimal.ZERO, false);

        return new LwfResult(nz(rule.getAmount()),             // employee share
                nz(rule.getEmployerRate()),       // employer share
                true);
    }

    /* ---------------- helpers ---------------- */
    private BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    private BigDecimal pct(BigDecimal base, BigDecimal rate) {
        if (base == null || rate == null) return BigDecimal.ZERO;
        return base.multiply(rate)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    /* ---------------- result records ---------------- */
    public record PfResult(BigDecimal employee, BigDecimal employer,
                           BigDecimal pfWage, boolean cappedAtCeiling) {}
    public record EsiResult(BigDecimal employee, BigDecimal employer,
                            boolean applicable) {}
    public record LwfResult(BigDecimal employee, BigDecimal employer,
                            boolean applicable) {}
}