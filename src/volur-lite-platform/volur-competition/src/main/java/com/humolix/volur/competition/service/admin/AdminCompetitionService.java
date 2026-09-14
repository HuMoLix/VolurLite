package com.humolix.volur.competition.service.admin;

import com.humolix.volur.competition.domain.dto.CompetitionCreateRequestDTO;
import com.humolix.volur.competition.domain.dto.CompetitionUpdateRequestDTO;
import com.humolix.volur.competition.domain.vo.AdminCompetitionDetailVO;
import com.humolix.volur.competition.domain.vo.AdminCompetitionListVO;
import com.humolix.volur.framework.common.api.PageResponse;

public interface AdminCompetitionService {

    PageResponse<AdminCompetitionListVO> listCompetitions(Integer page, Integer size, String keyword);

    AdminCompetitionDetailVO getCompetition(Long id);

    Long createCompetition(CompetitionCreateRequestDTO request, Long userId);

    void updateCompetition(Long id, CompetitionUpdateRequestDTO request);

    void deleteCompetition(Long id);

    void setScoreboardFrozen(Long id, boolean frozen);

    void archiveCompetition(Long id);

    void unarchiveCompetition(Long id);

}
