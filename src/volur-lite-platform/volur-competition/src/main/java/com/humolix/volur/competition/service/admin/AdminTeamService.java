package com.humolix.volur.competition.service.admin;

import com.humolix.volur.competition.domain.dto.AdminTeamUpdateDTO;
import com.humolix.volur.competition.domain.dto.BatchTeamAuditDTO;
import com.humolix.volur.competition.domain.dto.TeamAuditRequestDTO;
import com.humolix.volur.competition.domain.enums.TeamStatus;
import com.humolix.volur.competition.domain.vo.AdminTeamDetailVO;
import com.humolix.volur.competition.domain.vo.AdminTeamListVO;
import com.humolix.volur.competition.domain.vo.AdminTeamStatsVO;
import com.humolix.volur.framework.common.api.PageResponse;

import java.util.List;

public interface AdminTeamService {

    PageResponse<AdminTeamListVO> listTeams(Long competitionId, Integer page, Integer size, TeamStatus status, String keyword);

    AdminTeamStatsVO getTeamStats(Long competitionId);

    AdminTeamDetailVO getTeam(Long teamId);

    void auditTeam(Long teamId, TeamAuditRequestDTO request);

    void updateTeam(Long teamId, AdminTeamUpdateDTO request);

    void batchAuditTeams(Long competitionId, BatchTeamAuditDTO request);

    void batchDeleteTeams(Long competitionId, List<Long> teamIds);

    void deleteTeam(Long teamId);

}
