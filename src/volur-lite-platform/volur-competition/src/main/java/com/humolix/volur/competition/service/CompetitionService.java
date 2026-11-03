package com.humolix.volur.competition.service;

import com.humolix.volur.competition.domain.enums.CompetitionType;
import com.humolix.volur.competition.domain.vo.CompetitionAboutVO;
import com.humolix.volur.competition.domain.vo.CompetitionAnnouncementVO;
import com.humolix.volur.competition.domain.vo.CompetitionDetailVO;
import com.humolix.volur.competition.domain.vo.CompetitionSummaryVO;
import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.framework.common.api.SliceResponse;

import java.util.List;

public interface CompetitionService {

    PageResponse<CompetitionSummaryVO> listCompetitions(Integer page, Integer size, String keyword);

    SliceResponse<CompetitionSummaryVO> listCompetitionSlice(Integer page, Integer size, String keyword, CompetitionType type);

    CompetitionDetailVO getCompetitionBySlug(String slug);

    CompetitionDetailVO getCompetitionById(Long id);

    CompetitionAboutVO getCompetitionAbout(Long id);

    List<CompetitionAnnouncementVO> listAnnouncements(Long competitionId, Long userId);

}
