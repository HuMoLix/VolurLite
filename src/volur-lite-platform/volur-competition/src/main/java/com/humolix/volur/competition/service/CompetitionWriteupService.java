package com.humolix.volur.competition.service;

import com.humolix.volur.competition.domain.dto.CompetitionWriteupSubmitDTO;
import com.humolix.volur.competition.domain.vo.CompetitionWriteupTemplateVO;
import com.humolix.volur.competition.domain.vo.CompetitionWriteupVO;

import java.util.List;

public interface CompetitionWriteupService {

    List<CompetitionWriteupVO> getMyWriteups(Long competitionId, Long userId);

    CompetitionWriteupVO submitWriteup(Long competitionId, Long userId, CompetitionWriteupSubmitDTO dto);

    CompetitionWriteupTemplateVO getTemplate(Long competitionId);

}
