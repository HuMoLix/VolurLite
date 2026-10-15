package com.humolix.volur.container.service.impl;

import com.humolix.volur.challenge.domain.entity.ctf.CtfChallenge;
import com.humolix.volur.challenge.repository.ctf.CtfChallengeRepository;
import com.humolix.volur.container.domain.dto.ContainerRunResultDTO;
import com.humolix.volur.container.domain.entity.ClusterNode;
import com.humolix.volur.container.domain.entity.CtfChallengeInstance;
import com.humolix.volur.container.domain.enums.InstanceStartupStatus;
import com.humolix.volur.container.domain.vo.ChallengeInstanceResponseVO;
import com.humolix.volur.container.flag.DynamicFlagContext;
import com.humolix.volur.container.flag.DynamicFlagGenerator;
import com.humolix.volur.container.repository.ClusterImageRepository;
import com.humolix.volur.container.repository.ClusterNodeRepository;
import com.humolix.volur.container.repository.CtfChallengeInstanceRepository;
import com.humolix.volur.container.service.CtfInstanceService;
import com.humolix.volur.container.service.admin.AdminImageService;
import com.humolix.volur.container.strategy.ContainerRuntimeStrategy;
import com.humolix.volur.container.strategy.DockerSwarmRuntimeStrategy;
import com.humolix.volur.container.strategy.K3sRuntimeStrategy;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.system.constant.SystemSettingKeys;
import com.humolix.volur.system.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class CtfInstanceServiceImpl implements CtfInstanceService {

    private final CtfChallengeRepository challengeRepository;
    private final CtfChallengeInstanceRepository instanceRepository;
    private final DynamicFlagGenerator dynamicFlagGenerator;
    private final SystemSettingService systemSettingService;
    private final K3sRuntimeStrategy k3sRuntimeStrategy;
    private final DockerSwarmRuntimeStrategy dockerSwarmRuntimeStrategy;
    private final ClusterNodeRepository clusterNodeRepository;
    private final ClusterImageRepository clusterImageRepository;
    private final AdminImageService adminImageService;

    private static final int DEFAULT_INSTANCE_DURATION_MINUTES = 30;
    private static final int MAX_EXTEND_MINUTES = 15;

    private ContainerRuntimeStrategy getRuntimeStrategy() {
        String containerType = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_TYPE, "DOCKER_SWARM");
        if ("DOCKER_SWARM".equalsIgnoreCase(containerType)) {
            return dockerSwarmRuntimeStrategy;
        }
        return k3sRuntimeStrategy;
    }

    private void stopContainerSafely(String containerId) {
        if (!StringUtils.hasText(containerId)) {
            return;
        }
        if (containerId.startsWith("swarm-")) {
            dockerSwarmRuntimeStrategy.stopContainer(containerId);
        } else if (containerId.startsWith("pod-") || containerId.startsWith("mock-")) {
            k3sRuntimeStrategy.stopContainer(containerId);
        } else {
            getRuntimeStrategy().stopContainer(containerId);
        }
    }

    @Override
    @Transactional
    public ChallengeInstanceResponseVO startInstance(Long challengeId, Long userId) {
        return startInstance(challengeId, userId, 0L, null, null);
    }

    @Override
    @Transactional
    public ChallengeInstanceResponseVO startInstance(Long challengeId, Long userId, Long stageId, Long teamId, String teamToken) {
        CtfChallenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "题目不存在"));

        if (!challenge.isDynamicContainer()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "该题目不需要启动动态容器");
        }

        Optional<CtfChallengeInstance> existing = Optional.empty();
        if (stageId != null && stageId > 0 && teamId != null) {
            existing = instanceRepository.findByStageIdAndTeamIdAndChallengeId(stageId, teamId, challengeId);
        }
        if (existing.isEmpty()) {
            existing = instanceRepository.findByChallengeIdAndTargetId(challengeId, userId);
        }
        if (existing.isPresent()) {
            CtfChallengeInstance inst = existing.get();
            if (inst.getExpiresAt().isAfter(Instant.now())) {
                long remaining = Duration.between(Instant.now(), inst.getExpiresAt()).getSeconds();
                return ChallengeInstanceResponseVO.builder()
                        .challengeId(challengeId)
                        .accessUrl(inst.getAccessUrl())
                        .status(inst.getStatus())
                        .createdAt(inst.getCreatedAt())
                        .expiresAt(inst.getExpiresAt())
                        .remainingTimeSeconds((int) remaining)
                        .build();
            } else {
                stopContainerSafely(inst.getContainerId());
                instanceRepository.delete(inst);
            }
        }

        if (stageId == null || stageId == 0L) {
            long activeCount = instanceRepository.countByTargetIdAndStatus(userId, InstanceStartupStatus.RUNNING);
            if (activeCount >= 2) {
                throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "最多只能同时开启 2 个容器");
            }
        }

        Integer cpu = challenge.getCpuLimit() != null ? challenge.getCpuLimit() : 1;
        Integer mem = challenge.getMemoryLimit() != null ? challenge.getMemoryLimit() : 128;

        DynamicFlagContext flagContext = DynamicFlagContext.builder()
                .userId(userId)
                .challengeId(challengeId)
                .stageId(stageId != null ? stageId : 0L)
                .teamId(teamId)
                .teamToken(teamToken)
                .build();

        String dynamicFlag = dynamicFlagGenerator.generateFlag(challenge.getFlag(), flagContext);
        String resourceName = "volur-u" + userId + "-c" + challengeId + "-" + System.currentTimeMillis();

        String targetNodeIp = selectWorkerNodeForUser(challenge);
        String containerType = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_TYPE, "DOCKER_SWARM");

        if ("DOCKER_SWARM".equalsIgnoreCase(containerType) && StringUtils.hasText(targetNodeIp) && StringUtils.hasText(challenge.getImageName())) {
            adminImageService.ensureImageOnNode(targetNodeIp, challenge.getImageName());
        }

        ContainerRunResultDTO runResult = getRuntimeStrategy().startContainer(resourceName, challenge, dynamicFlag, cpu, mem, targetNodeIp);

        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofMinutes(DEFAULT_INSTANCE_DURATION_MINUTES));

        CtfChallengeInstance instance = CtfChallengeInstance.builder()
                .challenge(challenge)
                .targetId(userId)
                .stageId(stageId != null ? stageId : 0L)
                .teamId(teamId)
                .imageName(challenge.getImageName())
                .containerId(runResult.getContainerId())
                .accessUrl(runResult.getAccessUrl())
                .flag(dynamicFlag)
                .nodeIp(targetNodeIp)
                .status(runResult.getStatus())
                .containerType(containerType)
                .createdAt(now)
                .expiresAt(expiresAt)
                .build();

        instanceRepository.save(instance);

        return ChallengeInstanceResponseVO.builder()
                .challengeId(challengeId)
                .accessUrl(instance.getAccessUrl())
                .status(instance.getStatus())
                .createdAt(instance.getCreatedAt())
                .expiresAt(instance.getExpiresAt())
                .remainingTimeSeconds((int) Duration.between(now, expiresAt).getSeconds())
                .build();
    }

    private String selectWorkerNodeForUser(CtfChallenge challenge) {
        String imageName = challenge.getImageName();
        // 查找状态就绪的资源节点
        List<ClusterNode> readyResourceNodes = clusterNodeRepository.findByIsResourceNodeTrue().stream()
                .filter(n -> "READY".equalsIgnoreCase(n.getStatus()))
                .toList();

        if (readyResourceNodes.isEmpty()) {
            readyResourceNodes = clusterNodeRepository.findAll().stream()
                    .filter(n -> "READY".equalsIgnoreCase(n.getStatus()))
                    .toList();
        }

        // 优先筛选 Worker 节点
        List<ClusterNode> workerNodes = readyResourceNodes.stream()
                .filter(n -> "WORKER".equalsIgnoreCase(n.getRole()))
                .toList();

        List<ClusterNode> candidatePool = !workerNodes.isEmpty() ? workerNodes : readyResourceNodes;
        if (candidatePool.isEmpty()) {
            return systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_PUBLIC_HOST, "127.0.0.1");
        }

        // 随机选择一个就绪候选节点（后续启动容器时若节点本地缺失镜像，将由集群基础设施自动拉取）
        int randomIndex = ThreadLocalRandom.current().nextInt(candidatePool.size());
        return candidatePool.get(randomIndex).getIpAddress();
    }

    @Override
    @Transactional
    public void stopInstance(Long challengeId, Long userId) {
        instanceRepository.findByChallengeIdAndTargetId(challengeId, userId)
                .ifPresent(inst -> {
                    if (inst.getContainerId() != null) {
                        stopContainerSafely(inst.getContainerId());
                    }
                    instanceRepository.delete(inst);
                });
    }

    @Override
    @Transactional
    public ChallengeInstanceResponseVO extendInstance(Long challengeId, Long userId) {
        CtfChallengeInstance inst = instanceRepository.findByChallengeIdAndTargetId(challengeId, userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "没有正在运行的靶机实例"));

        Instant now = Instant.now();
        if (inst.getExpiresAt().isBefore(now)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "容器已过期，请重新开启");
        }

        Instant newExpiresAt = inst.getExpiresAt().plus(Duration.ofMinutes(MAX_EXTEND_MINUTES));
        inst.setExpiresAt(newExpiresAt);
        instanceRepository.save(inst);

        return ChallengeInstanceResponseVO.builder()
                .challengeId(challengeId)
                .accessUrl(inst.getAccessUrl())
                .status(inst.getStatus())
                .createdAt(inst.getCreatedAt())
                .expiresAt(inst.getExpiresAt())
                .remainingTimeSeconds((int) Duration.between(now, newExpiresAt).getSeconds())
                .build();
    }

    @Override
    public Optional<CtfChallengeInstance> getInstance(Long challengeId, Long userId) {
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
        return getInstance(challengeId, userId)
                .map(CtfChallengeInstance::getAccessUrl)
                .orElse(null);
    }

    @Override
    public Integer getRemainingSeconds(Long challengeId, Long userId) {
        return getInstance(challengeId, userId)
                .map(inst -> {
                    if (inst.getExpiresAt() == null) return null;
                    long secs = Duration.between(Instant.now(), inst.getExpiresAt()).getSeconds();
                    return (int) Math.max(0, secs);
                })
                .orElse(null);
    }

    @Override
    public ChallengeInstanceResponseVO getTeamInstance(Long stageId, Long teamId, Long challengeId, Collection<Long> memberUserIds) {
        Instant now = Instant.now();
        Optional<CtfChallengeInstance> instOpt = Optional.empty();
        if (stageId != null && teamId != null) {
            instOpt = instanceRepository.findByStageIdAndTeamIdAndChallengeId(stageId, teamId, challengeId)
                    .filter(i -> i.getStatus() == InstanceStartupStatus.RUNNING && i.getExpiresAt().isAfter(now));
        }
        if (instOpt.isEmpty() && memberUserIds != null && !memberUserIds.isEmpty()) {
            instOpt = instanceRepository.findFirstByChallengeIdAndTargetIdInAndStatusAndExpiresAtAfter(
                    challengeId, memberUserIds, InstanceStartupStatus.RUNNING, now
            );
        }
        return instOpt.map(inst -> ChallengeInstanceResponseVO.builder()
                .challengeId(challengeId)
                .accessUrl(inst.getAccessUrl())
                .status(inst.getStatus())
                .createdAt(inst.getCreatedAt())
                .expiresAt(inst.getExpiresAt())
                .remainingTimeSeconds((int) Duration.between(now, inst.getExpiresAt()).getSeconds())
                .build()
        ).orElse(null);
    }

    @Override
    public long countActiveInstancesForTeam(Long stageId, Long teamId, Collection<Long> memberUserIds) {
        Instant now = Instant.now();
        if (memberUserIds == null || memberUserIds.isEmpty()) {
            if (stageId != null && teamId != null) {
                return instanceRepository.countByStageIdAndTeamIdAndStatusAndExpiresAtAfter(
                        stageId, teamId, InstanceStartupStatus.RUNNING, now
                );
            }
            return 0L;
        }
        return instanceRepository.countActiveByStageAndTeamOrTargetIds(
                stageId != null ? stageId : 0L,
                teamId != null ? teamId : 0L,
                memberUserIds,
                InstanceStartupStatus.RUNNING,
                now
        );
    }

    @Override
    @Transactional
    public void stopTeamInstance(Long stageId, Long teamId, Long challengeId, Collection<Long> memberUserIds) {
        Optional<CtfChallengeInstance> instOpt = Optional.empty();
        if (stageId != null && teamId != null) {
            instOpt = instanceRepository.findByStageIdAndTeamIdAndChallengeId(stageId, teamId, challengeId);
        }
        if (instOpt.isEmpty() && memberUserIds != null && !memberUserIds.isEmpty()) {
            instOpt = instanceRepository.findFirstByChallengeIdAndTargetIdIn(challengeId, memberUserIds);
        }
        instOpt.ifPresent(inst -> {
            stopContainerSafely(inst.getContainerId());
            instanceRepository.delete(inst);
        });
    }

    @Override
    @Transactional
    public ChallengeInstanceResponseVO extendTeamInstance(Long stageId, Long teamId, Long challengeId, Collection<Long> memberUserIds) {
        Optional<CtfChallengeInstance> instOpt = Optional.empty();
        if (stageId != null && teamId != null) {
            instOpt = instanceRepository.findByStageIdAndTeamIdAndChallengeId(stageId, teamId, challengeId)
                    .filter(i -> i.getExpiresAt().isAfter(Instant.now()));
        }
        if (instOpt.isEmpty() && memberUserIds != null && !memberUserIds.isEmpty()) {
            instOpt = instanceRepository.findFirstByChallengeIdAndTargetIdInAndStatusAndExpiresAtAfter(
                    challengeId, memberUserIds, InstanceStartupStatus.RUNNING, Instant.now()
            );
        }
        if (instOpt.isEmpty()) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "未检测到正在运行的靶机容器实例");
        }
        CtfChallengeInstance inst = instOpt.get();
        Instant now = Instant.now();
        Instant newExpiresAt = inst.getExpiresAt().plus(Duration.ofMinutes(15));
        inst.setExpiresAt(newExpiresAt);
        instanceRepository.save(inst);
        return ChallengeInstanceResponseVO.builder()
                .challengeId(challengeId)
                .accessUrl(inst.getAccessUrl())
                .status(inst.getStatus())
                .createdAt(inst.getCreatedAt())
                .expiresAt(inst.getExpiresAt())
                .remainingTimeSeconds((int) Duration.between(now, newExpiresAt).getSeconds())
                .build();
    }

    @Override
    public boolean verifyTeamFlag(Long stageId, Long teamId, Long challengeId, Collection<Long> memberUserIds, String flag) {
        if (!StringUtils.hasText(flag)) {
            return false;
        }
        Optional<CtfChallengeInstance> instOpt = Optional.empty();
        if (stageId != null && teamId != null) {
            instOpt = instanceRepository.findByStageIdAndTeamIdAndChallengeId(stageId, teamId, challengeId);
        }
        if (instOpt.isEmpty() && memberUserIds != null && !memberUserIds.isEmpty()) {
            instOpt = instanceRepository.findFirstByChallengeIdAndTargetIdIn(challengeId, memberUserIds);
        }
        return instOpt.map(inst -> inst.getFlag() != null && inst.getFlag().trim().equalsIgnoreCase(flag.trim()))
                .orElse(false);
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
                    stopContainerSafely(inst.getContainerId());
                }
                instanceRepository.delete(inst);
            } catch (Exception ignored) {}
        }
    }

}
