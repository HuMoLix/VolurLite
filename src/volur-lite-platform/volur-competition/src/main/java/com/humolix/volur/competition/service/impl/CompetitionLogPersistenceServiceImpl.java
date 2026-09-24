package com.humolix.volur.competition.service.impl;

import com.humolix.volur.challenge.domain.entity.ctf.CtfChallenge;
import com.humolix.volur.challenge.repository.ctf.CtfChallengeRepository;
import com.humolix.volur.competition.domain.dto.CompetitionLogContextDTO;
import com.humolix.volur.competition.domain.entity.CompetitionMember;
import com.humolix.volur.competition.domain.entity.CompetitionOperationLog;
import com.humolix.volur.competition.domain.entity.ctf.CtfCompetitionChallenge;
import com.humolix.volur.competition.repository.CompetitionMemberRepository;
import com.humolix.volur.competition.repository.CompetitionOperationLogRepository;
import com.humolix.volur.competition.repository.ctf.CtfCompetitionChallengeRepository;
import com.humolix.volur.competition.service.CompetitionLogPersistenceService;
import com.humolix.volur.framework.common.thread.VolurThreadManager;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompetitionLogPersistenceServiceImpl implements CompetitionLogPersistenceService, SmartLifecycle {

    public static final String VREDIS_COMPETITION_LOG_BUFFER_KEY = "vredis:competition:log:buffer";
    private static final int BATCH_SIZE = 50;

    private final VolurThreadManager volurThreadManager;
    private final CompetitionOperationLogRepository logRepository;
    private final UserRepository userRepository;
    private final CompetitionMemberRepository memberRepository;
    private final CtfCompetitionChallengeRepository ctfCompChallengeRepository;
    private final CtfChallengeRepository ctfChallengeRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    private volatile boolean running = false;

    @Override
    public void saveLogAsync(CompetitionLogContextDTO context) {
        if (context == null || context.getCompetitionId() == null) {
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(context);
            Long queueSize = stringRedisTemplate.opsForList().rightPush(VREDIS_COMPETITION_LOG_BUFFER_KEY, json);

            // 自适应触发: 若瞬时积攒达到微批上限，立即利用虚拟线程触发落库，无需等待定时周期
            if (queueSize != null && queueSize >= BATCH_SIZE) {
                volurThreadManager.runAsync(this::drainBuffer);
            }
        } catch (Exception e) {
            log.warn("[CompetitionLog] 推入 Redis 日志缓冲队列失败，自动降级为虚拟线程直写: {}", e.getMessage());
            volurThreadManager.runAsync(() -> {
                try {
                    doSaveSingleLog(context);
                } catch (Exception ex) {
                    log.error("[CompetitionLog] 降级写入竞赛日志异常: {}", ex.getMessage());
                }
            });
        }
    }

    /**
     * 200ms 高频微批处理定时轮询器（由 SchedulingConfig 配置的 Java 25 虚拟线程执行）
     */
    @Scheduled(fixedDelay = 200)
    public void drainBuffer() {
        try {
            while (true) {
                List<String> rawLogs = stringRedisTemplate.opsForList().leftPop(VREDIS_COMPETITION_LOG_BUFFER_KEY, BATCH_SIZE);
                if (CollectionUtils.isEmpty(rawLogs)) {
                    break;
                }
                processBatch(rawLogs);
                if (rawLogs.size() < BATCH_SIZE) {
                    break;
                }
            }
        } catch (Exception e) {
            log.error("[CompetitionLog] 消费 Redis 日志缓冲区批量入库异常: {}", e.getMessage());
        }
    }

    @Override
    public void flushBuffer() {
        drainBuffer();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void processBatch(List<String> rawLogs) {
        List<CompetitionOperationLog> entities = new ArrayList<>(rawLogs.size());

        for (String raw : rawLogs) {
            try {
                CompetitionLogContextDTO ctx = objectMapper.readValue(raw, CompetitionLogContextDTO.class);
                if (ctx != null && ctx.getCompetitionId() != null) {
                    entities.add(buildEntity(ctx));
                }
            } catch (Exception e) {
                log.warn("[CompetitionLog] 反序列化日志快照失败，丢弃损坏条目: {}", e.getMessage());
            }
        }

        if (!entities.isEmpty()) {
            logRepository.saveAll(entities);
            log.debug("[CompetitionLog] 成功批量落库 {} 条竞赛日志", entities.size());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void doSaveSingleLog(CompetitionLogContextDTO ctx) {
        CompetitionOperationLog entity = buildEntity(ctx);
        logRepository.save(entity);
    }

    private CompetitionOperationLog buildEntity(CompetitionLogContextDTO ctx) {
        // 1. 异步补全用户名
        String username = ctx.getUsername();
        if (!StringUtils.hasText(username) && ctx.getUserId() != null) {
            try {
                Optional<User> userOpt = userRepository.findById(ctx.getUserId());
                if (userOpt.isPresent()) {
                    username = userOpt.get().getUsername();
                }
            } catch (Exception ignored) {
            }
        }
        if (!StringUtils.hasText(username)) {
            username = "ANONYMOUS";
        }

        // 2. 异步补全队伍信息
        Long teamId = ctx.getTeamId();
        String teamName = ctx.getTeamName();
        if ((!StringUtils.hasText(teamName) || teamId == null) && ctx.getCompetitionId() != null && ctx.getUserId() != null) {
            try {
                Optional<CompetitionMember> memberOpt = memberRepository.findMemberWithTeam(ctx.getCompetitionId(), ctx.getUserId());
                if (memberOpt.isPresent() && memberOpt.get().getTeam() != null) {
                    teamId = memberOpt.get().getTeam().getId();
                    teamName = memberOpt.get().getTeam().getTeamName();
                }
            } catch (Exception ignored) {
            }
        }

        // 3. 异步补全题目名称
        String challengeTitle = ctx.getChallengeTitle();
        if (!StringUtils.hasText(challengeTitle) && ctx.getChallengeId() != null) {
            try {
                Optional<CtfCompetitionChallenge> compChalOpt = ctfCompChallengeRepository.findByIdWithChallenge(ctx.getChallengeId());
                if (compChalOpt.isPresent() && compChalOpt.get().getChallenge() != null) {
                    challengeTitle = compChalOpt.get().getChallenge().getName();
                } else {
                    Optional<CtfChallenge> rawChalOpt = ctfChallengeRepository.findById(ctx.getChallengeId());
                    if (rawChalOpt.isPresent()) {
                        challengeTitle = rawChalOpt.get().getName();
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return CompetitionOperationLog.builder()
                .competitionId(ctx.getCompetitionId())
                .userId(ctx.getUserId())
                .username(username)
                .teamId(teamId)
                .teamName(teamName)
                .challengeId(ctx.getChallengeId())
                .challengeTitle(challengeTitle)
                .action(ctx.getAction())
                .logType(ctx.getLogType())
                .status(ctx.getStatus() != null ? ctx.getStatus() : true)
                .operatorIp(ctx.getOperatorIp())
                .requestMethod(ctx.getRequestMethod())
                .requestUri(ctx.getRequestUri())
                .durationMs(ctx.getDurationMs())
                .params(ctx.getParams())
                .result(ctx.getResult())
                .errorMessage(ctx.getErrorMessage())
                .userAgent(ctx.getUserAgent())
                .build();
    }

    @Override
    public void start() {
        this.running = true;
    }

    @Override
    public void stop() {
        if (!this.running) {
            return;
        }
        this.running = false;
        log.info("[CompetitionLog] 服务优雅停机，正在执行 Redis 日志缓冲区收尾落库...");
        drainBuffer();
    }

    @Override
    public boolean isRunning() {
        return this.running;
    }

    @Override
    public int getPhase() {
        // 赋予大于 LettuceConnectionFactory(phase=0) 的高优先级，确保在 Redis 连接池关闭前执行收尾
        return Integer.MAX_VALUE - 1000;
    }

}
