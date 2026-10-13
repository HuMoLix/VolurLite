package com.humolix.volur.competition.controller.v1.admin;

import com.humolix.volur.competition.domain.dto.CompetitionWriteupAuditDTO;
import com.humolix.volur.competition.domain.entity.CompetitionWriteup;
import com.humolix.volur.competition.domain.vo.CompetitionWriteupVO;
import com.humolix.volur.competition.repository.CompetitionWriteupRepository;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.logging.annotation.Syslog;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/competitions/{competitionId}/writeups")
@RequiredArgsConstructor
public class AdminCompetitionWriteupController {

    private final CompetitionWriteupRepository writeupRepository;

    @GetMapping
    public ApiResponseEntity<PageResponse<CompetitionWriteupVO>> listWriteups(
            @PathVariable Long competitionId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {

        int validPage = Math.max(page, 1);
        int validSize = Math.max(size, 1);
        Pageable pageable = PageRequest.of(validPage - 1, validSize);
        String kw = StringUtils.hasText(keyword) ? keyword.trim() : null;
        Page<CompetitionWriteup> pageResult = writeupRepository.searchByCompetitionIdAndKeyword(competitionId, kw, pageable);
        return ApiResponseEntity.ok("获取队伍 Writeup 列表成功", PageResponse.of(pageResult.map(this::toVO)));
    }

    @GetMapping("/{writeupId}")
    public ApiResponseEntity<CompetitionWriteupVO> getWriteup(
            @PathVariable Long competitionId,
            @PathVariable Long writeupId) {

        CompetitionWriteup wp = writeupRepository.findById(writeupId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Writeup 不存在"));

        return ApiResponseEntity.ok("获取 Writeup 详情成功", toVO(wp));
    }

    @Syslog(module = "COMPETITION", value = "审核队伍 Writeup", recordParams = true)
    @PutMapping("/{writeupId}/audit")
    @Transactional
    public ApiResponseEntity<CompetitionWriteupVO> auditWriteup(
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

    @Syslog(module = "COMPETITION", value = "删除队伍 Writeup", recordParams = true)
    @DeleteMapping("/{writeupId}")
    @Transactional
    public ApiResponseEntity<Void> deleteWriteup(
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
