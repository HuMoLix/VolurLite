package com.humolix.volur.container.service.impl;

import cn.hutool.core.util.IdUtil;
import com.humolix.volur.challenge.entity.stage.ctf.CtfChallenge;
import com.humolix.volur.challenge.entity.stage.ctf.CtfTrainingChallenge;
import com.humolix.volur.challenge.repository.stage.ctf.CtfChallengeRepository;
import com.humolix.volur.challenge.repository.stage.ctf.CtfTrainingChallengeRepository;
import com.humolix.volur.challenge.service.provider.ChallengeContainerProvider;
import com.humolix.volur.container.dto.ContainerRunResultDTO;
import com.humolix.volur.container.entity.CtfChallengeInstance;
import com.humolix.volur.container.entity.enums.InstanceStartupStatus;
import com.humolix.volur.container.repository.CtfChallengeInstanceRepository;
import com.humolix.volur.container.service.CtfInstanceService;
import com.humolix.volur.container.strategy.ContainerRuntimeStrategy;
import com.humolix.volur.container.vo.CtfTrainingChallengeInstanceResponseVO;
import com.humolix.volur.framework.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CtfInstanceServiceImpl implements CtfInstanceService, ChallengeContainerProvider {

    private final CtfChallengeRepository challengeRepository;
    private final CtfTrainingChallengeRepository trainingChallengeRepository;
    private final CtfChallengeInstanceRepository instanceRepository;
    private final ContainerRuntimeStrategy runtimeStrategy;

    private static final int DEFAULT_INSTANCE_DURATION_MINUTES = 30;
    private static final int MAX_EXTEND_MINUTES = 15;

    @Override
    @Transactional
    public CtfTrainingChallengeInstanceResponseVO startTrainingInstance(Long challengeId, Long userId) {
        CtfChallenge challenge = challengeRepository.findByIdAndDeletedFalse(challengeId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "题目不存在"));

        if (!challenge.isDynamicContainer()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "该题目不需要启动动态容器");
        }

        // Check if existing instance is already running
        Optional<CtfChallengeInstance> existing = instanceRepository.findByChallengeIdAndTargetId(challengeId, userId);
        if (existing.isPresent()) {
            CtfChallengeInstance inst = existing.get();
            if (inst.getExpiresAt().isAfter(Instant.now())) {
                long remaining = Duration.between(Instant.now(), inst.getExpiresAt()).getSeconds();
                return CtfTrainingChallengeInstanceResponseVO.builder()
                        .challengeId(challengeId)
                        .accessUrl(inst.getAccessUrl())
                        .status(inst.getStatus())
                        .createdAt(inst.getCreatedAt())
                        .expiresAt(inst.getExpiresAt())
                        .remainingTimeSeconds((int) remaining)
                        .build();
            } else {
                stopTrainingInstance(challengeId, userId);
            }
        }

        long activeCount = instanceRepository.countByTargetIdAndStatus(userId, InstanceStartupStatus.RUNNING);
        if (activeCount >= 2) {
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "最多只能同时开启 2 个训练场容器");
        }

        CtfTrainingChallenge training = trainingChallengeRepository.findByChallengeId(challengeId).orElse(null);
        Integer cpu = training != null ? training.getCpuLimit() : 1;
        Integer mem = training != null ? training.getMemoryLimit() : 128;

        String dynamicFlag = "flag{" + IdUtil.fastSimpleUUID() + "}";
        String resourceName = "volur-ctf-u" + userId + "-c" + challengeId + "-" + System.currentTimeMillis();

        ContainerRunResultDTO runResult = runtimeStrategy.startContainer(resourceName, challenge, dynamicFlag, cpu, mem);

        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofMinutes(DEFAULT_INSTANCE_DURATION_MINUTES));

        CtfChallengeInstance instance = CtfChallengeInstance.builder()
                .challenge(challenge)
                .targetId(userId)
                .stageId(0L)
                .containerId(runResult.getContainerId())
                .accessUrl(runResult.getAccessUrl())
                .flag(dynamicFlag)
                .status(runResult.getStatus())
                .createdAt(now)
                .expiresAt(expiresAt)
                .build();

        instanceRepository.save(instance);

        return CtfTrainingChallengeInstanceResponseVO.builder()
                .challengeId(challengeId)
                .accessUrl(instance.getAccessUrl())
                .status(instance.getStatus())
                .createdAt(instance.getCreatedAt())
                .expiresAt(instance.getExpiresAt())
                .remainingTimeSeconds((int) Duration.between(now, expiresAt).getSeconds())
                .build();
    }

    @Override
    @Transactional
    public void stopTrainingInstance(Long challengeId, Long userId) {
        instanceRepository.findByChallengeIdAndTargetId(challengeId, userId)
                .ifPresent(inst -> {
                    if (inst.getContainerId() != null) {
                        runtimeStrategy.stopContainer(inst.getContainerId());
                    }
                    instanceRepository.delete(inst);
                });
    }

    @Override
    @Transactional
    public CtfTrainingChallengeInstanceResponseVO extendTrainingInstance(Long challengeId, Long userId) {
        CtfChallengeInstance inst = instanceRepository.findByChallengeIdAndTargetId(challengeId, userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "没有正在运行的靶机实例"));

        Instant now = Instant.now();
        if (inst.getExpiresAt().isBefore(now)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "容器已过期，请重新开启");
        }

        Instant newExpiresAt = inst.getExpiresAt().plus(Duration.ofMinutes(MAX_EXTEND_MINUTES));
        inst.setExpiresAt(newExpiresAt);
        instanceRepository.save(inst);

        return CtfTrainingChallengeInstanceResponseVO.builder()
                .challengeId(challengeId)
                .accessUrl(inst.getAccessUrl())
                .status(inst.getStatus())
                .createdAt(inst.getCreatedAt())
                .expiresAt(inst.getExpiresAt())
                .remainingTimeSeconds((int) Duration.between(now, newExpiresAt).getSeconds())
                .build();
    }

    @Override
    public Optional<CtfChallengeInstance> getTrainingInstance(Long challengeId, Long userId) {
        return instanceRepository.findByChallengeIdAndTargetId(challengeId, userId)
                .filter(inst -> inst.getExpiresAt().isAfter(Instant.now()));
    }

    @Override
    public boolean verifyFlag(Long challengeId, Long userId, String flag) {
        if (flag == null) {
            return false;
        }
        return instanceRepository.findByChallengeIdAndTargetId(challengeId, userId)
                .map(inst -> inst.getFlag() != null && inst.getFlag().trim().equalsIgnoreCase(flag.trim()))
                .orElse(false);
    }

    @Override
    public String getAccessUrl(Long challengeId, Long userId) {
        return getTrainingInstance(challengeId, userId)
                .map(CtfChallengeInstance::getAccessUrl)
                .orElse(null);
    }

    @Override
    public Integer getRemainingSeconds(Long challengeId, Long userId) {
        return getTrainingInstance(challengeId, userId)
                .map(inst -> {
                    if (inst.getExpiresAt() == null) return null;
                    long secs = Duration.between(Instant.now(), inst.getExpiresAt()).getSeconds();
                    return (int) Math.max(0, secs);
                })
                .orElse(null);
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void autoCleanExpiredInstances() {
        List<CtfChallengeInstance> expired = instanceRepository.findByExpiresAtBeforeAndStatus(
                Instant.now(), InstanceStartupStatus.RUNNING
        );
        for (CtfChallengeInstance inst : expired) {
            try {
                if (inst.getContainerId() != null) {
                    runtimeStrategy.stopContainer(inst.getContainerId());
                }
                instanceRepository.delete(inst);
            } catch (Exception ignored) {}
        }
    }

}
