package com.humolix.volur.competition.service;

import com.humolix.volur.competition.domain.dto.TeamInviteAcceptDTO;
import com.humolix.volur.competition.domain.dto.TeamSignupRequestDTO;
import com.humolix.volur.competition.domain.dto.TeamUpdateRequestDTO;
import com.humolix.volur.competition.domain.vo.CompetitionTeamVO;
import com.humolix.volur.competition.domain.vo.MyTeamInfoVO;

import java.util.List;

public interface CompetitionTeamService {

    Long signupTeam(Long competitionId, Long captainUserId, TeamSignupRequestDTO request);

    void updateTeam(Long competitionId, Long captainUserId, TeamUpdateRequestDTO request);

    void acceptInvitation(Long competitionId, Long teamId, Long userId, TeamInviteAcceptDTO request);

    void rejectInvitation(Long competitionId, Long teamId, Long userId);

    void cancelInvitation(Long competitionId, Long teamId, Long captainUserId, Long targetUserId);

    MyTeamInfoVO getMyTeam(Long competitionId, Long userId);

    List<CompetitionTeamVO> listTeams(Long competitionId);

}
