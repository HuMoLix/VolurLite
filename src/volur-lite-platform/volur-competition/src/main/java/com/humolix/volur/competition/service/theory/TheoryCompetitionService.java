package com.humolix.volur.competition.service.theory;

import com.humolix.volur.competition.domain.dto.theory.TheoryPaperSubmitRequestDTO;
import com.humolix.volur.competition.domain.vo.theory.TheoryPaperExamVO;
import com.humolix.volur.competition.domain.vo.theory.TheoryPaperResultVO;

public interface TheoryCompetitionService {

    TheoryPaperExamVO getPaper(Long competitionId, Long userId);

    TheoryPaperResultVO submitPaper(Long competitionId, Long userId, TheoryPaperSubmitRequestDTO request);

}
