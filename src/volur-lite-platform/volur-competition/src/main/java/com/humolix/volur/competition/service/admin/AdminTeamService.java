package com.humolix.volur.competition.service.admin;

import com.humolix.volur.competition.domain.dto.TeamAuditRequestDTO;
import com.humolix.volur.competition.domain.enums.TeamStatus;
import com.humolix.volur.competition.domain.vo.AdminTeamDetailVO;
import com.humolix.volur.competition.domain.vo.AdminTeamListVO;
import com.humolix.volur.framework.common.api.PageResponse;

public interface AdminTeamService {

    PageResponse<AdminTeamListVO> listTeams(Long competitionId, Integer page, Integer size, TeamStatus status, String keyword);

    AdminTeamDetailVO getTeam(Long teamId);

    void auditTeam(Long teamId, TeamAuditRequestDTO request);

    void deleteTeam(Long teamId);

}
