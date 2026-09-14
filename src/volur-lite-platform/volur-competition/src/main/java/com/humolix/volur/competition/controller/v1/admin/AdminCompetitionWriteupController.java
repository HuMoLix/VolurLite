package com.humolix.volur.competition.controller.v1.admin;

import com.humolix.volur.competition.domain.dto.CompetitionWriteupAuditDTO;
import com.humolix.volur.competition.domain.entity.CompetitionWriteup;
import com.humolix.volur.competition.domain.vo.CompetitionWriteupVO;
import com.humolix.volur.competition.repository.CompetitionWriteupRepository;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.logging.annotation.Syslog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "管理端题解 WP 管理", description = "管理员查阅、评审与下载参赛队伍提交的 Writeup 题解接口")
@RestController
@RequestMapping("/api/v1/admin/competitions/{competitionId}/writeups")
@RequiredArgsConstructor
public class AdminCompetitionWriteupController {

    private final CompetitionWriteupRepository writeupRepository;

    @Operation(summary = "获取指定比赛所有队伍提交的 Writeup 列表")
    @GetMapping
    public ApiResponse<List<CompetitionWriteupVO>> listWriteups(
            @PathVariable Long competitionId,
            @RequestParam(required = false) String keyword) {

        List<CompetitionWriteup> list = writeupRepository.findByCompetitionIdOrderByCreatedAtDesc(competitionId);
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim().toLowerCase();
            list = list.stream().filter(w ->
                    (w.getTeamName() != null && w.getTeamName().toLowerCase().contains(kw)) ||
                    (w.getUsername() != null && w.getUsername().toLowerCase().contains(kw)) ||
                    (w.getFileName() != null && w.getFileName().toLowerCase().contains(kw))
            ).toList();
        }

        List<CompetitionWriteupVO> voList = list.stream().map(this::toVO).toList();
        return ApiResponseEntity.ok("获取队伍 Writeup 列表成功", voList);
    }

    @Operation(summary = "获取单个 Writeup 详情")
    @GetMapping("/{writeupId}")
    public ApiResponse<CompetitionWriteupVO> getWriteup(
            @PathVariable Long competitionId,
            @PathVariable Long writeupId) {

        CompetitionWriteup wp = writeupRepository.findById(writeupId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Writeup 不存在"));

        return ApiResponseEntity.ok("获取 Writeup 详情成功", toVO(wp));
    }

    @Operation(summary = "审核与评分队伍 Writeup")
    @Syslog(module = "COMPETITION", value = "审核队伍 Writeup", recordParams = true)
    @PutMapping("/{writeupId}/audit")
    @Transactional
    public ApiResponse<CompetitionWriteupVO> auditWriteup(
            @PathVariable Long competitionId,
            @PathVariable Long writeupId,
            @RequestBody CompetitionWriteupAuditDTO dto) {

        CompetitionWriteup wp = writeupRepository.findById(writeupId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Writeup 不存在"));

        if (StringUtils.hasText(dto.getStatus())) {
            wp.setStatus(dto.getStatus().trim().toUpperCase());
        }
        if (dto.getScore() != null) {
            wp.setScore(dto.getScore());
        }
        if (dto.getComment() != null) {
            wp.setComment(dto.getComment());
        }

        CompetitionWriteup saved = writeupRepository.save(wp);
        return ApiResponseEntity.ok("审核 Writeup 成功", toVO(saved));
    }

    @Operation(summary = "删除队伍 Writeup")
    @Syslog(module = "COMPETITION", value = "删除队伍 Writeup", recordParams = true)
    @DeleteMapping("/{writeupId}")
    @Transactional
    public ApiResponse<Void> deleteWriteup(
            @PathVariable Long competitionId,
            @PathVariable Long writeupId) {

        CompetitionWriteup wp = writeupRepository.findById(writeupId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Writeup 不存在"));

        writeupRepository.delete(wp);
        return ApiResponseEntity.success("删除队伍 Writeup 成功");
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
