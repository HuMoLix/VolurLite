package com.humolix.volur.competition.controller.v1.admin;

import com.humolix.volur.competition.domain.dto.CompetitionAnnouncementDTO;
import com.humolix.volur.competition.domain.entity.CompetitionAnnouncement;
import com.humolix.volur.competition.domain.vo.CompetitionAnnouncementVO;
import com.humolix.volur.competition.repository.CompetitionAnnouncementRepository;
import com.humolix.volur.competition.repository.CompetitionRepository;
import com.humolix.volur.framework.cache.CacheNames;
import com.humolix.volur.framework.cache.VolurCacheTemplate;
import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.framework.security.annotation.CurrentUserId;
import com.humolix.volur.logging.annotation.Syslog;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/competitions/{competitionId}/announcements")
@RequiredArgsConstructor
public class AdminCompetitionAnnouncementController {

    private final CompetitionAnnouncementRepository announcementRepository;
    private final CompetitionRepository competitionRepository;
    private final UserRepository userRepository;
    private final VolurCacheTemplate cacheTemplate;

    @GetMapping
    public ApiResponseEntity<List<CompetitionAnnouncementVO>> listAnnouncements(@PathVariable Long competitionId) {
        List<CompetitionAnnouncement> list = announcementRepository.findByCompetitionIdOrderByIsPinnedDescCreatedAtDesc(competitionId);
        List<CompetitionAnnouncementVO> voList = list.stream().map(this::toVO).toList();
        return ApiResponseEntity.ok("获取比赛公告成功", voList);
    }

    @Syslog(module = "COMPETITION", value = "发布比赛公告", recordParams = true)
    @PostMapping
    @Transactional
    public ApiResponseEntity<CompetitionAnnouncementVO> createAnnouncement(
            @PathVariable Long competitionId,
            @Valid @RequestBody CompetitionAnnouncementDTO dto,
            @CurrentUserId Long userId) {

        if (!competitionRepository.existsById(competitionId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "竞赛不存在");
        }

        String authorName = "Admin";
        if (userId != null) {
            authorName = userRepository.findById(userId).map(User::getUsername).orElse("Admin");
        }

        CompetitionAnnouncement anno = CompetitionAnnouncement.builder()
                .competitionId(competitionId)
                .title(dto.getTitle().trim())
                .content(dto.getContent())
                .level(StringUtils.hasText(dto.getLevel()) ? dto.getLevel().trim().toUpperCase() : "INFO")
                .isPinned(dto.getIsPinned() != null && dto.getIsPinned())
                .challengeId(dto.getChallengeId())
                .challengeName(dto.getChallengeName())
                .createdById(userId)
                .createdByName(authorName)
                .build();

        CompetitionAnnouncement saved = announcementRepository.save(anno);
        cacheTemplate.evict(CacheNames.COMPETITION_STATUS, "announcements:" + competitionId);
        return ApiResponseEntity.created("发布比赛公告成功", toVO(saved));
    }

    @Syslog(module = "COMPETITION", value = "更新比赛公告", recordParams = true)
    @PutMapping("/{announcementId}")
    @Transactional
    public ApiResponseEntity<CompetitionAnnouncementVO> updateAnnouncement(
            @PathVariable Long competitionId,
            @PathVariable Long announcementId,
            @Valid @RequestBody CompetitionAnnouncementDTO dto) {

        CompetitionAnnouncement anno = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "比赛公告不存在"));

        if (!anno.getCompetitionId().equals(competitionId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "公告与比赛不匹配");
        }

        anno.setTitle(dto.getTitle().trim());
        anno.setContent(dto.getContent());
        if (StringUtils.hasText(dto.getLevel())) {
            anno.setLevel(dto.getLevel().trim().toUpperCase());
        }
        if (dto.getIsPinned() != null) {
            anno.setIsPinned(dto.getIsPinned());
        }
        anno.setChallengeId(dto.getChallengeId());
        anno.setChallengeName(dto.getChallengeName());

        CompetitionAnnouncement updated = announcementRepository.save(anno);
        cacheTemplate.evict(CacheNames.COMPETITION_STATUS, "announcements:" + competitionId);
        return ApiResponseEntity.ok("更新比赛公告成功", toVO(updated));
    }

    @Syslog(module = "COMPETITION", value = "切换比赛公告置顶状态", recordParams = true)
    @PostMapping("/{announcementId}/pin")
    @Transactional
    public ApiResponseEntity<Void> togglePin(
            @PathVariable Long competitionId,
            @PathVariable Long announcementId,
            @RequestParam boolean pinned) {

        CompetitionAnnouncement anno = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "比赛公告不存在"));

        anno.setIsPinned(pinned);
        announcementRepository.save(anno);
        cacheTemplate.evict(CacheNames.COMPETITION_STATUS, "announcements:" + competitionId);
        return ApiResponseEntity.success(pinned ? "公告已置顶" : "已取消置顶");
    }

    @Syslog(module = "COMPETITION", value = "删除比赛公告", recordParams = true)
    @DeleteMapping("/{announcementId}")
    @Transactional
    public ApiResponseEntity<Void> deleteAnnouncement(
            @PathVariable Long competitionId,
            @PathVariable Long announcementId) {

        CompetitionAnnouncement anno = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "比赛公告不存在"));

        announcementRepository.delete(anno);
        cacheTemplate.evict(CacheNames.COMPETITION_STATUS, "announcements:" + competitionId);
        return ApiResponseEntity.success("删除比赛公告成功");
    }

    private CompetitionAnnouncementVO toVO(CompetitionAnnouncement e) {
        return CompetitionAnnouncementVO.builder()
                .id(e.getId())
                .competitionId(e.getCompetitionId())
                .title(e.getTitle())
                .content(e.getContent())
                .level(e.getLevel())
                .isPinned(e.getIsPinned())
                .challengeId(e.getChallengeId())
                .challengeName(e.getChallengeName())
                .createdById(e.getCreatedById())
                .createdByName(e.getCreatedByName())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

}
