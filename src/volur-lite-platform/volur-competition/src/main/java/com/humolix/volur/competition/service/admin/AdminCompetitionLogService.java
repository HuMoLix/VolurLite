package com.humolix.volur.competition.service.admin;

import com.humolix.volur.competition.domain.dto.AdminCompetitionLogQueryDTO;
import com.humolix.volur.competition.domain.vo.AdminCompetitionLogStatisticsVO;
import com.humolix.volur.competition.domain.vo.AdminCompetitionLogVO;
import com.humolix.volur.framework.common.api.PageResponse;

public interface AdminCompetitionLogService {

    /**
     * 分页查询单场比赛操作日志
     *
     * @param competitionId 比赛 ID
     * @param queryDTO      筛选入参
     * @return 分页视图
     */
    PageResponse<AdminCompetitionLogVO> listLogs(Long competitionId, AdminCompetitionLogQueryDTO queryDTO);

    /**
     * 获取单场比赛日志统计指标
     *
     * @param competitionId 比赛 ID
     * @return 统计指标
     */
    AdminCompetitionLogStatisticsVO getStatistics(Long competitionId);

    /**
     * 获取指定日志记录详情
     *
     * @param competitionId 比赛 ID
     * @param id            日志主键 ID
     * @return 日志详情
     */
    AdminCompetitionLogVO getLogDetail(Long competitionId, Long id);

}
