package com.humolix.volur.verification.repository;

import com.humolix.volur.verification.domain.entity.RiskIpRule;
import com.humolix.volur.verification.domain.enums.IpRuleSource;
import com.humolix.volur.verification.domain.enums.IpRuleStatus;
import com.humolix.volur.verification.domain.enums.IpRuleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface RiskIpRuleRepository extends JpaRepository<RiskIpRule, Long>, JpaSpecificationExecutor<RiskIpRule> {

    Optional<RiskIpRule> findFirstByIpAndRuleTypeAndStatusOrderByCreatedAtDesc(String ip, IpRuleType ruleType, IpRuleStatus status);

    List<RiskIpRule> findByIpAndStatus(String ip, IpRuleStatus status);

    List<RiskIpRule> findByRuleTypeAndStatus(IpRuleType ruleType, IpRuleStatus status);

    long countByRuleTypeAndStatus(IpRuleType ruleType, IpRuleStatus status);

    long countBySourceTypeAndCreatedAtAfter(IpRuleSource sourceType, Instant after);

    long countByRuleType(IpRuleType ruleType);
}
