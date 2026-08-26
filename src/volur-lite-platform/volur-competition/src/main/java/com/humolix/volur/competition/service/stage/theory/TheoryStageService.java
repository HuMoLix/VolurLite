package com.humolix.volur.competition.service.stage.theory;

import com.humolix.volur.competition.domain.dto.stage.theory.TheoryPaperSubmitRequestDTO;
import com.humolix.volur.competition.domain.vo.stage.theory.TheoryPaperExamVO;
import com.humolix.volur.competition.domain.vo.stage.theory.TheoryPaperResultVO;

public interface TheoryStageService {

    TheoryPaperExamVO getPaper(Long competitionId, Long userId);

    TheoryPaperResultVO submitPaper(Long competitionId, Long userId, TheoryPaperSubmitRequestDTO request);

}
