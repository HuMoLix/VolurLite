package com.humolix.volur.logging.service.admin;

import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.logging.domain.dto.AdminLogQueryDTO;
import com.humolix.volur.logging.domain.vo.AdminLogStatisticsVO;
import com.humolix.volur.logging.domain.vo.AdminLogVO;

public interface AdminLogService {

    PageResponse<AdminLogVO> listLogs(AdminLogQueryDTO queryDTO);

    AdminLogStatisticsVO getStatistics();

    AdminLogVO getLogDetail(Long id);

}
