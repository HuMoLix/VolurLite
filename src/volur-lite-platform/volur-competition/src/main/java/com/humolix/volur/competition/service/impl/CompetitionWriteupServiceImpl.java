package com.humolix.volur.competition.service.impl;

import com.humolix.volur.competition.domain.dto.CompetitionWriteupSubmitDTO;
import com.humolix.volur.competition.domain.entity.Competition;
import com.humolix.volur.competition.domain.entity.CompetitionConfig;
import com.humolix.volur.competition.domain.entity.CompetitionWriteup;
import com.humolix.volur.competition.domain.vo.CompetitionWriteupTemplateVO;
import com.humolix.volur.competition.domain.vo.CompetitionWriteupVO;
import com.humolix.volur.competition.domain.vo.MyTeamInfoVO;
import com.humolix.volur.competition.repository.CompetitionConfigRepository;
import com.humolix.volur.competition.repository.CompetitionRepository;
import com.humolix.volur.competition.repository.CompetitionWriteupRepository;
import com.humolix.volur.competition.service.CompetitionTeamService;
import com.humolix.volur.competition.service.CompetitionWriteupService;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.repository.UserRepository;
import com.humolix.volur.verification.service.CaptchaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CompetitionWriteupServiceImpl implements CompetitionWriteupService {

    private final CompetitionRepository competitionRepository;
    private final CompetitionConfigRepository configRepository;
    private final CompetitionWriteupRepository writeupRepository;
    private final CompetitionTeamService teamService;
    private final UserRepository userRepository;
    private final CaptchaService captchaService;

    @Override
    @Transactional(readOnly = true)
    public List<CompetitionWriteupVO> getMyWriteups(Long competitionId, Long userId) {
        MyTeamInfoVO myTeam = teamService.getMyTeam(competitionId, userId);
        if (myTeam == null || myTeam.getTeamId() == null) {
            return Collections.emptyList();
        }
        Long teamId = (myTeam.getTeamId() instanceof Long) ? (Long) myTeam.getTeamId() : Long.valueOf(myTeam.getTeamId().toString());
        List<CompetitionWriteup> list = writeupRepository.findByCompetitionIdAndTeamIdOrderByCreatedAtDesc(competitionId, teamId);
        return list.stream().map(this::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompetitionWriteupVO submitWriteup(Long competitionId, Long userId, CompetitionWriteupSubmitDTO dto) {
        Competition competition = competitionRepository.findById(competitionId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "竞赛不存在"));

        CompetitionConfig config = configRepository.findByCompetitionId(competitionId).orElse(null);
        if (config != null && Boolean.FALSE.equals(config.getAllowWriteupSubmission())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "当前竞赛未开启 Writeup 提交");
        }

        MyTeamInfoVO myTeam = teamService.getMyTeam(competitionId, userId);
        if (myTeam == null || myTeam.getTeamId() == null) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "您尚未加入或创建参赛战队，无法提交 Writeup");
        }

        Long teamId = (myTeam.getTeamId() instanceof Long) ? (Long) myTeam.getTeamId() : Long.valueOf(myTeam.getTeamId().toString());
        long count = writeupRepository.countByCompetitionIdAndTeamId(competitionId, teamId);
        if (count >= 5) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "每个参赛队伍最多提交 5 次 Writeup");
        }

        boolean captchaOk = captchaService.verifyCaptcha(dto.getCaptchaId(), dto.getCaptchaCode());
        if (!captchaOk) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "验证码错误或已失效，请重新输入");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));

        CompetitionWriteup wp = CompetitionWriteup.builder()
                .competitionId(competitionId)
                .teamId(teamId)
                .teamName(myTeam.getTeamName())
                .userId(userId)
                .username(user.getUsername())
                .fileName(dto.getFileName())
                .fileUrl(dto.getFileUrl())
                .fileSize(dto.getFileSize() != null ? dto.getFileSize() : 0L)
                .fileType("PDF")
                .status("SUBMITTED")
                .build();

        CompetitionWriteup saved = writeupRepository.save(wp);
        return toVO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CompetitionWriteupTemplateVO getTemplate(Long competitionId) {
        CompetitionConfig config = configRepository.findByCompetitionId(competitionId).orElse(null);
        return CompetitionWriteupTemplateVO.builder()
                .templateUrl(config != null ? config.getWriteupTemplateUrl() : null)
                .templateName(config != null ? config.getWriteupTemplateName() : null)
                .build();
    }

    private CompetitionWriteupVO toVO(CompetitionWriteup w) {
        return CompetitionWriteupVO.builder()
                .id(w.getId())
                .competitionId(w.getCompetitionId())
                .teamId(w.getTeamId())
                .teamName(w.getTeamName())
                .userId(w.getUserId())
                .username(w.getUsername())
                .fileName(w.getFileName())
                .fileUrl(w.getFileUrl())
                .fileSize(w.getFileSize())
                .fileType(w.getFileType())
                .status(w.getStatus())
                .score(w.getScore())
                .comment(w.getComment())
                .createdAt(w.getCreatedAt())
                .updatedAt(w.getUpdatedAt())
                .build();
    }

}
