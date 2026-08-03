package com.complianceiq.controller;

import com.complianceiq.model.StatutoryRule;
import com.complianceiq.model.StatutoryRule.RuleType;
import com.complianceiq.repository.StatutoryRuleRepository;
import com.complianceiq.service.StatutoryRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/statutory-rules")
@RequiredArgsConstructor
public class StatutoryRuleController {

    private final StatutoryRuleRepository ruleRepository;
    private final StatutoryRuleService ruleService;

    /** Saare rules (CA transparency ke liye) */
    @GetMapping
    public List<StatutoryRule> getAll() {
        return ruleRepository.findAll();
    }

    /** Ek type ke rules: /api/statutory-rules/type/PT */
    @GetMapping("/type/{ruleType}")
    public List<StatutoryRule> getByType(@PathVariable RuleType ruleType) {
        return ruleRepository.findByRuleType(ruleType);
    }

    /** Preview calculator - CA khud verify kar sake
     *  /api/statutory-rules/preview?state=HARYANA&basic=20000&gross=45000 */
    @GetMapping("/preview")
    public Map<String, Object> preview(@RequestParam String state,
                                       @RequestParam BigDecimal basic,
                                       @RequestParam BigDecimal gross) {
        var pf  = ruleService.calculatePf(basic);
        var esi = ruleService.calculateEsi(gross);
        var lwf = ruleService.calculateLwf(state);
        BigDecimal pt = ruleService.calculateProfessionalTax(state, gross);

        return Map.of(
                "state", state,
                "pf", Map.of("employee", pf.employee(), "employer", pf.employer(),
                        "pfWage", pf.pfWage(), "cappedAt15000", pf.cappedAtCeiling()),
                "esi", Map.of("applicable", esi.applicable(),
                        "employee", esi.employee(), "employer", esi.employer()),
                "professionalTax", pt,
                "lwf", Map.of("applicable", lwf.applicable(),
                        "employee", lwf.employee(), "employer", lwf.employer())
        );
    }

    /** Rate badalne ke liye (government notification aaye toh) */
    @PutMapping("/{id}")
    public ResponseEntity<StatutoryRule> update(@PathVariable UUID id,
                                                @RequestBody StatutoryRule updated) {
        return ruleRepository.findById(id)
                .map(existing -> {
                    existing.setApplicable(updated.getApplicable());
                    existing.setAmount(updated.getAmount());
                    existing.setEmployeeRate(updated.getEmployeeRate());
                    existing.setEmployerRate(updated.getEmployerRate());
                    existing.setWageCeiling(updated.getWageCeiling());
                    existing.setSlabMin(updated.getSlabMin());
                    existing.setSlabMax(updated.getSlabMax());
                    existing.setRemarks(updated.getRemarks());
                    return ResponseEntity.ok(ruleRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}