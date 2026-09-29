package com.humolix.volur.competition.aspect;

import com.humolix.volur.competition.domain.entity.Competition;
import com.humolix.volur.competition.domain.enums.CompetitionStatus;
import com.humolix.volur.competition.domain.enums.CompetitionType;
import com.humolix.volur.competition.repository.CompetitionRepository;
import com.humolix.volur.framework.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompetitionStatusValidAspectTest {

    @Mock
    private CompetitionRepository competitionRepository;

    @InjectMocks
    private CompetitionStatusValidAspect aspect;

    private final Long competitionId = 1L;

    @Test
    @DisplayName("报名校验：竞赛尚未发布处于 DRAFT，抛出友好提示")
    void testRegistration_whenDraft_shouldThrowException() {
        Competition comp = Competition.builder()
                .id(competitionId)
                .status(CompetitionStatus.DRAFT)
                .build();
        when(competitionRepository.findById(competitionId)).thenReturn(Optional.of(comp));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                aspect.validateCompetitionStatus(competitionId, new CompetitionStatus[]{CompetitionStatus.REGISTRATION}, null));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("竞赛尚未发布，暂不开放报名", ex.getMessage());
    }

    @Test
    @DisplayName("报名校验：竞赛已结束处于 ENDED，抛出友好提示")
    void testRegistration_whenEnded_shouldThrowException() {
        Instant now = Instant.now();
        Competition comp = Competition.builder()
                .id(competitionId)
                .status(CompetitionStatus.PUBLISHED)
                .startTime(now.minus(2, ChronoUnit.HOURS))
                .endTime(now.minus(1, ChronoUnit.HOURS))
                .build();
        when(competitionRepository.findById(competitionId)).thenReturn(Optional.of(comp));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                aspect.validateCompetitionStatus(competitionId, new CompetitionStatus[]{CompetitionStatus.REGISTRATION}, null));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("竞赛已结束或已归档，报名通道已关闭", ex.getMessage());
    }

    @Test
    @DisplayName("报名校验：比赛已开赛处于 RUNNING，抛出友好提示")
    void testRegistration_whenRunning_shouldThrowException() {
        Instant now = Instant.now();
        Competition comp = Competition.builder()
                .id(competitionId)
                .status(CompetitionStatus.PUBLISHED)
                .startTime(now.minus(1, ChronoUnit.HOURS))
                .endTime(now.plus(2, ChronoUnit.HOURS))
                .build();
        when(competitionRepository.findById(competitionId)).thenReturn(Optional.of(comp));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                aspect.validateCompetitionStatus(competitionId, new CompetitionStatus[]{CompetitionStatus.REGISTRATION}, null));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("比赛已开始，报名通道已关闭", ex.getMessage());
    }

    @Test
    @DisplayName("报名校验：未到报名开始时间，抛出友好提示")
    void testRegistration_whenBeforeRegStart_shouldThrowException() {
        Instant now = Instant.now();
        Competition comp = Competition.builder()
                .id(competitionId)
                .status(CompetitionStatus.PUBLISHED)
                .regStartTime(now.plus(1, ChronoUnit.HOURS))
                .regEndTime(now.plus(5, ChronoUnit.HOURS))
                .startTime(now.plus(6, ChronoUnit.HOURS))
                .endTime(now.plus(12, ChronoUnit.HOURS))
                .build();
        when(competitionRepository.findById(competitionId)).thenReturn(Optional.of(comp));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                aspect.validateCompetitionStatus(competitionId, new CompetitionStatus[]{CompetitionStatus.REGISTRATION}, null));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("报名尚未开启", ex.getMessage());
    }

    @Test
    @DisplayName("报名校验：已过报名截止时间，抛出友好提示")
    void testRegistration_whenAfterRegEnd_shouldThrowException() {
        Instant now = Instant.now();
        Competition comp = Competition.builder()
                .id(competitionId)
                .status(CompetitionStatus.PUBLISHED)
                .regStartTime(now.minus(5, ChronoUnit.HOURS))
                .regEndTime(now.minus(1, ChronoUnit.HOURS))
                .startTime(now.plus(1, ChronoUnit.HOURS))
                .endTime(now.plus(12, ChronoUnit.HOURS))
                .build();
        when(competitionRepository.findById(competitionId)).thenReturn(Optional.of(comp));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                aspect.validateCompetitionStatus(competitionId, new CompetitionStatus[]{CompetitionStatus.REGISTRATION}, null));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("报名已截止", ex.getMessage());
    }

    @Test
    @DisplayName("报名校验：处于报名时间窗口内，动态解析为 REGISTRATION 并放行通过")
    void testRegistration_whenInWindow_shouldPass() {
        Instant now = Instant.now();
        Competition comp = Competition.builder()
                .id(competitionId)
                .status(CompetitionStatus.PUBLISHED)
                .regStartTime(now.minus(1, ChronoUnit.HOURS))
                .regEndTime(now.plus(2, ChronoUnit.HOURS))
                .startTime(now.plus(3, ChronoUnit.HOURS))
                .endTime(now.plus(12, ChronoUnit.HOURS))
                .build();
        when(competitionRepository.findById(competitionId)).thenReturn(Optional.of(comp));

        Competition validated = aspect.validateCompetitionStatus(
                competitionId, new CompetitionStatus[]{CompetitionStatus.REGISTRATION}, null);
        assertNotNull(validated);
        assertEquals(CompetitionStatus.REGISTRATION, validated.getStatus());
    }

    @Test
    @DisplayName("运行校验：处于比赛时间内，动态解析为 RUNNING 并校验赛制通过")
    void testRunning_whenRunningAndTypeMatches_shouldPass() {
        Instant now = Instant.now();
        Competition comp = Competition.builder()
                .id(competitionId)
                .status(CompetitionStatus.PUBLISHED)
                .type(CompetitionType.CTF)
                .startTime(now.minus(1, ChronoUnit.HOURS))
                .endTime(now.plus(2, ChronoUnit.HOURS))
                .build();
        when(competitionRepository.findById(competitionId)).thenReturn(Optional.of(comp));

        Competition validated = aspect.validateCompetitionStatus(
                competitionId, new CompetitionStatus[]{CompetitionStatus.RUNNING}, new CompetitionType[]{CompetitionType.CTF});
        assertNotNull(validated);
        assertEquals(CompetitionStatus.RUNNING, validated.getStatus());
    }

    @Test
    @DisplayName("赛制模式校验：赛制不匹配时抛出异常")
    void testRunning_whenTypeMismatch_shouldThrowException() {
        Instant now = Instant.now();
        Competition comp = Competition.builder()
                .id(competitionId)
                .status(CompetitionStatus.PUBLISHED)
                .type(CompetitionType.AWD)
                .startTime(now.minus(1, ChronoUnit.HOURS))
                .endTime(now.plus(2, ChronoUnit.HOURS))
                .build();
        when(competitionRepository.findById(competitionId)).thenReturn(Optional.of(comp));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                aspect.validateCompetitionStatus(
                        competitionId, new CompetitionStatus[]{CompetitionStatus.RUNNING}, new CompetitionType[]{CompetitionType.CTF}));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("该竞赛模式不满足要求", ex.getMessage());
    }
}
