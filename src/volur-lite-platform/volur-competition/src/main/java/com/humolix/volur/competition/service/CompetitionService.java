package com.humolix.volur.competition.service;

import com.humolix.volur.competition.domain.vo.CompetitionAboutVO;
import com.humolix.volur.competition.domain.vo.CompetitionDetailVO;
import com.humolix.volur.competition.domain.vo.CompetitionSummaryVO;
import com.humolix.volur.framework.common.api.PageResponse;

public interface CompetitionService {

    PageResponse<CompetitionSummaryVO> listCompetitions(Integer page, Integer size, String keyword);

    CompetitionDetailVO getCompetitionBySlug(String slug);

    CompetitionDetailVO getCompetitionById(Long id);

    CompetitionAboutVO getCompetitionAbout(Long id);

}
