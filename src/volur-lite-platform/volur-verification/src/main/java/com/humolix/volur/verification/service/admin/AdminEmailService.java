package com.humolix.volur.verification.service.admin;

import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.verification.domain.dto.AdminEmailQueryDTO;
import com.humolix.volur.verification.domain.dto.AdminSendEmailDTO;
import com.humolix.volur.verification.domain.vo.AdminEmailStatisticsVO;
import com.humolix.volur.verification.domain.vo.EmailLogVO;

public interface AdminEmailService {

    PageResponse<EmailLogVO> listEmails(AdminEmailQueryDTO queryDTO);

    AdminEmailStatisticsVO getStatistics();

    EmailLogVO getEmailDetail(Long id);

    void sendManualEmail(AdminSendEmailDTO sendDTO, Long operatorId);

    void resendEmail(Long id, Long operatorId);

}
