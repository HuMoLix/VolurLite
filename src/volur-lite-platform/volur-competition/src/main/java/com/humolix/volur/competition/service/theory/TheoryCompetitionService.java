package com.humolix.volur.competition.service.theory;

import com.humolix.volur.competition.domain.dto.theory.TheoryCheatReportDTO;
import com.humolix.volur.competition.domain.dto.theory.TheoryPaperSubmitRequestDTO;
import com.humolix.volur.competition.domain.dto.theory.TheoryProctorHeartbeatDTO;
import com.humolix.volur.competition.domain.vo.theory.AdminTheoryCandidateMonitorVO;
import com.humolix.volur.competition.domain.vo.theory.TheoryCheatReportVO;
import com.humolix.volur.competition.domain.vo.theory.TheoryPaperExamVO;
import com.humolix.volur.competition.domain.vo.theory.TheoryPaperResultVO;
import com.humolix.volur.competition.domain.vo.theory.TheoryProctorHeartbeatVO;

import java.util.List;

public interface TheoryCompetitionService {

    TheoryPaperExamVO getPaper(Long competitionId, Long userId);

    TheoryPaperResultVO submitPaper(Long competitionId, Long userId, TheoryPaperSubmitRequestDTO request);

    TheoryCheatReportVO reportCheatEvent(Long competitionId, Long userId, TheoryCheatReportDTO request);

    void registerProctorReady(Long competitionId, Long userId);

    TheoryProctorHeartbeatVO recordProctorHeartbeat(Long competitionId, Long userId, TheoryProctorHeartbeatDTO request);

    List<AdminTheoryCandidateMonitorVO> listCandidateMonitors(Long competitionId);

    void uploadHighResSnapshot(Long competitionId, Long userId, com.humolix.volur.competition.domain.dto.theory.TheoryProctorSnapshotDTO request);

    void requestCandidateHighResSnapshot(Long competitionId, Long adminUserId, Long candidateUserId);

    void forwardRtcSignal(Long competitionId, Long senderUserId, com.humolix.volur.competition.domain.dto.theory.TheoryProctorRtcSignalDTO signal);

}
