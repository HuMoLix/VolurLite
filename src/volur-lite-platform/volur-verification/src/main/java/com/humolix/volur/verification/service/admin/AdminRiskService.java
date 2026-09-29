package com.humolix.volur.verification.service.admin;

import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.verification.domain.dto.AdminIpRuleCreateDTO;
import com.humolix.volur.verification.domain.dto.AdminIpRuleQueryDTO;
import com.humolix.volur.verification.domain.dto.AdminRiskPolicyDTO;
import com.humolix.volur.verification.domain.dto.AdminRiskSettingsDTO;
import com.humolix.volur.verification.domain.vo.*;

public interface AdminRiskService {

    AdminRiskSettingsVO getSettings();

    void updateSettings(AdminRiskSettingsDTO dto, Long operatorId);

    AdminRiskPolicyVO getPolicy();

    void updatePolicy(AdminRiskPolicyDTO dto, Long operatorId);

    PageResponse<AdminIpRuleVO> listIpRules(AdminIpRuleQueryDTO queryDTO);

    void createIpRule(AdminIpRuleCreateDTO dto, Long operatorId, String operatorName, String operatorIp);

    void revokeIpRule(Long id, Long operatorId, String operatorName);

    void deleteIpRule(Long id);

    AdminIpDiagnosisVO diagnoseIp(String ip);

    AdminRiskStatisticsVO getStatistics();

    boolean isIpBanned(String ip);

    boolean isIpWhitelisted(String ip);

    void autoBanIp(String ip, String reason);

}
