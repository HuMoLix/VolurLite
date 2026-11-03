package com.humolix.volur.competition.service.admin.ctf;

import com.humolix.volur.competition.domain.vo.ctf.CtfCompetitionSubmissionVO;
import com.humolix.volur.framework.common.api.PageResponse;

public interface AdminCtfSubmissionService {

    PageResponse<CtfCompetitionSubmissionVO> listSubmissions(
            Long competitionId,
            int page,
            int size,
            Long challengeId,
            String keyword);

}
