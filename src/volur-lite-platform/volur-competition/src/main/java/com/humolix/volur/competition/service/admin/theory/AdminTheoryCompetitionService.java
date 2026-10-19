package com.humolix.volur.competition.service.admin.theory;

import com.humolix.volur.competition.domain.dto.theory.TheoryCompetitionConfigDTO;
import com.humolix.volur.competition.domain.dto.theory.TheoryCompetitionPaperGenerateRequestDTO;
import com.humolix.volur.competition.domain.vo.theory.TheoryCompetitionConfigVO;
import com.humolix.volur.competition.domain.vo.theory.TheoryPaperQuestionAdminVO;

import java.util.List;

public interface AdminTheoryCompetitionService {

    TheoryCompetitionConfigVO getConfig(Long competitionId);

    TheoryCompetitionConfigVO saveConfig(Long competitionId, TheoryCompetitionConfigDTO request);

    int generatePaper(Long competitionId, TheoryCompetitionPaperGenerateRequestDTO request);

    List<TheoryPaperQuestionAdminVO> listPaperQuestions(Long competitionId);

    void removePaperQuestion(Long competitionId, Long paperId);

}
