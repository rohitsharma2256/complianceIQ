package com.complianceiq.config;

import com.complianceiq.model.StatutoryRule;
import com.complianceiq.model.StatutoryRule.RuleType;
import com.complianceiq.repository.StatutoryRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class StatutoryRuleSeeder implements CommandLineRunner {

    private final StatutoryRuleRepository repo;
    private static final LocalDate FROM = LocalDate.of(2025, 4, 1);

    @Override
    public void run(String... args) {
        if (repo.count() > 0) {
            log.info("Statutory rules already present - skipping seed.");
            return;
        }
        List<StatutoryRule> rules = new ArrayList<>();

        /* ---------- PF (country-wide) ---------- */
        rules.add(StatutoryRule.builder()
                .ruleType(RuleType.PF).state("ALL").applicable(true)
                .employeeRate(new BigDecimal("12"))
                .employerRate(new BigDecimal("12"))
                .wageCeiling(new BigDecimal("15000"))
                .effectiveFrom(FROM)
                .remarks("EPF on Basic+DA, statutory wage ceiling Rs 15,000")
                .build());

        /* ---------- ESI (country-wide) ---------- */
        rules.add(StatutoryRule.builder()
                .ruleType(RuleType.ESI).state("ALL").applicable(true)
                .employeeRate(new BigDecimal("0.75"))
                .employerRate(new BigDecimal("3.25"))
                .wageCeiling(new BigDecimal("21000"))
                .effectiveFrom(FROM)
                .remarks("ESI applicable when monthly GROSS wages <= Rs 21,000")
                .build());

        /* ---------- PT: states where NOT applicable ---------- */
        for (String s : List.of("HARYANA", "DELHI", "UTTAR PRADESH", "RAJASTHAN",
                "PUNJAB", "HIMACHAL PRADESH", "UTTARAKHAND",
                "GOA", "CHANDIGARH", "JAMMU AND KASHMIR",
                "ARUNACHAL PRADESH", "LADAKH")) {
            rules.add(StatutoryRule.builder()
                    .ruleType(RuleType.PT).state(s)
                    .applicable(false)                    // <<< HARYANA FIX
                    .amount(BigDecimal.ZERO)
                    .effectiveFrom(FROM)
                    .remarks("Professional Tax not levied in " + s)
                    .build());
        }

        /* ---------- PT slabs (applicable states) ---------- */
        rules.add(pt("MAHARASHTRA", "0", "7500", "0", "Up to 7,500 - Nil"));
        rules.add(pt("MAHARASHTRA", "7501", "10000", "175", "7,501-10,000"));
        rules.add(pt("MAHARASHTRA", "10001", null, "200", "Above 10,000 (Rs 300 in Feb)"));

        /* ---------- PT: Karnataka (revised w.e.f. 1 April 2025) ---------- */
        rules.add(pt("KARNATAKA", "0", "24999", "0",
                "Up to 25,000 - Nil (threshold raised from 1 Apr 2025)"));
        rules.add(pt("KARNATAKA", "25000", "41666", "150",
                "25,001-41,666 - Rs 150/month"));
        rules.add(pt("KARNATAKA", "41667", null, "200",
                "Above 41,666 - Rs 200/month"));

        rules.add(pt("TELANGANA", "0", "15000", "0", "Up to 15,000 - Nil"));
        rules.add(pt("TELANGANA", "15001", "20000", "150", "15,001-20,000"));
        rules.add(pt("TELANGANA", "20001", null, "200", "Above 20,000"));

        rules.add(pt("ANDHRA PRADESH", "0", "15000", "0", "Up to 15,000 - Nil"));
        rules.add(pt("ANDHRA PRADESH", "15001", "20000", "150", "15,001-20,000"));
        rules.add(pt("ANDHRA PRADESH", "20001", null, "200", "Above 20,000"));

        rules.add(pt("WEST BENGAL", "0", "10000", "0", "Up to 10,000 - Nil"));
        rules.add(pt("WEST BENGAL", "10001", "15000", "110", "10,001-15,000"));
        rules.add(pt("WEST BENGAL", "15001", "25000", "130", "15,001-25,000"));
        rules.add(pt("WEST BENGAL", "25001", "40000", "150", "25,001-40,000"));
        rules.add(pt("WEST BENGAL", "40001", null, "200", "Above 40,000"));

        rules.add(pt("GUJARAT", "0", "11999", "0", "Up to 11,999 - Nil"));
        rules.add(pt("GUJARAT", "12000", null, "200", "12,000 and above"));

        rules.add(pt("TAMIL NADU", "0", "21000", "0", "Up to 21,000 - Nil"));
        rules.add(pt("TAMIL NADU", "21001", "30000", "135", "21,001-30,000"));
        rules.add(pt("TAMIL NADU", "30001", "45000", "315", "30,001-45,000"));
        rules.add(pt("TAMIL NADU", "45001", "60000", "690", "45,001-60,000"));
        rules.add(pt("TAMIL NADU", "60001", "75000", "1025", "60,001-75,000"));
        rules.add(pt("TAMIL NADU", "75001", null, "1250", "Above 75,000"));

        rules.add(pt("KERALA", "0", "11999", "0", "Up to 11,999 - Nil"));
        rules.add(pt("KERALA", "12000", "17999", "120", "12,000-17,999"));
        rules.add(pt("KERALA", "18000", "29999", "180", "18,000-29,999"));
        rules.add(pt("KERALA", "30000", null, "208", "30,000 and above"));

        rules.add(pt("MADHYA PRADESH", "0", "18750", "0", "Up to 18,750 - Nil"));
        rules.add(pt("MADHYA PRADESH", "18751", "25000", "125", "18,751-25,000"));
        rules.add(pt("MADHYA PRADESH", "25001", null, "208", "Above 25,000"));

        rules.add(pt("ODISHA", "0", "13304", "0", "Up to 13,304 - Nil"));
        rules.add(pt("ODISHA", "13305", "25000", "125", "13,305-25,000"));
        rules.add(pt("ODISHA", "25001", null, "200", "Above 25,000"));

        rules.add(pt("BIHAR", "0", "25000", "0", "Up to 25,000 - Nil"));
        rules.add(pt("BIHAR", "25001", "41666", "83", "25,001-41,666"));
        rules.add(pt("BIHAR", "41667", null, "208", "Above 41,666"));

        rules.add(pt("ASSAM", "0", "10000", "0", "Up to 10,000 - Nil"));
        rules.add(pt("ASSAM", "10001", "15000", "150", "10,001-15,000"));
        rules.add(pt("ASSAM", "15001", "25000", "180", "15,001-25,000"));
        rules.add(pt("ASSAM", "25001", null, "208", "Above 25,000"));

        /* ---------- LWF ---------- */
        rules.add(lwf("MAHARASHTRA", "25", "75", true, "Half-yearly"));
        rules.add(lwf("KARNATAKA", "20", "40", true, "Annual"));
        rules.add(lwf("TAMIL NADU", "20", "40", true, "Annual"));
        rules.add(lwf("GUJARAT", "6", "12", true, "Half-yearly"));
        rules.add(lwf("WEST BENGAL", "3", "15", true, "Half-yearly"));
        rules.add(lwf("HARYANA", "31", "62", true, "Monthly"));
        rules.add(lwf("PUNJAB", "5", "20", true, "Monthly"));
        rules.add(lwf("DELHI", "0.75", "2.25", true, "Half-yearly"));
        rules.add(lwf("MADHYA PRADESH", "10", "30", true, "Half-yearly"));
        rules.add(lwf("ODISHA", "10", "20", true, "Half-yearly"));
        rules.add(lwf("KERALA", "20", "20", true, "Monthly"));
        rules.add(lwf("ANDHRA PRADESH", "30", "70", true, "Annual"));
        rules.add(lwf("TELANGANA", "2", "5", true, "Annual"));
        for (String s : List.of("UTTAR PRADESH", "RAJASTHAN", "BIHAR", "ASSAM",
                "JHARKHAND", "UTTARAKHAND", "HIMACHAL PRADESH")) {
            rules.add(lwf(s, "0", "0", false, "LWF not applicable in " + s));
        }

        repo.saveAll(rules);
        log.info("Seeded {} statutory rules (PT/LWF/PF/ESI).", rules.size());
    }

    private StatutoryRule pt(String state, String min, String max,
                             String amount, String remarks) {
        return StatutoryRule.builder()
                .ruleType(RuleType.PT).state(state).applicable(true)
                .slabMin(new BigDecimal(min))
                .slabMax(max == null ? null : new BigDecimal(max))
                .amount(new BigDecimal(amount))
                .effectiveFrom(FROM).remarks(remarks).build();
    }

    private StatutoryRule lwf(String state, String emp, String employer,
                              boolean applicable, String remarks) {
        return StatutoryRule.builder()
                .ruleType(RuleType.LWF).state(state).applicable(applicable)
                .amount(new BigDecimal(emp))
                .employerRate(new BigDecimal(employer))
                .effectiveFrom(FROM).remarks(remarks).build();
    }
}