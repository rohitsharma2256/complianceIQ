package com.complianceiq.repository;

import com.complianceiq.model.StatutoryRule;
import com.complianceiq.model.StatutoryRule.RuleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StatutoryRuleRepository extends JpaRepository<StatutoryRule, UUID> {

    /** Ek state ke saare slabs (PT ke liye multiple slabs hote hain) */
    List<StatutoryRule> findByRuleTypeAndStateIgnoreCase(RuleType ruleType, String state);

    /** Country-wide single rule (PF, ESI) - state = "ALL" */
    Optional<StatutoryRule> findFirstByRuleTypeAndStateIgnoreCase(RuleType ruleType, String state);

    List<StatutoryRule> findByRuleType(RuleType ruleType);
}