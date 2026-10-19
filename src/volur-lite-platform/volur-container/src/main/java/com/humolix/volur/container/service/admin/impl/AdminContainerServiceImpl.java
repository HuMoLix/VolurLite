package com.humolix.volur.container.service.admin.impl;

import com.humolix.volur.auth.security.util.SecurityUtils;
import com.humolix.volur.challenge.domain.entity.ctf.CtfChallenge;
import com.humolix.volur.challenge.repository.ctf.CtfChallengeRepository;
import com.humolix.volur.container.domain.dto.*;
import com.humolix.volur.container.domain.entity.ClusterNode;
import com.humolix.volur.container.domain.entity.CtfChallengeInstance;
import com.humolix.volur.container.domain.enums.InstanceStartupStatus;
import com.humolix.volur.container.domain.vo.*;
import com.humolix.volur.container.flag.DynamicFlagGenerator;
import com.humolix.volur.container.mapper.ClusterNodeMapper;
import com.humolix.volur.container.mapper.ContainerMapper;
import com.humolix.volur.container.repository.ClusterImageRepository;
import com.humolix.volur.container.repository.ClusterNodeRepository;
import com.humolix.volur.container.repository.CtfChallengeInstanceRepository;
import com.humolix.volur.container.service.admin.AdminContainerService;
import com.humolix.volur.container.service.admin.AdminImageService;
import com.humolix.volur.container.strategy.DockerSwarmRuntimeStrategy;
import com.humolix.volur.container.strategy.K3sRuntimeStrategy;
import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.system.constant.SystemSettingKeys;
import com.humolix.volur.system.service.SystemSettingService;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminContainerServiceImpl implements AdminContainerService {

    private final CtfChallengeInstanceRepository instanceRepository;
    private final CtfChallengeRepository challengeRepository;
    private final DynamicFlagGenerator dynamicFlagGenerator;
    private final UserRepository userRepository;
    private final ContainerMapper containerMapper;
    private final ClusterNodeMapper clusterNodeMapper;
    private final SystemSettingService systemSettingService;
    private final ClusterNodeRepository clusterNodeRepository;
    private final ClusterImageRepository clusterImageRepository;
    private final AdminImageService adminImageService;
    private final K3sRuntimeStrategy k3sRuntimeStrategy;
    private final DockerSwarmRuntimeStrategy dockerSwarmRuntimeStrategy;

    @Override
    @Transactional
    public PageResponse<AdminContainerInstanceVO> listInstances(AdminContainerPageRequestDTO request) {
        Specification<CtfChallengeInstance> spec = buildSpecification(request);
        Pageable pageable = request.toPageable(CtfChallengeInstance.class, "createdAt", Sort.Direction.DESC);
        Page<CtfChallengeInstance> page = instanceRepository.findAll(spec, pageable);

        if (page.isEmpty()) {
            return PageResponse.empty();
        }

        List<AdminContainerInstanceVO> voList = containerMapper.toAdminInstanceVOList(page.getContent());

        // Batch populate usernames
        Set<Long> userIds = page.getContent().stream()
                .map(CtfChallengeInstance::getTargetId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (!userIds.isEmpty()) {
            Map<Long, String> userMap = userRepository.findAllById(userIds).stream()
                    .collect(Collectors.toMap(User::getId, User::getUsername, (k1, k2) -> k1));
            voList.forEach(vo -> {
                if (vo.getUserId() != null) {
                    vo.setUsername(userMap.getOrDefault(vo.getUserId(), "UID:" + vo.getUserId()));
                }
            });
        }

        // Live status sync for active instances on current page
        for (CtfChallengeInstance inst : page.getContent()) {
            if (inst.getStatus() != InstanceStartupStatus.STOPPED) {
                try {
                    String containerType = StringUtils.hasText(inst.getContainerType())
                            ? inst.getContainerType()
                            : "DOCKER_SWARM";
                    InstanceStartupStatus liveStatus = "DOCKER_SWARM".equalsIgnoreCase(containerType)
                            ? dockerSwarmRuntimeStrategy.checkInstanceStatus(inst.getContainerId())
                            : k3sRuntimeStrategy.checkInstanceStatus(inst.getContainerId());
                    if (liveStatus != null && liveStatus != inst.getStatus()) {
                        inst.setStatus(liveStatus);
                        instanceRepository.save(inst);
                        voList.stream()
                                .filter(v -> Objects.equals(v.getId(), inst.getId()))
                                .findFirst()
                                .ifPresent(v -> v.setStatus(liveStatus));
                    }
                } catch (Exception e) {
                    log.warn("[AdminContainer] 同步实例 {} 运行时状态异常: {}", inst.getId(), e.getMessage());
                }
            }
        }

        // 节点感知与离线状态：核验各容器实例宿主节点当前存活状态与名称角色
        List<ClusterNode> allNodes = clusterNodeRepository.findAll();
        Map<String, ClusterNode> nodeIpMap = allNodes.stream()
                .filter(n -> StringUtils.hasText(n.getIpAddress()))
                .collect(Collectors.toMap(ClusterNode::getIpAddress, n -> n, (k1, k2) -> k1));
        Set<String> offlineNodeIps = allNodes.stream()
                .filter(n -> "OFFLINE".equalsIgnoreCase(n.getStatus()) || "ERROR".equalsIgnoreCase(n.getStatus()))
                .map(ClusterNode::getIpAddress)
                .collect(Collectors.toSet());
        String defaultHost = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_PUBLIC_HOST, "127.0.0.1");

        for (AdminContainerInstanceVO vo : voList) {
            String targetHost = defaultHost;
            if (StringUtils.hasText(vo.getNodeIp())) {
                targetHost = vo.getNodeIp().trim();
            } else if (StringUtils.hasText(vo.getAccessUrl())) {
                try {
                    URI uri = URI.create(vo.getAccessUrl());
                    if (StringUtils.hasText(uri.getHost())) {
                        targetHost = uri.getHost();
                    }
                } catch (Exception ignored) {}
            }
            vo.setNodeIp(targetHost);
            ClusterNode hostNode = nodeIpMap.get(targetHost);
            if (hostNode != null) {
                vo.setNodeName(hostNode.getNodeName());
                vo.setNodeRole(hostNode.getRole());
            } else if ("127.0.0.1".equals(targetHost) || defaultHost.equals(targetHost)) {
                vo.setNodeName("Master-01");
                vo.setNodeRole("MASTER");
            }
            boolean isOffline = offlineNodeIps.contains(targetHost);
            vo.setIsNodeOffline(isOffline);
        }

        return new PageResponse<>(
                voList,
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber() + 1
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AdminContainerStatisticsVO getStatistics() {
        long total = instanceRepository.count();
        long running = instanceRepository.countByStatus(InstanceStartupStatus.RUNNING);
        long starting = instanceRepository.countByStatusIn(List.of(
                InstanceStartupStatus.STARTING,
                InstanceStartupStatus.QUEUED,
                InstanceStartupStatus.DOWNLOADING_IMAGE
        ));
        long failed = instanceRepository.countByStatus(InstanceStartupStatus.FAILED);

        Instant todayStart = LocalDate.now(ZoneId.systemDefault())
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant();
        long todayCreated = instanceRepository.countByCreatedAtAfter(todayStart);

        String activeType = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_TYPE, "DOCKER_SWARM");

        return AdminContainerStatisticsVO.builder()
                .totalInstances(total)
                .runningCount(running)
                .startingCount(starting)
                .failedCount(failed)
                .todayCreatedCount(todayCreated)
                .activeContainerType(activeType)
                .build();
    }

    @Override
    @Transactional
    public void stopInstance(Long instanceId) {
        CtfChallengeInstance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "容器实例不存在或已被释放"));

        stopContainerSafely(instance.getContainerId());
        instanceRepository.delete(instance);
        log.info("[AdminContainer] 管理员强制销毁容器实例 id={}, containerId={}", instanceId, instance.getContainerId());
    }

    @Override
    @Transactional
    public void batchStop(AdminContainerBatchStopRequestDTO request) {
        if (request.getInstanceIds() == null || request.getInstanceIds().isEmpty()) {
            return;
        }

        List<CtfChallengeInstance> instances = instanceRepository.findAllById(request.getInstanceIds());
        for (CtfChallengeInstance inst : instances) {
            try {
                stopContainerSafely(inst.getContainerId());
                instanceRepository.delete(inst);
            } catch (Exception e) {
                log.warn("[AdminContainer] 批量释放容器实例 id={} 发生异常: {}", inst.getId(), e.getMessage());
            }
        }
        log.info("[AdminContainer] 管理员批量清理 {} 个容器实例完成", instances.size());
    }

    @Override
    @Transactional
    public void renewInstance(Long instanceId, AdminContainerRenewRequestDTO request) {
        CtfChallengeInstance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "容器实例不存在"));

        Instant now = Instant.now();
        Instant baseTime = (instance.getExpiresAt() != null && instance.getExpiresAt().isAfter(now))
                ? instance.getExpiresAt()
                : now;

        int duration = request != null && request.getDurationMinutes() > 0 ? request.getDurationMinutes() : 30;
        instance.setExpiresAt(baseTime.plus(Duration.ofMinutes(duration)));
        instanceRepository.save(instance);
        log.info("[AdminContainer] 管理员为容器实例 id={} 成功续期 {} 分钟", instanceId, duration);
    }

    @Override
    @Transactional
    public AdminContainerInstanceVO manualStartInstance(AdminContainerManualStartDTO request) {
        if (request == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "请求参数不能为空");
        }

        CtfChallenge challenge = null;
        if (request.getChallengeId() != null) {
            challenge = challengeRepository.findById(request.getChallengeId()).orElse(null);
        }

        String resolvedImageName = StringUtils.hasText(request.getImageName())
                ? request.getImageName().trim()
                : (challenge instanceof com.humolix.volur.challenge.domain.entity.Containerizable c ? c.getImageName() : null);

        if (!StringUtils.hasText(resolvedImageName)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "必须指定待运行的 Docker 镜像名称");
        }

        Long targetUserId = request.getUserId();
        if (targetUserId == null) {
            targetUserId = SecurityUtils.getCurrentUserId();
        }

        // 自定义 Flag 处理：若管理员未显式指定，则不生成动态 Flag
        String dynamicFlag = null;
        if (StringUtils.hasText(request.getCustomFlag())) {
            dynamicFlag = request.getCustomFlag().trim();
        }

        String containerName = StringUtils.hasText(request.getContainerName())
                ? request.getContainerName().trim()
                : null;

        String resourceName;
        if (StringUtils.hasText(containerName)) {
            resourceName = containerName;
        } else if (challenge != null) {
            resourceName = "volur-admin-u" + targetUserId + "-c" + challenge.getId() + "-" + System.currentTimeMillis();
        } else {
            resourceName = "volur-admin-u" + targetUserId + "-" + System.currentTimeMillis();
        }

        // 确定容器部署目标节点 (优先管理员指定，否则优先已有镜像的 Worker 节点，单节点则调度到 Master)
        String targetNodeIp = resolveTargetNodeIpForManualStart(request.getNodeIp(), resolvedImageName);
        request.setNodeIp(targetNodeIp);

        String containerType = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_TYPE, "DOCKER_SWARM");

        if ("DOCKER_SWARM".equalsIgnoreCase(containerType) && StringUtils.hasText(targetNodeIp)) {
            adminImageService.ensureImageOnNode(targetNodeIp, resolvedImageName);
        }

        ContainerRunResultDTO runResult = "DOCKER_SWARM".equalsIgnoreCase(containerType)
                ? dockerSwarmRuntimeStrategy.startManualContainer(request, resourceName, challenge, dynamicFlag)
                : k3sRuntimeStrategy.startManualContainer(request, resourceName, challenge, dynamicFlag);

        int duration = (request.getDurationMinutes() != null && request.getDurationMinutes() > 0)
                ? request.getDurationMinutes()
                : 30;

        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofMinutes(duration));

        String portsSummary = formatPortsSummary(request.getPorts(), runResult.getMappedPort());
        String volumesSummary = formatVolumesSummary(request.getVolumes());
        String envsSummary = formatEnvsSummary(request.getEnvironments());

        CtfChallengeInstance instance = CtfChallengeInstance.builder()
                .challenge(challenge)
                .imageName(resolvedImageName)
                .containerName(containerName)
                .ports(portsSummary)
                .volumes(volumesSummary)
                .envVars(envsSummary)
                .targetId(targetUserId)
                .stageId(0L)
                .containerId(runResult.getContainerId())
                .accessUrl(runResult.getAccessUrl())
                .flag(dynamicFlag != null ? dynamicFlag : "")
                .nodeIp(targetNodeIp)
                .status(runResult.getStatus())
                .containerType(containerType)
                .createdAt(now)
                .expiresAt(expiresAt)
                .build();

        instanceRepository.save(instance);

        AdminContainerInstanceVO vo = containerMapper.toAdminInstanceVO(instance);
        vo.setNodeIp(targetNodeIp);
        if (StringUtils.hasText(targetNodeIp)) {
            clusterNodeRepository.findByIpAddress(targetNodeIp).ifPresent(n -> {
                vo.setNodeName(n.getNodeName());
                vo.setNodeRole(n.getRole());
            });
        }
        userRepository.findById(targetUserId).ifPresent(u -> vo.setUsername(u.getUsername()));
        log.info("[AdminContainer] 管理员手动启动容器实例 id={}, containerId={}, targetNode={}, url={}",
                instance.getId(), instance.getContainerId(), targetNodeIp, instance.getAccessUrl());
        return vo;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminClusterNodeVO> getCandidateNodes() {
        return getCandidateResourceNodes().stream()
                .map(clusterNodeMapper::toVO)
                .toList();
    }

    private List<ClusterNode> getCandidateResourceNodes() {
        List<ClusterNode> readyResourceNodes = clusterNodeRepository.findByIsResourceNodeTrue().stream()
                .filter(n -> "READY".equalsIgnoreCase(n.getStatus()))
                .toList();

        if (readyResourceNodes.isEmpty()) {
            readyResourceNodes = clusterNodeRepository.findAll().stream()
                    .filter(n -> "READY".equalsIgnoreCase(n.getStatus()))
                    .toList();
        }

        List<ClusterNode> workerNodes = readyResourceNodes.stream()
                .filter(n -> "WORKER".equalsIgnoreCase(n.getRole()))
                .toList();

        if (!workerNodes.isEmpty()) {
            return workerNodes;
        }

        return readyResourceNodes;
    }

    private String resolveTargetNodeIpForManualStart(String requestedNodeIp, String imageName) {
        if (StringUtils.hasText(requestedNodeIp)) {
            return requestedNodeIp.trim();
        }

        List<ClusterNode> candidateNodes = getCandidateResourceNodes();
        if (candidateNodes.isEmpty()) {
            return systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_PUBLIC_HOST, "127.0.0.1");
        }

        return candidateNodes.get(0).getIpAddress();
    }

    private String formatPortsSummary(List<ContainerPortMappingDTO> ports, Integer defaultPort) {
        if (ports != null && !ports.isEmpty()) {
            return ports.stream()
                    .filter(p -> p.getContainerPort() != null && p.getContainerPort() > 0)
                    .map(p -> {
                        Object hp = (p.getHostPort() != null && p.getHostPort() > 0) ? p.getHostPort() : (defaultPort != null ? defaultPort : "*");
                        String proto = StringUtils.hasText(p.getProtocol()) ? p.getProtocol().trim().toUpperCase() : "TCP";
                        return hp + ":" + p.getContainerPort() + "/" + proto;
                    })
                    .collect(Collectors.joining(", "));
        }
        return defaultPort != null ? defaultPort + ":80/TCP" : null;
    }

    private String formatVolumesSummary(List<ContainerVolumeMappingDTO> volumes) {
        if (volumes != null && !volumes.isEmpty()) {
            return volumes.stream()
                    .filter(v -> StringUtils.hasText(v.getHostPath()) && StringUtils.hasText(v.getContainerPath()))
                    .map(v -> v.getHostPath().trim() + ":" + v.getContainerPath().trim() + ":" + (v.getMode() != null ? v.getMode() : "rw"))
                    .collect(Collectors.joining("; "));
        }
        return null;
    }

    private String formatEnvsSummary(List<ContainerEnvVarDTO> envs) {
        if (envs != null && !envs.isEmpty()) {
            return envs.stream()
                    .filter(e -> StringUtils.hasText(e.getName()))
                    .map(e -> e.getName().trim() + "=" + (e.getValue() != null ? e.getValue() : ""))
                    .collect(Collectors.joining(", "));
        }
        return null;
    }

    @Override
    public AdminContainerExecResultVO execContainer(Long instanceId, AdminContainerExecDTO request) {
        CtfChallengeInstance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "容器实例不存在或已被释放"));

        String containerId = instance.getContainerId();
        String fallbackFlag = instance.getFlag();

        String containerType = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_TYPE, "DOCKER_SWARM");
        if ("DOCKER_SWARM".equalsIgnoreCase(containerType) || (containerId != null && containerId.startsWith("swarm-"))) {
            return dockerSwarmRuntimeStrategy.execCommand(containerId, request, fallbackFlag);
        } else {
            return k3sRuntimeStrategy.execCommand(containerId, request, fallbackFlag);
        }
    }

    private Specification<CtfChallengeInstance> buildSpecification(AdminContainerPageRequestDTO request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (request.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), request.getStatus()));
            }

            if (request.getChallengeId() != null) {
                predicates.add(cb.equal(root.get("challenge").get("id"), request.getChallengeId()));
            }

            if (request.getUserId() != null) {
                predicates.add(cb.equal(root.get("targetId"), request.getUserId()));
            }

            if (request.getStageId() != null) {
                predicates.add(cb.equal(root.get("stageId"), request.getStageId()));
            }

            if (StringUtils.hasText(request.getKeyword())) {
                String kw = "%" + request.getKeyword().trim().toLowerCase() + "%";
                List<Predicate> keywordOr = new ArrayList<>();

                // 题目名称模糊匹配 (支持 challenge 为空的独立容器，必须使用 LEFT JOIN)
                var challengeJoin = root.join("challenge", jakarta.persistence.criteria.JoinType.LEFT);
                keywordOr.add(cb.like(cb.lower(challengeJoin.get("name")), kw));
                // 镜像名称模糊匹配
                keywordOr.add(cb.like(cb.lower(root.get("imageName")), kw));
                // 容器名称模糊匹配
                keywordOr.add(cb.like(cb.lower(root.get("containerName")), kw));
                // 容器 ID 模糊匹配
                keywordOr.add(cb.like(cb.lower(root.get("containerId")), kw));
                // Flag 模糊匹配
                keywordOr.add(cb.like(cb.lower(root.get("flag")), kw));

                // 尝试匹配用户 ID 或由用户名反查用户 ID
                String cleanKw = request.getKeyword().trim();
                List<User> matchedUsers = userRepository.findAll((userRoot, uq, ucb) ->
                        ucb.like(ucb.lower(userRoot.get("username")), "%" + cleanKw.toLowerCase() + "%")
                );
                if (!matchedUsers.isEmpty()) {
                    List<Long> matchedUserIds = matchedUsers.stream().map(User::getId).toList();
                    keywordOr.add(root.get("targetId").in(matchedUserIds));
                }

                // 若搜索词为纯数字，同时匹配用户 ID
                if (cleanKw.matches("\\d+")) {
                    try {
                        keywordOr.add(cb.equal(root.get("targetId"), Long.parseLong(cleanKw)));
                    } catch (NumberFormatException ignored) {}
                }

                predicates.add(cb.or(keywordOr.toArray(new Predicate[0])));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Override
    @Transactional(readOnly = true)
    public AdminContainerTerminalInfoVO getTerminalInfo(Long instanceId) {
        CtfChallengeInstance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "容器实例不存在或已被销毁"));

        String containerType = StringUtils.hasText(instance.getContainerType())
                ? instance.getContainerType()
                : systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_TYPE, "DOCKER_SWARM");

        return "DOCKER_SWARM".equalsIgnoreCase(containerType)
                ? dockerSwarmRuntimeStrategy.getTerminalInfo(instance.getContainerId(), instance.getFlag())
                : k3sRuntimeStrategy.getTerminalInfo(instance.getContainerId(), instance.getFlag());
    }

    private void stopContainerSafely(String containerId) {
        if (!StringUtils.hasText(containerId)) {
            return;
        }
        String containerType = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_TYPE, "DOCKER_SWARM");
        if ("DOCKER_SWARM".equalsIgnoreCase(containerType) || containerId.startsWith("swarm-")) {
            try {
                dockerSwarmRuntimeStrategy.stopContainer(containerId);
            } catch (Exception e) {
                log.warn("[AdminContainer] Docker Swarm 清理服务 {} 失败: {}", containerId, e.getMessage());
            }
        } else {
            try {
                k3sRuntimeStrategy.stopContainer(containerId);
            } catch (Exception e) {
                log.warn("[AdminContainer] K3s 清理资源 {} 失败: {}", containerId, e.getMessage());
            }
        }
    }

}
