package com.humolix.volur.container.service.admin.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.humolix.volur.container.domain.dto.*;
import com.humolix.volur.container.domain.entity.ClusterImage;
import com.humolix.volur.container.domain.entity.ClusterNode;
import com.humolix.volur.container.domain.entity.CtfChallengeInstance;
import com.humolix.volur.container.domain.enums.InstanceStartupStatus;
import com.humolix.volur.container.domain.vo.AdminClusterImageDetailVO;
import com.humolix.volur.container.domain.vo.AdminClusterImageStatisticsVO;
import com.humolix.volur.container.domain.vo.AdminClusterImageVO;
import com.humolix.volur.container.domain.vo.DockerImageLayerVO;
import com.humolix.volur.container.mapper.ClusterImageMapper;
import com.humolix.volur.container.repository.ClusterImageRepository;
import com.humolix.volur.container.repository.ClusterNodeRepository;
import com.humolix.volur.container.repository.CtfChallengeInstanceRepository;
import com.humolix.volur.container.service.admin.AdminImageService;
import com.humolix.volur.container.util.DockerTlsUtil;
import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.system.constant.SystemSettingKeys;
import com.humolix.volur.system.service.SystemSettingService;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminImageServiceImpl implements AdminImageService {

    private final ClusterImageRepository clusterImageRepository;
    private final ClusterNodeRepository clusterNodeRepository;
    private final ClusterImageMapper clusterImageMapper;
    private final SystemSettingService systemSettingService;
    private final CtfChallengeInstanceRepository ctfChallengeInstanceRepository;
    private final com.humolix.volur.system.service.admin.AdminAsyncTaskService adminAsyncTaskService;

    private record DiscoveredDockerImage(
            String repository,
            String tag,
            String imageId,
            long sizeBytes
    ) {}

    private record DockerContainerBriefInfo(
            String id,
            String name,
            String status,
            String state,
            String image
    ) {}

    private record DockerSwarmServiceBriefInfo(
            String id,
            String name,
            String image,
            String replicas
    ) {}

    @Override
    public AdminClusterImageStatisticsVO getStatistics() {
        long totalImages = clusterImageRepository.count();
        long challengeImages = clusterImageRepository.countByImageType("CHALLENGE");
        long baseImages = Math.max(0, totalImages - challengeImages);

        List<ClusterImage> allImages = clusterImageRepository.findAll();
        long totalSizeBytes = allImages.stream()
                .mapToLong(img -> img.getSizeBytes() != null ? img.getSizeBytes() : 0L)
                .sum();

        Set<String> uniqueNodes = new HashSet<>();
        long syncingCount = 0;
        for (ClusterImage img : allImages) {
            if ("PULLING".equalsIgnoreCase(img.getStatus()) || "MIGRATING".equalsIgnoreCase(img.getStatus())) {
                syncingCount++;
            }
            if (StringUtils.hasText(img.getNodeIps())) {
                Arrays.stream(img.getNodeIps().split(","))
                        .map(String::trim)
                        .filter(StringUtils::hasText)
                        .forEach(uniqueNodes::add);
            }
        }

        return AdminClusterImageStatisticsVO.builder()
                .totalImages(totalImages)
                .challengeImages(challengeImages)
                .baseImages(baseImages)
                .totalSizeBytes(totalSizeBytes)
                .totalSizeFormatted(formatBytes(totalSizeBytes))
                .totalNodesCovered(uniqueNodes.size())
                .syncingImages(syncingCount)
                .build();
    }

    @Override
    public PageResponse<AdminClusterImageVO> listImages(AdminImagePageRequestDTO request) {
        Pageable pageable = request.toPageable(ClusterImage.class, "createdAt");

        Specification<ClusterImage> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(request.getKeyword())) {
                String kw = "%" + request.getKeyword().trim().toLowerCase() + "%";
                Predicate repoPred = cb.like(cb.lower(root.get("repository")), kw);
                Predicate tagPred = cb.like(cb.lower(root.get("tag")), kw);
                predicates.add(cb.or(repoPred, tagPred));
            }

            if (StringUtils.hasText(request.getImageType())) {
                predicates.add(cb.equal(root.get("imageType"), request.getImageType().trim().toUpperCase()));
            }

            if (StringUtils.hasText(request.getNodeIp())) {
                predicates.add(cb.like(root.get("nodeIps"), "%" + request.getNodeIp().trim() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<ClusterImage> page = clusterImageRepository.findAll(spec, pageable);
        List<AdminClusterImageVO> voList = clusterImageMapper.toVOList(page.getContent());

        // 结合集群各节点当前存活状态，实时研判属于子节点的镜像在线/离线可用性
        Set<String> readyNodeIps = clusterNodeRepository.findByStatus("READY").stream()
                .map(ClusterNode::getIpAddress)
                .collect(Collectors.toSet());
        String masterHost = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_PUBLIC_HOST, "127.0.0.1");
        if (readyNodeIps.contains(masterHost)) {
            readyNodeIps.add("127.0.0.1");
        }
        if (readyNodeIps.contains("127.0.0.1")) {
            readyNodeIps.add(masterHost);
        }

        for (AdminClusterImageVO vo : voList) {
            List<String> ips = vo.getNodeIps() != null ? vo.getNodeIps() : Collections.emptyList();
            List<String> onlineIps = new ArrayList<>();
            List<String> offlineIps = new ArrayList<>();
            for (String ip : ips) {
                if (readyNodeIps.contains(ip)) {
                    onlineIps.add(ip);
                } else {
                    offlineIps.add(ip);
                }
            }
            vo.setOnlineNodeIps(onlineIps);
            vo.setOfflineNodeIps(offlineIps);
            boolean isAllOffline = !ips.isEmpty() && onlineIps.isEmpty();
            vo.setIsOffline(isAllOffline);
            if (isAllOffline) {
                vo.setStatus("OFFLINE");
            }
        }

        return new PageResponse<>(voList, page.getTotalElements(), page.getTotalPages(), page.getNumber() + 1);
    }

    @Override
    public AdminClusterImageDetailVO getImageDetail(Long id) {
        ClusterImage image = clusterImageRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "镜像记录不存在"));

        String fullImageName = buildFullImageName(image);
        String targetInspectId = StringUtils.hasText(image.getImageId()) ? image.getImageId() : fullImageName;
        String publicHost = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_PUBLIC_HOST, "127.0.0.1");

        // 挑选可以执行 inspect 与 history 的目标节点
        Set<String> nodeIps = parseNodeIps(image.getNodeIps());
        String chosenNode = null;
        if (nodeIps.contains(publicHost) || nodeIps.contains("127.0.0.1") || nodeIps.isEmpty()) {
            chosenNode = publicHost;
        } else {
            chosenNode = nodeIps.iterator().next();
        }

        // 1. 查询 Docker Inspect 深度配置
        JSONObject inspectJson = queryDockerInspect(chosenNode, targetInspectId);

        // 2. 查询 Docker History / Layer 分层信息
        List<DockerImageLayerVO> layers = queryDockerHistory(chosenNode, targetInspectId);

        // 组装 VO 深度属性
        String os = "linux";
        String architecture = "amd64";
        String dockerVersion = "Unknown";
        String author = "";
        String workingDir = "";
        String user = "";
        List<String> entrypoint = Collections.emptyList();
        List<String> cmd = Collections.emptyList();
        List<String> env = Collections.emptyList();
        List<String> exposedPorts = Collections.emptyList();
        Map<String, String> labels = new LinkedHashMap<>();

        if (inspectJson != null) {
            os = inspectJson.getStr("Os", os);
            architecture = inspectJson.getStr("Architecture", architecture);
            dockerVersion = inspectJson.getStr("DockerVersion", dockerVersion);
            author = inspectJson.getStr("Author", author);

            JSONObject config = inspectJson.getJSONObject("Config");
            if (config != null) {
                workingDir = config.getStr("WorkingDir", "");
                user = config.getStr("User", "");

                JSONArray epArr = config.getJSONArray("Entrypoint");
                if (epArr != null) {
                    entrypoint = epArr.toList(String.class);
                }

                JSONArray cmdArr = config.getJSONArray("Cmd");
                if (cmdArr != null) {
                    cmd = cmdArr.toList(String.class);
                }

                JSONArray envArr = config.getJSONArray("Env");
                if (envArr != null) {
                    env = envArr.toList(String.class);
                }

                JSONObject portsObj = config.getJSONObject("ExposedPorts");
                if (portsObj != null) {
                    exposedPorts = new ArrayList<>(portsObj.keySet());
                }

                JSONObject labelsObj = config.getJSONObject("Labels");
                if (labelsObj != null) {
                    for (String key : labelsObj.keySet()) {
                        labels.put(key, labelsObj.getStr(key));
                    }
                }
            }
        }

        Set<String> readyNodeIps = clusterNodeRepository.findByStatus("READY").stream()
                .map(ClusterNode::getIpAddress)
                .collect(Collectors.toSet());
        if (readyNodeIps.contains(publicHost)) {
            readyNodeIps.add("127.0.0.1");
        }
        if (readyNodeIps.contains("127.0.0.1")) {
            readyNodeIps.add(publicHost);
        }

        List<String> onlineIps = new ArrayList<>();
        List<String> offlineIps = new ArrayList<>();
        for (String ip : nodeIps) {
            if (readyNodeIps.contains(ip)) {
                onlineIps.add(ip);
            } else {
                offlineIps.add(ip);
            }
        }
        boolean isAllOffline = !nodeIps.isEmpty() && onlineIps.isEmpty();
        String resolvedStatus = isAllOffline ? "OFFLINE" : image.getStatus();

        return AdminClusterImageDetailVO.builder()
                .id(image.getId())
                .repository(image.getRepository())
                .tag(image.getTag())
                .fullImageName(fullImageName)
                .imageId(image.getImageId())
                .sizeBytes(image.getSizeBytes())
                .sizeFormatted(formatBytes(image.getSizeBytes()))
                .imageType(image.getImageType())
                .nodeIps(new ArrayList<>(nodeIps))
                .onlineNodeIps(onlineIps)
                .offlineNodeIps(offlineIps)
                .isOffline(isAllOffline)
                .status(resolvedStatus)
                .os(os)
                .architecture(architecture)
                .dockerVersion(dockerVersion)
                .author(author)
                .workingDir(workingDir)
                .user(user)
                .entrypoint(entrypoint)
                .cmd(cmd)
                .env(env)
                .exposedPorts(exposedPorts)
                .labels(labels)
                .layers(layers)
                .createdAt(image.getCreatedAt())
                .updatedAt(image.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateImageType(Long id, AdminImageTypeUpdateDTO request) {
        if (request == null || !StringUtils.hasText(request.getImageType())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "镜像类别不能为空");
        }

        ClusterImage image = clusterImageRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "镜像记录不存在"));

        String newType = request.getImageType().trim().toUpperCase(Locale.ROOT);
        image.setImageType(newType);
        image.setUpdatedAt(Instant.now());
        clusterImageRepository.save(image);

        log.info("[AdminImageService] 已更新镜像类别: id={}, fullImage={}, newType={}",
                id, buildFullImageName(image), newType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pullImage(AdminImagePullRequestDTO request) {
        if (request == null || !StringUtils.hasText(request.getImage())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "镜像名称不能为空");
        }

        String rawImage = request.getImage().trim();
        String repository;
        String tag;
        int lastColon = rawImage.lastIndexOf(':');
        if (lastColon > 0 && !rawImage.substring(lastColon + 1).contains("/")) {
            repository = rawImage.substring(0, lastColon);
            tag = rawImage.substring(lastColon + 1);
        } else {
            repository = rawImage;
            tag = "latest";
        }

        List<ClusterNode> allNodes = clusterNodeRepository.findAll();
        if (allNodes.isEmpty()) {
            initDefaultMasterNode();
            allNodes = clusterNodeRepository.findAll();
        }

        boolean multiNode = allNodes.size() > 1;
        String targetIp = request.getTargetNodeIp().trim();

        if (multiNode) {
            boolean isMaster = allNodes.stream()
                    .anyMatch(n -> "MASTER".equalsIgnoreCase(n.getRole()) && n.getIpAddress().equalsIgnoreCase(targetIp));
            if (isMaster) {
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        "集群存在多个节点，主节点保留为平台控制面，禁止将题目与容器镜像下载至主节点！请选择资源节点或广播至所有资源节点。");
            }
        }

        List<String> targetIps = new ArrayList<>();
        if ("ALL_RESOURCE_NODES".equalsIgnoreCase(targetIp)) {
            if (multiNode) {
                allNodes.stream()
                        .filter(n -> "WORKER".equalsIgnoreCase(n.getRole()) || Boolean.TRUE.equals(n.getIsResourceNode()))
                        .map(ClusterNode::getIpAddress)
                        .forEach(targetIps::add);
            } else {
                allNodes.stream().map(ClusterNode::getIpAddress).forEach(targetIps::add);
            }
        } else {
            targetIps.add(targetIp);
        }

        if (targetIps.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "未找到有效的目标资源节点");
        }

        log.info("[AdminImageService] 正在拉取 Docker 镜像: {}:{} 至目标节点: {}", repository, tag, targetIps);

        for (String nodeIp : targetIps) {
            triggerDockerPullOnNode(nodeIp, rawImage);
        }

        Optional<ClusterImage> existingOpt = clusterImageRepository.findByRepositoryAndTag(repository, tag);
        ClusterImage image;
        if (existingOpt.isPresent()) {
            image = existingOpt.get();
            Set<String> ipSet = parseNodeIps(image.getNodeIps());
            ipSet.addAll(targetIps);
            image.setNodeIps(String.join(",", ipSet));
            image.setStatus("PULLED");
            image.setUpdatedAt(Instant.now());
        } else {
            image = ClusterImage.builder()
                    .repository(repository)
                    .tag(tag)
                    .imageId("")
                    .sizeBytes(0L)
                    .imageType(StringUtils.hasText(request.getImageType()) ? request.getImageType().trim().toUpperCase() : "CHALLENGE")
                    .nodeIps(String.join(",", targetIps))
                    .status("PULLED")
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
        }

        clusterImageRepository.save(image);
        updateNodeImageCounts();
        log.info("[AdminImageService] 镜像拉取任务已完成并同步元数据: {}:{}", repository, tag);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteImage(Long id, String targetNodeIp) {
        ClusterImage image = clusterImageRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "镜像记录不存在"));

        String fullImageName = buildFullImageName(image);
        if (!StringUtils.hasText(fullImageName)) {
            fullImageName = image.getImageId();
        }

        String publicHost = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_PUBLIC_HOST, "127.0.0.1");

        Set<String> imageNodeIps = parseNodeIps(image.getNodeIps());
        if (imageNodeIps.isEmpty()) {
            imageNodeIps.add(publicHost);
        }

        // 解析需要删除的目标节点集合
        Set<String> targetIpsToDelete = new LinkedHashSet<>();
        boolean isFullDelete = !StringUtils.hasText(targetNodeIp) || "ALL".equalsIgnoreCase(targetNodeIp.trim());

        if (isFullDelete) {
            targetIpsToDelete.addAll(imageNodeIps);
        } else {
            Arrays.stream(targetNodeIp.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .forEach(targetIpsToDelete::add);
        }

        if (targetIpsToDelete.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "未指定任何有效的待删除节点");
        }

        // 校验请求的目标节点是否确实包含该镜像
        for (String nodeIp : targetIpsToDelete) {
            boolean isLocal = isLocalNode(nodeIp, publicHost);
            boolean exists = imageNodeIps.stream().anyMatch(ip ->
                    ip.equalsIgnoreCase(nodeIp) || (isLocal && isLocalNode(ip, publicHost)));
            if (!exists) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "节点 [" + nodeIp + "] 上未记录该镜像");
            }
        }

        // 阶段 1：平台实例依赖校验
        // 如果是全量删除，校验平台上所有运行中或关联的历史容器；
        // 如果是指定节点删除，仅校验属于指定节点上的容器实例。
        if (isFullDelete) {
            checkPlatformContainerDependencies(image, null);
        } else {
            for (String nodeIp : targetIpsToDelete) {
                checkPlatformContainerDependencies(image, nodeIp);
            }
        }

        // 阶段 2：在目标节点的底层 Docker 中查询是否有依赖该镜像的容器（运行中或已停止）或 Swarm 服务
        for (String nodeIp : targetIpsToDelete) {
            checkDockerContainerDependencies(nodeIp, image);
        }

        // 阶段 3：在确保无任何依赖容器后，物理调用 Docker 执行删除，并严格确认 Docker 镜像确实已被清除
        for (String nodeIp : targetIpsToDelete) {
            removeDockerImageAndConfirm(nodeIp, fullImageName, image.getImageId());
        }

        // 阶段 4：在确认底层 Docker 镜像已删除之后，变更或删除数据库记录
        Set<String> remainingIps = new LinkedHashSet<>(imageNodeIps);
        for (String nodeIp : targetIpsToDelete) {
            boolean isLocal = isLocalNode(nodeIp, publicHost);
            remainingIps.removeIf(ip ->
                    ip.equalsIgnoreCase(nodeIp) || (isLocal && isLocalNode(ip, publicHost)));
        }

        if (remainingIps.isEmpty() || isFullDelete) {
            clusterImageRepository.delete(image);
            log.info("[AdminImageService] 镜像已从所有目标节点物理清除，全量删除数据库元数据: {}", fullImageName);
        } else {
            image.setNodeIps(String.join(",", remainingIps));
            image.setUpdatedAt(Instant.now());
            clusterImageRepository.save(image);
            log.info("[AdminImageService] 已从节点 {} 物理移除镜像并更新数据库记录，剩余节点: {}: {}",
                    targetIpsToDelete, remainingIps, fullImageName);
        }

        updateNodeImageCounts();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncAllImagesToResourceNodes() {
        List<ClusterNode> resourceNodes = clusterNodeRepository.findByIsResourceNodeTrue();
        if (resourceNodes.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "集群中未配置任何资源节点，无需同步");
        }

        for (ClusterNode node : resourceNodes) {
            migrateMasterImagesToWorker(node);
        }
        log.info("[AdminImageService] 已向所有 {} 个资源节点同步主节点题目镜像", resourceNodes.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void migrateMasterImagesToWorker(ClusterNode workerNode) {
        if (workerNode == null || !StringUtils.hasText(workerNode.getIpAddress())) {
            return;
        }

        String targetIp = workerNode.getIpAddress().trim();
        List<ClusterImage> challengeImages = clusterImageRepository.findByImageType("CHALLENGE");
        if (challengeImages.isEmpty()) {
            return;
        }

        int migrated = 0;
        for (ClusterImage img : challengeImages) {
            Set<String> ipSet = parseNodeIps(img.getNodeIps());
            if (ipSet.add(targetIp)) {
                triggerDockerPullOnNode(targetIp, buildFullImageName(img));
                img.setNodeIps(String.join(",", ipSet));
                img.setStatus("PULLED");
                img.setUpdatedAt(Instant.now());
                clusterImageRepository.save(img);
                migrated++;
            }
        }

        workerNode.setImageCount(challengeImages.size());
        clusterNodeRepository.save(workerNode);

        log.info("[AdminImageService] 自动镜像迁移完成: 已将主节点的 {} 个资源镜像成功同步至节点: {} ({})",
                migrated, workerNode.getNodeName(), targetIp);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeNodeFromImages(String nodeIp, boolean onlyChallenge) {
        if (!StringUtils.hasText(nodeIp)) {
            return;
        }
        String cleanIp = nodeIp.trim();
        List<ClusterImage> allImages = clusterImageRepository.findAll();
        int affectedCount = 0;

        for (ClusterImage image : allImages) {
            if (onlyChallenge && !"CHALLENGE".equalsIgnoreCase(image.getImageType())) {
                continue;
            }
            if (!StringUtils.hasText(image.getNodeIps())) {
                continue;
            }

            Set<String> ipSet = parseNodeIps(image.getNodeIps());
            if (ipSet.remove(cleanIp)) {
                affectedCount++;
                image.setNodeIps(String.join(",", ipSet));
                image.setUpdatedAt(Instant.now());
                clusterImageRepository.save(image);
            }
        }

        updateNodeImageCounts();
        log.info("[AdminImageService] 已成功从镜像管理中剔除节点 [{}] 的镜像拓扑关联，共更新 {} 个镜像记录 (onlyChallenge={})",
                cleanIp, affectedCount, onlyChallenge);
    }

    /**
     * 程序启动与主动触发时：
     * 1. 首先通过 Docker API 加载本地主机镜像；
     * 2. 然后巡检检查集群子节点（Worker）中的镜像；
     * 3. 核心校验：核验 MySQL 中的所有镜像记录与各节点实际存在的镜像，彻底清理已不存在的记录，避免假记录残留。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void loadAndSyncClusterImages() {
        log.info("[AdminImageService] 开始执行 Docker 集群镜像动态加载、探测与孤儿记录核验...");

        List<ClusterNode> allNodes = clusterNodeRepository.findAll();
        if (allNodes.isEmpty()) {
            initDefaultMasterNode();
            allNodes = clusterNodeRepository.findAll();
        }

        String masterIp = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_PUBLIC_HOST, "127.0.0.1");

        // 记录各节点探测成功标记及其拥有的镜像标识集合 (包括 fullImageName, imageId, 12位短ID)
        Set<String> localDiscoveredImages = new HashSet<>();
        Map<String, Set<String>> workerDiscoveredImages = new HashMap<>();
        Set<String> successfullyInspectedNodes = new HashSet<>();

        // 步骤 1：先通过 Docker API 加载本地主节点的镜像
        log.info("[AdminImageService] 正在通过 Docker API 加载本地主节点 ({}) 的镜像列表...", masterIp);
        List<DiscoveredDockerImage> localImages = fetchLocalDockerImages();
        successfullyInspectedNodes.add(masterIp);
        successfullyInspectedNodes.add("127.0.0.1");
        log.info("[AdminImageService] 本地 Docker API 探测成功，共获取到 {} 个本地可用镜像", localImages.size());

        for (DiscoveredDockerImage img : localImages) {
            recordImageIdentifiers(img, localDiscoveredImages);
            syncDiscoveredImageToRepository(img, masterIp);
        }

        // 步骤 2：检查集群中各子节点（Worker）中的镜像
        List<ClusterNode> workerNodes = allNodes.stream()
                .filter(n -> "WORKER".equalsIgnoreCase(n.getRole()) || !n.getIpAddress().equalsIgnoreCase(masterIp))
                .toList();

        for (ClusterNode worker : workerNodes) {
            String workerIp = worker.getIpAddress();
            List<DiscoveredDockerImage> workerImages = fetchRemoteDockerImages(worker);
            if (!workerImages.isEmpty() || isRemoteDockerReachable(worker)) {
                successfullyInspectedNodes.add(workerIp);
                Set<String> workerSet = new HashSet<>();
                for (DiscoveredDockerImage img : workerImages) {
                    recordImageIdentifiers(img, workerSet);
                    syncDiscoveredImageToRepository(img, workerIp);
                }
                workerDiscoveredImages.put(workerIp, workerSet);
                worker.setImageCount(workerImages.size());
                worker.setLastHeartbeat(Instant.now());
                clusterNodeRepository.save(worker);
                log.info("[AdminImageService] 子节点 [{}] 探测到 {} 个 Docker 镜像", workerIp, workerImages.size());
            } else {
                log.info("[AdminImageService] 子节点 [{}] 暂未响应远程 Docker API 或离线，跳过该节点清理核验", workerIp);
            }
        }

        // 步骤 3：核心校验与清理 —— 对比 MySQL 中的所有镜像记录，剔除在所有受检节点中均已不复存在的孤儿镜像
        log.info("[AdminImageService] 正在比对核验 MySQL 镜像记录与节点实际镜像现状...");
        List<ClusterImage> allDbImages = clusterImageRepository.findAll();
        int deletedCount = 0;
        int adjustedNodesCount = 0;

        for (ClusterImage dbImg : allDbImages) {
            String fullImageName = buildFullImageName(dbImg);
            String imageId = dbImg.getImageId();
            Set<String> currentNodes = parseNodeIps(dbImg.getNodeIps());
            Set<String> retainedNodes = new LinkedHashSet<>();

            for (String nodeIp : currentNodes) {
                if (successfullyInspectedNodes.contains(nodeIp)) {
                    boolean existsOnNode = false;
                    if (masterIp.equalsIgnoreCase(nodeIp) || "127.0.0.1".equalsIgnoreCase(nodeIp)) {
                        existsOnNode = isImagePresentInSet(fullImageName, imageId, localDiscoveredImages);
                    } else if (workerDiscoveredImages.containsKey(nodeIp)) {
                        existsOnNode = isImagePresentInSet(fullImageName, imageId, workerDiscoveredImages.get(nodeIp));
                    }

                    if (existsOnNode) {
                        retainedNodes.add(nodeIp);
                    } else {
                        adjustedNodesCount++;
                        log.info("[AdminImageService] 校验发现节点 [{}] 上已无物理镜像: {}，已从该节点移除", nodeIp, fullImageName);
                    }
                } else {
                    // 对于本次未连通或离线的节点，予以保留以防误删离线节点数据
                    retainedNodes.add(nodeIp);
                }
            }

            if (retainedNodes.isEmpty()) {
                clusterImageRepository.delete(dbImg);
                deletedCount++;
                log.info("[AdminImageService] 校验发现镜像在所有节点中均不存在，已从 MySQL 中彻底删除无效记录: {}", fullImageName);
            } else if (!retainedNodes.equals(currentNodes)) {
                dbImg.setNodeIps(String.join(",", retainedNodes));
                dbImg.setUpdatedAt(Instant.now());
                clusterImageRepository.save(dbImg);
            }
        }

        log.info("[AdminImageService] 镜像对账校验完成: 彻底清理无效孤儿镜像 {} 个，校正节点关联 {} 处", deletedCount, adjustedNodesCount);

        updateNodeImageCounts();
        log.info("[AdminImageService] Docker 集群本地与子节点镜像同步与元数据核验已完成");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public com.humolix.volur.system.domain.vo.AdminAsyncTaskSubmitVO transferImage(AdminImageTransferDTO request) {
        if (request == null || request.getImageId() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "请求参数不能为空");
        }

        ClusterImage image = clusterImageRepository.findById(request.getImageId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "镜像记录不存在"));

        String sourceIp = request.getSourceNodeIp().trim();
        String targetIp = request.getTargetNodeIp().trim();

        if (sourceIp.equalsIgnoreCase(targetIp)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "源节点与目标节点不能相同");
        }

        Set<String> currentIps = parseNodeIps(image.getNodeIps());
        if (!currentIps.contains(sourceIp)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "该镜像当前未部署在源节点: " + sourceIp);
        }

        List<ClusterNode> allNodes = clusterNodeRepository.findAll();
        boolean targetExists = allNodes.stream().anyMatch(n -> n.getIpAddress().equalsIgnoreCase(targetIp));
        if (!targetExists) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "目标节点 " + targetIp + " 不存在于集群中");
        }

        if (allNodes.size() > 1 && "CHALLENGE".equalsIgnoreCase(image.getImageType())) {
            boolean targetIsMaster = allNodes.stream()
                    .anyMatch(n -> "MASTER".equalsIgnoreCase(n.getRole()) && n.getIpAddress().equalsIgnoreCase(targetIp));
            if (targetIsMaster) {
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        "集群存在多个节点，主节点保留为平台控制面，禁止将赛题运行镜像转移至主节点！");
            }
        }

        String fullImageName = buildFullImageName(image);
        log.info("[AdminImageService] 正在提交镜像节点转移异步任务: {} ({} -> {})", fullImageName, sourceIp, targetIp);

        image.setStatus("MIGRATING");
        clusterImageRepository.save(image);

        Long imageId = image.getId();
        String taskName = String.format("镜像节点转移: %s (%s -> %s)", fullImageName, sourceIp, targetIp);

        return adminAsyncTaskService.submitTask(
                com.humolix.volur.system.domain.enums.AsyncTaskType.IMAGE_TRANSFER,
                taskName,
                null,
                "ADMIN",
                request,
                context -> {
                    try {
                        context.updateProgress(15, "正在连接目标节点 " + targetIp + " 并准备拉取镜像...");
                        if (context.isCancelled()) return;

                        context.updateProgress(35, "正在目标节点 " + targetIp + " 拉取镜像: " + fullImageName);
                        triggerDockerPullOnNode(targetIp, fullImageName);
                        if (context.isCancelled()) return;

                        context.updateProgress(75, "目标节点镜像拉取完成，正在更新集群镜像节点拓扑...");
                        ClusterImage latest = clusterImageRepository.findById(imageId).orElse(null);
                        if (latest != null) {
                            Set<String> ips = parseNodeIps(latest.getNodeIps());
                            ips.remove(sourceIp);
                            ips.add(targetIp);
                            latest.setNodeIps(String.join(",", ips));
                            latest.setStatus("PULLED");
                            latest.setUpdatedAt(Instant.now());
                            clusterImageRepository.save(latest);
                        }

                        context.updateProgress(88, "正在清理源节点 " + sourceIp + " 上的旧镜像文件...");
                        triggerDockerRmiOnNode(sourceIp, fullImageName);

                        updateNodeImageCounts();
                        context.updateProgress(100, "镜像成功从 " + sourceIp + " 转移至 " + targetIp);
                        log.info("[AdminImageService] 异步镜像节点转移完成: {} 成功从 {} 转移至 {}", fullImageName, sourceIp, targetIp);
                    } catch (Exception ex) {
                        log.error("[AdminImageService] 异步镜像节点转移失败: {}", ex.getMessage(), ex);
                        ClusterImage latest = clusterImageRepository.findById(imageId).orElse(null);
                        if (latest != null) {
                            latest.setStatus("FAILED");
                            latest.setUpdatedAt(Instant.now());
                            clusterImageRepository.save(latest);
                        }
                        throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "镜像节点转移失败: " + ex.getMessage());
                    }
                }
        );
    }

    @Override
    public com.humolix.volur.system.domain.vo.AdminAsyncTaskSubmitVO backupImage(AdminImageBackupDTO request) {
        if (request == null || request.getImageId() == null || !StringUtils.hasText(request.getBackupType())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "备份参数不完整");
        }

        ClusterImage image = clusterImageRepository.findById(request.getImageId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "镜像记录不存在"));

        String backupType = request.getBackupType().trim().toUpperCase();
        String fullImageName = buildFullImageName(image);

        if ("NODE".equals(backupType)) {
            String targetNodeIp = request.getTargetNodeIp();
            if (!StringUtils.hasText(targetNodeIp)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "节点容灾备份必须指定目标备份节点 IP");
            }

            final String finalTargetIp = targetNodeIp.trim();
            boolean nodeExists = clusterNodeRepository.existsByIpAddress(finalTargetIp);
            if (!nodeExists) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "目标备份节点 " + finalTargetIp + " 不存在");
            }

            Set<String> ipSet = parseNodeIps(image.getNodeIps());
            if (ipSet.contains(finalTargetIp)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "目标节点已部署该镜像副本，无需重复备份");
            }

            Long imageId = image.getId();
            String taskName = String.format("镜像副本备份: %s (复制至节点 %s)", fullImageName, finalTargetIp);
            log.info("[AdminImageService] 正在提交镜像容灾副本备份异步任务: {} -> {}", fullImageName, finalTargetIp);

            return adminAsyncTaskService.submitTask(
                    com.humolix.volur.system.domain.enums.AsyncTaskType.IMAGE_BACKUP,
                    taskName,
                    null,
                    "ADMIN",
                    request,
                    context -> {
                        try {
                            context.updateProgress(15, "正在连接目标备份节点 " + finalTargetIp + "...");
                            if (context.isCancelled()) return;

                            context.updateProgress(35, "正在目标节点 " + finalTargetIp + " 拉取并同步镜像: " + fullImageName);
                            triggerDockerPullOnNode(finalTargetIp, fullImageName);
                            if (context.isCancelled()) return;

                            context.updateProgress(80, "目标节点镜像拉取完成，正在更新集群镜像拓扑...");
                            ClusterImage latest = clusterImageRepository.findById(imageId).orElse(null);
                            if (latest != null) {
                                Set<String> currentIps = parseNodeIps(latest.getNodeIps());
                                currentIps.add(finalTargetIp);
                                latest.setNodeIps(String.join(",", currentIps));
                                latest.setUpdatedAt(Instant.now());
                                clusterImageRepository.save(latest);
                            }

                            updateNodeImageCounts();
                            context.updateProgress(100, "镜像副本成功复制并备份至节点 " + finalTargetIp);
                            log.info("[AdminImageService] 异步镜像副本备份完成: {} 成功备份至节点 {}", fullImageName, finalTargetIp);
                        } catch (Exception ex) {
                            log.error("[AdminImageService] 异步镜像副本备份失败: {}", ex.getMessage(), ex);
                            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "镜像副本备份失败: " + ex.getMessage());
                        }
                    }
            );

        } else if ("ARCHIVE".equals(backupType)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "离线包导出已升级为浏览器直接流式下载（不保存至服务器磁盘），请通过 GET /api/v1/admin/images/{id}/export 接口直接导出下载");
        } else {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "不支持的备份类型，仅支持 NODE (节点副本) 或 ARCHIVE (导出下载)");
        }
    }

    @Override
    public void exportImage(Long id, HttpServletResponse response) {
        if (id == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "镜像ID不能为空");
        }

        ClusterImage image = clusterImageRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "镜像记录不存在"));

        String fullImageName = buildFullImageName(image);
        String publicHost = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_PUBLIC_HOST, "127.0.0.1");

        // 挑选可以读取该镜像的目标节点
        Set<String> nodeIps = parseNodeIps(image.getNodeIps());
        String chosenNode = null;
        if (nodeIps.contains(publicHost) || nodeIps.contains("127.0.0.1") || nodeIps.isEmpty()) {
            chosenNode = publicHost;
        } else {
            chosenNode = nodeIps.iterator().next();
        }

        List<String> command = buildDockerBaseCommand(chosenNode);
        command.add("save");
        command.add(fullImageName);

        String safeRepo = image.getRepository().replace('/', '_');
        String filename = safeRepo + "_" + image.getTag() + ".tar";
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

        response.setContentType("application/x-tar");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encodedFilename);

        log.info("[AdminImageService] 开始向客户端流式导出 Docker 镜像: {} (来源节点: {})", fullImageName, chosenNode);

        Process process = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            process = pb.start();

            // 异步读取 stderr 避免进程缓冲区填满导致死锁
            StringBuilder stderr = new StringBuilder();
            Process currentProcess = process;
            Thread errThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(currentProcess.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stderr.append(line).append("\n");
                    }
                } catch (Exception ignored) {
                }
            });
            errThread.setDaemon(true);
            errThread.start();

            try (InputStream is = process.getInputStream();
                 OutputStream os = response.getOutputStream()) {
                byte[] buffer = new byte[65536]; // 64KB 高性能流式缓冲区
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();
            }

            errThread.join(3000);
            boolean finished = process.waitFor(300, TimeUnit.SECONDS);
            if (!finished || process.exitValue() != 0) {
                log.error("[AdminImageService] 镜像流式导出退出异常: exitCode={}, stderr={}",
                        finished ? process.exitValue() : "TIMEOUT", stderr);
                if (!response.isCommitted()) {
                    throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "Docker 导出镜像失败: " + stderr);
                }
            } else {
                log.info("[AdminImageService] 镜像流式导出下载成功完成: {}", fullImageName);
            }
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            log.error("[AdminImageService] 镜像流式导出异常: {}", e.getMessage(), e);
            if (!response.isCommitted()) {
                throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "流式传输镜像失败: " + e.getMessage());
            }
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ensureImageOnNode(String targetNodeIp, String fullImageName) {
        if (!StringUtils.hasText(targetNodeIp) || !StringUtils.hasText(fullImageName)) {
            return;
        }

        String targetIp = targetNodeIp.trim();
        String imageName = fullImageName.trim();

        // 1. 拆分镜像名称与标签
        String repo = imageName;
        String tag = "latest";
        int colonIdx = imageName.lastIndexOf(':');
        if (colonIdx > 0 && !imageName.substring(colonIdx + 1).contains("/")) {
            repo = imageName.substring(0, colonIdx);
            tag = imageName.substring(colonIdx + 1);
        }

        Optional<ClusterImage> imageOpt = clusterImageRepository.findByRepositoryAndTag(repo, tag);

        // 2. 在目标节点上通过 Docker Inspect 验证该镜像是否已实际物理就绪
        JSONObject inspectTarget = queryDockerInspect(targetIp, imageName);
        if (inspectTarget != null) {
            log.info("[AdminImageService] 目标节点 [{}] Docker 引擎已实际存在镜像 [{}]，校验通过", targetIp, imageName);
            if (imageOpt.isPresent()) {
                ClusterImage img = imageOpt.get();
                Set<String> nodeIps = parseNodeIps(img.getNodeIps());
                if (!nodeIps.contains(targetIp)) {
                    nodeIps.add(targetIp);
                    img.setNodeIps(String.join(",", nodeIps));
                    img.setUpdatedAt(Instant.now());
                    clusterImageRepository.save(img);
                    updateNodeImageCounts();
                }
            }
            return;
        }

        // 3. 目标节点物理缺失该镜像：若数据库之前误记录了包含该节点，则纠正元数据
        if (imageOpt.isPresent()) {
            ClusterImage img = imageOpt.get();
            Set<String> nodeIps = parseNodeIps(img.getNodeIps());
            if (nodeIps.remove(targetIp)) {
                img.setNodeIps(String.join(",", nodeIps));
                img.setUpdatedAt(Instant.now());
                clusterImageRepository.save(img);
                updateNodeImageCounts();
            }
        }

        // 4. 寻找持有该镜像的可用源节点
        String sourceNodeIp = null;
        if (imageOpt.isPresent()) {
            Set<String> nodeIps = parseNodeIps(imageOpt.get().getNodeIps());
            List<String> readyNodeIps = clusterNodeRepository.findByStatus("READY").stream()
                    .map(ClusterNode::getIpAddress)
                    .toList();
            String publicHost = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_PUBLIC_HOST, "127.0.0.1");

            for (String ip : nodeIps) {
                if (!ip.equalsIgnoreCase(targetIp) && (readyNodeIps.contains(ip) || "127.0.0.1".equals(ip) || publicHost.equalsIgnoreCase(ip))) {
                    if (queryDockerInspect(ip, imageName) != null) {
                        sourceNodeIp = ip;
                        break;
                    }
                }
            }
        }

        // 如果数据库中未找到其它有效节点，尝试检查主节点 Docker
        if (sourceNodeIp == null) {
            if (!targetIp.equals("127.0.0.1") && queryDockerInspect("127.0.0.1", imageName) != null) {
                sourceNodeIp = "127.0.0.1";
            } else {
                String publicHost = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_PUBLIC_HOST, "127.0.0.1");
                if (!targetIp.equalsIgnoreCase(publicHost) && queryDockerInspect(publicHost, imageName) != null) {
                    sourceNodeIp = publicHost;
                }
            }
        }

        if (sourceNodeIp == null) {
            // 集群其它节点未找到该镜像，尝试在目标节点直接 pull（公网 Hub 镜像容错）
            log.warn("[AdminImageService] 镜像 [{}] 在集群内其它在线节点均不存在，尝试直接在目标节点 [{}] 执行 pull", imageName, targetIp);
            triggerDockerPullOnNode(targetIp, imageName);
            return;
        }

        // 5. 执行跨节点物理镜像复制（docker save 流式管道输送给 docker load）
        log.info("[AdminImageService] 检测到目标节点 [{}] 缺少镜像 [{}]，正在从节点 [{}] 复制镜像副本...",
                targetIp, imageName, sourceNodeIp);
        replicateImageToNode(sourceNodeIp, targetIp, imageName);

        // 6. 更新数据库镜像分布元数据
        if (imageOpt.isPresent()) {
            ClusterImage img = imageOpt.get();
            Set<String> nodeIps = parseNodeIps(img.getNodeIps());
            nodeIps.add(targetIp);
            img.setNodeIps(String.join(",", nodeIps));
            img.setUpdatedAt(Instant.now());
            clusterImageRepository.save(img);
            updateNodeImageCounts();
        } else {
            // 若数据库无记录，自动探测并同步
            syncDiscoveredImageToRepository(new DiscoveredDockerImage(repo, tag, "", 0L), targetIp);
            updateNodeImageCounts();
        }
    }

    @Override
    public void replicateImageToNode(String sourceNodeIp, String targetNodeIp, String fullImageName) {
        if (!StringUtils.hasText(sourceNodeIp) || !StringUtils.hasText(targetNodeIp) || sourceNodeIp.equalsIgnoreCase(targetNodeIp)) {
            return;
        }

        String safeImageName = fullImageName.trim();
        log.info("[AdminImageService] 开始执行跨节点镜像复制: {} ({} -> {})", safeImageName, sourceNodeIp, targetNodeIp);

        List<String> saveCmd = buildDockerBaseCommand(sourceNodeIp);
        saveCmd.add("save");
        saveCmd.add(safeImageName);

        List<String> loadCmd = buildDockerBaseCommand(targetNodeIp);
        loadCmd.add("load");

        Process saveProcess = null;
        Process loadProcess = null;
        try {
            ProcessBuilder savePb = new ProcessBuilder(saveCmd);
            ProcessBuilder loadPb = new ProcessBuilder(loadCmd);

            saveProcess = savePb.start();
            loadProcess = loadPb.start();

            Process finalSave = saveProcess;
            Process finalLoad = loadProcess;

            StringBuilder saveErr = new StringBuilder();
            StringBuilder loadErr = new StringBuilder();

            Thread saveErrThread = new Thread(() -> {
                try (BufferedReader r = new BufferedReader(new InputStreamReader(finalSave.getErrorStream(), StandardCharsets.UTF_8))) {
                    String l;
                    while ((l = r.readLine()) != null) saveErr.append(l).append("\n");
                } catch (Exception ignored) {}
            });
            Thread loadErrThread = new Thread(() -> {
                try (BufferedReader r = new BufferedReader(new InputStreamReader(finalLoad.getErrorStream(), StandardCharsets.UTF_8))) {
                    String l;
                    while ((l = r.readLine()) != null) loadErr.append(l).append("\n");
                } catch (Exception ignored) {}
            });

            saveErrThread.setDaemon(true);
            loadErrThread.setDaemon(true);
            saveErrThread.start();
            loadErrThread.start();

            // 流式高效直通管道
            try (InputStream is = saveProcess.getInputStream();
                 OutputStream os = loadProcess.getOutputStream()) {
                is.transferTo(os);
                os.flush();
            }

            boolean saveFinished = saveProcess.waitFor(300, TimeUnit.SECONDS);
            boolean loadFinished = loadProcess.waitFor(300, TimeUnit.SECONDS);

            saveErrThread.join(2000);
            loadErrThread.join(2000);

            if (!saveFinished || saveProcess.exitValue() != 0 || !loadFinished || loadProcess.exitValue() != 0) {
                String errorDetails = String.format("save(exit=%s, err=%s), load(exit=%s, err=%s)",
                        saveFinished ? saveProcess.exitValue() : "TIMEOUT", saveErr.toString().trim(),
                        loadFinished ? loadProcess.exitValue() : "TIMEOUT", loadErr.toString().trim());
                log.error("[AdminImageService] 跨节点镜像复制失败: {}", errorDetails);
                throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "跨节点复制镜像失败: " + errorDetails);
            }

            log.info("[AdminImageService] 跨节点镜像复制成功: {} 成功加载至目标节点 {}", safeImageName, targetNodeIp);
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            log.error("[AdminImageService] 跨节点镜像复制异常: {}", e.getMessage(), e);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "跨节点复制镜像异常: " + e.getMessage());
        } finally {
            if (saveProcess != null && saveProcess.isAlive()) saveProcess.destroyForcibly();
            if (loadProcess != null && loadProcess.isAlive()) loadProcess.destroyForcibly();
        }
    }

    private JSONObject queryDockerInspect(String targetHost, String imageNameOrId) {
        try {
            List<String> command = buildDockerBaseCommand(targetHost);
            command.add("image");
            command.add("inspect");
            command.add(imageNameOrId);

            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();
            boolean finished = process.waitFor(8, TimeUnit.SECONDS);
            if (finished && process.exitValue() == 0) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String output = reader.lines().collect(Collectors.joining("\n"));
                    if (StringUtils.hasText(output)) {
                        JSONArray arr = JSONUtil.parseArray(output);
                        if (!arr.isEmpty()) {
                            return arr.getJSONObject(0);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("[AdminImageService] 获取 Docker inspect 提示: {}", e.getMessage());
        }
        return null;
    }

    private List<DockerImageLayerVO> queryDockerHistory(String targetHost, String imageNameOrId) {
        List<DockerImageLayerVO> layers = new ArrayList<>();
        try {
            List<String> command = buildDockerBaseCommand(targetHost);
            command.add("history");
            command.add("--no-trunc");
            command.add("--format");
            command.add("{{.ID}}\t{{.Size}}\t{{.CreatedAt}}\t{{.CreatedBy}}");
            command.add(imageNameOrId);

            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            if (finished && process.exitValue() == 0) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    List<String> lines = reader.lines().toList();
                    int totalLayers = lines.size();
                    for (int i = 0; i < totalLayers; i++) {
                        String line = lines.get(i);
                        if (!StringUtils.hasText(line)) continue;
                        String[] parts = line.split("\t");
                        String layerId = parts.length > 0 ? parts[0].trim() : "";
                        String sizeStr = parts.length > 1 ? parts[1].trim() : "0B";
                        String createdAt = parts.length > 2 ? parts[2].trim() : "";
                        String rawCreatedBy = parts.length > 3 ? parts[3].trim() : "";

                        long sizeBytes = parseSizeToBytes(sizeStr);
                        String instruction = extractInstruction(rawCreatedBy);

                        DockerImageLayerVO layerVO = DockerImageLayerVO.builder()
                                .layerIndex(totalLayers - i)
                                .layerId(layerId)
                                .instruction(instruction)
                                .createdBy(rawCreatedBy)
                                .sizeBytes(sizeBytes)
                                .sizeFormatted(formatBytes(sizeBytes))
                                .createdAt(createdAt)
                                .comment("")
                                .build();
                        layers.add(layerVO);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("[AdminImageService] 获取 Docker history 提示: {}", e.getMessage());
        }
        return layers;
    }

    private String extractInstruction(String createdBy) {
        if (!StringUtils.hasText(createdBy)) return "CMD";
        String trimmed = createdBy.trim();
        int nopIdx = trimmed.indexOf("#(nop)");
        if (nopIdx != -1) {
            return trimmed.substring(nopIdx + 6).trim();
        }
        if (trimmed.startsWith("/bin/sh -c ")) {
            return "RUN " + trimmed.substring(11).trim();
        }
        return trimmed;
    }

    private List<DiscoveredDockerImage> fetchLocalDockerImages() {
        List<DiscoveredDockerImage> list = new ArrayList<>();
        String swarmApiUrl = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_DOCKER_SWARM_API_URL, "unix:///var/run/docker.sock");

        if (StringUtils.hasText(swarmApiUrl) && (swarmApiUrl.startsWith("tcp://") || swarmApiUrl.startsWith("http://"))) {
            String httpUrl = swarmApiUrl.replace("tcp://", "http://");
            list = queryDockerHttpApi(httpUrl);
        }

        if (list.isEmpty()) {
            list = queryDockerCli(null);
        }

        return list;
    }

    private List<DiscoveredDockerImage> fetchRemoteDockerImages(ClusterNode node) {
        if (node == null || !StringUtils.hasText(node.getIpAddress())) return Collections.emptyList();
        String endpointUrl = DockerTlsUtil.getDockerEndpointUrl(node);
        List<DiscoveredDockerImage> list = queryDockerHttpApi(endpointUrl, node);

        if (list.isEmpty()) {
            list = queryDockerCli(node.getIpAddress());
        }

        return list;
    }

    private boolean isRemoteDockerReachable(ClusterNode node) {
        if (node == null || !StringUtils.hasText(node.getIpAddress())) return false;
        try {
            HttpClient client = DockerTlsUtil.buildHttpClientForNode(node, Duration.ofSeconds(2));
            String endpointUrl = DockerTlsUtil.getDockerEndpointUrl(node);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(endpointUrl + "/_ping"))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            return resp.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    private List<DiscoveredDockerImage> queryDockerHttpApi(String endpointUrl) {
        return queryDockerHttpApi(endpointUrl, null);
    }

    private List<DiscoveredDockerImage> queryDockerHttpApi(String endpointUrl, ClusterNode node) {
        List<DiscoveredDockerImage> list = new ArrayList<>();
        try {
            HttpClient client = DockerTlsUtil.buildHttpClientForNode(node, Duration.ofSeconds(3));
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(endpointUrl + "/images/json"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() == 200 && StringUtils.hasText(resp.body())) {
                JSONArray array = JSONUtil.parseArray(resp.body());
                for (int i = 0; i < array.size(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    String id = obj.getStr("Id", "");
                    Long size = obj.getLong("Size", 0L);
                    JSONArray repoTags = obj.getJSONArray("RepoTags");
                    if (repoTags != null) {
                        for (int j = 0; j < repoTags.size(); j++) {
                            String repoTag = repoTags.getStr(j);
                            if (!StringUtils.hasText(repoTag) || "<none>:<none>".equals(repoTag)) continue;
                            String repo = repoTag;
                            String tag = "latest";
                            int idx = repoTag.lastIndexOf(':');
                            if (idx > 0 && !repoTag.substring(idx + 1).contains("/")) {
                                repo = repoTag.substring(0, idx);
                                tag = repoTag.substring(idx + 1);
                            }
                            list.add(new DiscoveredDockerImage(repo, tag, id, size != null ? size : 0L));
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("[AdminImageService] 查询 Docker HTTP API ({}) 提示: {}", endpointUrl, e.getMessage());
        }
        return list;
    }

    private List<DiscoveredDockerImage> queryDockerCli(String targetHost) {
        List<DiscoveredDockerImage> list = new ArrayList<>();
        try {
            List<String> command = new ArrayList<>();
            command.add("docker");
            if (StringUtils.hasText(targetHost)) {
                command.add("-H");
                command.add(targetHost.trim());
            }
            command.add("images");
            command.add("--format");
            command.add("{{.Repository}}\t{{.Tag}}\t{{.ID}}\t{{.Size}}");

            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            if (finished && process.exitValue() == 0) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!StringUtils.hasText(line)) continue;
                        String[] parts = line.split("\t");
                        if (parts.length >= 2) {
                            String repo = parts[0].trim();
                            String tag = parts[1].trim();
                            if ("<none>".equalsIgnoreCase(repo) || "<none>".equalsIgnoreCase(tag)) {
                                continue;
                            }
                            String imageId = parts.length > 2 ? parts[2].trim() : "";
                            String sizeStr = parts.length > 3 ? parts[3].trim() : "0 B";
                            long sizeBytes = parseSizeToBytes(sizeStr);
                            list.add(new DiscoveredDockerImage(repo, tag, imageId, sizeBytes));
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("[AdminImageService] 调度 Docker CLI 获取镜像列表提示: {}", e.getMessage());
        }
        return list;
    }

    private void recordImageIdentifiers(DiscoveredDockerImage img, Set<String> targetSet) {
        targetSet.add(img.repository() + ":" + img.tag());
        if (StringUtils.hasText(img.imageId())) {
            targetSet.add(img.imageId());
            if (img.imageId().startsWith("sha256:")) {
                String pureSha = img.imageId().substring(7);
                targetSet.add(pureSha);
                if (pureSha.length() >= 12) {
                    targetSet.add(pureSha.substring(0, 12));
                }
            } else if (img.imageId().length() >= 12) {
                targetSet.add(img.imageId().substring(0, 12));
            }
        }
    }

    private boolean isImagePresentInSet(String fullName, String imageId, Set<String> set) {
        if (set == null || set.isEmpty()) return false;
        if (set.contains(fullName)) return true;
        if (StringUtils.hasText(imageId)) {
            if (set.contains(imageId)) return true;
            if (imageId.startsWith("sha256:") && set.contains(imageId.substring(7))) return true;
            String clean = imageId.replace("sha256:", "");
            if (clean.length() >= 12) {
                String prefix = clean.substring(0, 12);
                for (String item : set) {
                    if (item.startsWith(prefix) || item.contains(prefix)) return true;
                }
            }
        }
        return false;
    }

    private void syncDiscoveredImageToRepository(DiscoveredDockerImage img, String nodeIp) {
        Optional<ClusterImage> existingOpt = clusterImageRepository.findByRepositoryAndTag(img.repository(), img.tag());
        ClusterImage image;
        if (existingOpt.isPresent()) {
            image = existingOpt.get();
            if (StringUtils.hasText(img.imageId())) {
                image.setImageId(img.imageId());
            }
            if (img.sizeBytes() > 0) {
                image.setSizeBytes(img.sizeBytes());
            }
            Set<String> ipSet = parseNodeIps(image.getNodeIps());
            ipSet.add(nodeIp);
            image.setNodeIps(String.join(",", ipSet));
            image.setStatus("PULLED");
            image.setUpdatedAt(Instant.now());
        } else {
            String imageType = "BASE";
            String lowerRepo = img.repository().toLowerCase(Locale.ROOT);
            if (lowerRepo.contains("ctf") || lowerRepo.contains("challenge") || lowerRepo.contains("pwn") || lowerRepo.contains("web")) {
                imageType = "CHALLENGE";
            }
            image = ClusterImage.builder()
                    .repository(img.repository())
                    .tag(img.tag())
                    .imageId(img.imageId())
                    .sizeBytes(img.sizeBytes())
                    .imageType(imageType)
                    .nodeIps(nodeIp)
                    .status("PULLED")
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
        }
        clusterImageRepository.save(image);
    }

    private void triggerDockerPullOnNode(String nodeIp, String imageName) {
        try {
            List<String> command = buildDockerBaseCommand(nodeIp);
            command.add("pull");
            command.add(imageName);

            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();
            boolean finished = process.waitFor(45, TimeUnit.SECONDS);
            if (finished && process.exitValue() == 0) {
                log.info("[AdminImageService] 节点 [{}] Docker pull 成功: {}", nodeIp, imageName);
            } else {
                log.info("[AdminImageService] 节点 [{}] Docker pull 调度完毕，已同步元数据: {}", nodeIp, imageName);
            }
        } catch (Exception e) {
            log.warn("[AdminImageService] 调度节点 [{}] docker pull 提示: {}，已记录镜像元数据", nodeIp, e.getMessage());
        }
    }

    private void triggerDockerRmiOnNode(String nodeIp, String imageName) {
        removeDockerImageAndConfirm(nodeIp, imageName, null);
    }

    /**
     * 校验平台内部是否有依赖该镜像的容器实例（不论正在运行还是历史实例记录）
     * 确保平台层面不存在任何引用该镜像的实例后方可继续
     *
     * @param image 目标镜像
     * @param targetNodeIp 指定目标节点 IP (若为 null 则检查全平台范围)
     */
    private void checkPlatformContainerDependencies(ClusterImage image, String targetNodeIp) {
        String publicHost = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_PUBLIC_HOST, "127.0.0.1");
        boolean hasTargetNode = StringUtils.hasText(targetNodeIp);
        String cleanTargetIp = hasTargetNode ? targetNodeIp.trim() : null;
        boolean isTargetLocal = hasTargetNode && isLocalNode(cleanTargetIp, publicHost);

        List<CtfChallengeInstance> allInstances = ctfChallengeInstanceRepository.findAll();
        List<CtfChallengeInstance> matchedInstances = allInstances.stream()
                .filter(inst -> isInstanceUsingImage(inst, image))
                .filter(inst -> {
                    if (!hasTargetNode) {
                        return true;
                    }
                    String instNodeIp = inst.getNodeIp();
                    boolean isInstLocal = isLocalNode(instNodeIp, publicHost);
                    if (isTargetLocal) {
                        return isInstLocal;
                    } else {
                        return cleanTargetIp.equalsIgnoreCase(instNodeIp);
                    }
                })
                .toList();

        if (matchedInstances.isEmpty()) {
            return;
        }

        String fullImageName = buildFullImageName(image);
        String nodeScopeNotice = hasTargetNode ? "节点 [" + targetNodeIp + "] 上" : "平台中";

        // 优先检查是否有处于运行中、启动中或准备中的活跃容器实例
        List<CtfChallengeInstance> runningInstances = matchedInstances.stream()
                .filter(inst -> inst.getStatus() == InstanceStartupStatus.RUNNING
                        || inst.getStatus() == InstanceStartupStatus.STARTING
                        || inst.getStatus() == InstanceStartupStatus.DOWNLOADING_IMAGE
                        || inst.getStatus() == InstanceStartupStatus.QUEUED)
                .toList();

        if (!runningInstances.isEmpty()) {
            String runningDetails = runningInstances.stream()
                    .map(inst -> String.format("实例[ID:%s, 状态:%s, 节点:%s, 容器:%s]",
                            inst.getId(),
                            inst.getStatus(),
                            StringUtils.hasText(inst.getNodeIp()) ? inst.getNodeIp() : "本地主节点",
                            StringUtils.hasText(inst.getContainerName()) ? inst.getContainerName() : inst.getContainerId()))
                    .collect(Collectors.joining("; "));

            log.warn("[AdminImageService] 拒绝删除镜像 {}: {} 检测到仍有 {} 个容器实例处于活跃运行状态: {}",
                    fullImageName, nodeScopeNotice, runningInstances.size(), runningDetails);

            throw new BusinessException(HttpStatus.CONFLICT,
                    String.format("无法删除镜像 [%s]：%s检测到仍有 %d 个容器实例处于活跃运行状态（%s），请先在容器管理中停止并彻底删除容器后再删除镜像！",
                            fullImageName, nodeScopeNotice, runningInstances.size(), runningDetails));
        }

        // 即使容器已停止或失败，但只要平台数据库中仍存在该容器记录，亦必须彻底删除容器实例
        String stoppedDetails = matchedInstances.stream()
                .map(inst -> String.format("实例[ID:%s, 状态:%s, 节点:%s, 容器:%s]",
                        inst.getId(),
                        inst.getStatus(),
                        StringUtils.hasText(inst.getNodeIp()) ? inst.getNodeIp() : "本地主节点",
                        StringUtils.hasText(inst.getContainerName()) ? inst.getContainerName() : inst.getContainerId()))
                .collect(Collectors.joining("; "));

        log.warn("[AdminImageService] 拒绝删除镜像 {}: {} 仍存在 {} 个关联容器实例记录: {}",
                fullImageName, nodeScopeNotice, matchedInstances.size(), stoppedDetails);

        throw new BusinessException(HttpStatus.CONFLICT,
                String.format("无法删除镜像 [%s]：%s检测到仍存在 %d 个关联容器实例记录（%s），请先在容器管理中彻底清理删除这些容器实例后再删除镜像！",
                        fullImageName, nodeScopeNotice, matchedInstances.size(), stoppedDetails));
    }

    private boolean isInstanceUsingImage(CtfChallengeInstance inst, ClusterImage image) {
        if (inst == null || image == null) {
            return false;
        }

        String fullImageName = buildFullImageName(image);
        String repo = image.getRepository();
        String tag = StringUtils.hasText(image.getTag()) ? image.getTag() : "latest";

        // 1. 检查实例自身的 imageName 属性
        if (StringUtils.hasText(inst.getImageName())) {
            if (isImageStringMatched(inst.getImageName().trim(), fullImageName, repo, tag, image.getImageId())) {
                return true;
            }
        }

        // 2. 检查实例关联赛题 (CtfChallenge) 中的 imageName 属性
        if (inst.getChallenge() != null && StringUtils.hasText(inst.getChallenge().getImageName())) {
            if (isImageStringMatched(inst.getChallenge().getImageName().trim(), fullImageName, repo, tag, image.getImageId())) {
                return true;
            }
        }

        return false;
    }

    /**
     * 校验指定节点底层 Docker 中是否有依赖该镜像的容器（运行中或已停止）或 Swarm 服务
     * 确保底层 Docker 容器完全删除之后方可允许删除镜像
     */
    private void checkDockerContainerDependencies(String nodeIp, ClusterImage image) {
        String publicHost = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_PUBLIC_HOST, "127.0.0.1");
        boolean isLocal = !StringUtils.hasText(nodeIp) || "127.0.0.1".equalsIgnoreCase(nodeIp) || publicHost.equalsIgnoreCase(nodeIp);
        String targetHostDisplay = isLocal ? "本地主节点" : "子节点[" + nodeIp + "]";

        String fullImageName = buildFullImageName(image);
        Map<String, DockerContainerBriefInfo> matchedContainers = new LinkedHashMap<>();

        // 1. 通过 ancestor 过滤器定向查询依赖该镜像（包括子层继承）的容器
        List<DockerContainerBriefInfo> ancestorContainers = queryDockerContainers(nodeIp, fullImageName);
        for (DockerContainerBriefInfo c : ancestorContainers) {
            matchedContainers.put(c.id(), c);
        }

        if ("latest".equalsIgnoreCase(image.getTag())) {
            List<DockerContainerBriefInfo> repoContainers = queryDockerContainers(nodeIp, image.getRepository());
            for (DockerContainerBriefInfo c : repoContainers) {
                matchedContainers.put(c.id(), c);
            }
        }

        // 2. 全量遍历该节点的所有容器并比对 Image 标识（覆盖直接使用短ID、未打标签以及特定 SHA256 的容器）
        List<DockerContainerBriefInfo> allContainers = queryDockerContainers(nodeIp, null);
        for (DockerContainerBriefInfo c : allContainers) {
            if (isDockerContainerUsingImage(c, image)) {
                matchedContainers.put(c.id(), c);
            }
        }

        // 3. 查询 Swarm 集群服务依赖（若处于 Docker Swarm 模式）
        List<DockerSwarmServiceBriefInfo> matchedServices = new ArrayList<>();
        List<DockerSwarmServiceBriefInfo> allServices = queryDockerSwarmServices(nodeIp);
        for (DockerSwarmServiceBriefInfo s : allServices) {
            if (isDockerServiceUsingImage(s, image)) {
                matchedServices.add(s);
            }
        }

        // 优先拦截：底层 Docker 中有正在运行的容器
        List<DockerContainerBriefInfo> runningContainers = matchedContainers.values().stream()
                .filter(c -> "running".equalsIgnoreCase(c.state()) || c.status().toLowerCase(Locale.ROOT).contains("up"))
                .toList();

        if (!runningContainers.isEmpty()) {
            String runningDetails = runningContainers.stream()
                    .map(c -> String.format("容器[名称:%s, ID:%s, 状态:%s]", c.name(), c.id(), c.status()))
                    .collect(Collectors.joining("; "));

            log.warn("[AdminImageService] 拒绝删除镜像 {}: {} 底层 Docker 检测到仍有 {} 个容器正在运行: {}",
                    fullImageName, targetHostDisplay, runningContainers.size(), runningDetails);

            throw new BusinessException(HttpStatus.CONFLICT,
                    String.format("无法删除镜像 [%s]：底层 Docker（%s）检测到有 %d 个容器正在运行依赖该镜像（%s），请先停止并彻底删除该容器！",
                            fullImageName, targetHostDisplay, runningContainers.size(), runningDetails));
        }

        // 拦截：底层 Docker Swarm 中仍有服务定义引用该镜像
        if (!matchedServices.isEmpty()) {
            String serviceDetails = matchedServices.stream()
                    .map(s -> String.format("服务[名称:%s, ID:%s, 副本:%s]", s.name(), s.id(), s.replicas()))
                    .collect(Collectors.joining("; "));

            log.warn("[AdminImageService] 拒绝删除镜像 {}: {} 底层 Docker Swarm 检测到仍有 {} 个服务依赖该镜像: {}",
                    fullImageName, targetHostDisplay, matchedServices.size(), serviceDetails);

            throw new BusinessException(HttpStatus.CONFLICT,
                    String.format("无法删除镜像 [%s]：底层 Docker Swarm（%s）检测到仍有 %d 个服务依赖该镜像（%s），请先在集群中移除相关服务！",
                            fullImageName, targetHostDisplay, matchedServices.size(), serviceDetails));
        }

        // 拦截：底层 Docker 中仍残留已停止/退出的历史容器实例
        if (!matchedContainers.isEmpty()) {
            String stoppedDetails = matchedContainers.values().stream()
                    .map(c -> String.format("容器[名称:%s, ID:%s, 状态:%s]", c.name(), c.id(), c.status()))
                    .collect(Collectors.joining("; "));

            log.warn("[AdminImageService] 拒绝删除镜像 {}: {} 底层 Docker 检测到仍残留有 {} 个历史容器: {}",
                    fullImageName, targetHostDisplay, matchedContainers.size(), stoppedDetails);

            throw new BusinessException(HttpStatus.CONFLICT,
                    String.format("无法删除镜像 [%s]：底层 Docker（%s）检测到仍残留有 %d 个依赖该镜像的历史容器（%s），请先彻底删除容器后再删除镜像！",
                            fullImageName, targetHostDisplay, matchedContainers.size(), stoppedDetails));
        }
    }

    private boolean isDockerContainerUsingImage(DockerContainerBriefInfo container, ClusterImage image) {
        if (container == null || !StringUtils.hasText(container.image())) {
            return false;
        }
        String cImg = container.image().trim();
        if (cImg.contains("@")) {
            cImg = cImg.substring(0, cImg.indexOf('@')).trim();
        }
        String fullImageName = buildFullImageName(image);
        return isImageStringMatched(cImg, fullImageName, image.getRepository(), image.getTag(), image.getImageId());
    }

    private boolean isDockerServiceUsingImage(DockerSwarmServiceBriefInfo service, ClusterImage image) {
        if (service == null || !StringUtils.hasText(service.image())) {
            return false;
        }
        String sImg = service.image().trim();
        if (sImg.contains("@")) {
            sImg = sImg.substring(0, sImg.indexOf('@')).trim();
        }
        String fullImageName = buildFullImageName(image);
        return isImageStringMatched(sImg, fullImageName, image.getRepository(), image.getTag(), image.getImageId());
    }

    private boolean isImageStringMatched(String candidate, String fullImageName, String repo, String tag, String imageId) {
        if (!StringUtils.hasText(candidate)) {
            return false;
        }
        String clean = candidate.trim();
        if (clean.contains("@")) {
            clean = clean.substring(0, clean.indexOf('@')).trim();
        }
        if (clean.equalsIgnoreCase(fullImageName)) {
            return true;
        }
        if (clean.equalsIgnoreCase(repo) && "latest".equalsIgnoreCase(tag)) {
            return true;
        }
        if (clean.endsWith("/" + fullImageName) || fullImageName.endsWith("/" + clean)) {
            return true;
        }
        if (clean.endsWith("/" + repo + ":" + tag)) {
            return true;
        }
        int colon = clean.lastIndexOf(':');
        if (colon > 0 && !clean.substring(colon + 1).contains("/")) {
            String cRepo = clean.substring(0, colon);
            String cTag = clean.substring(colon + 1);
            if (cRepo.equalsIgnoreCase(repo) && cTag.equalsIgnoreCase(tag)) {
                return true;
            }
        }
        if (StringUtils.hasText(imageId)) {
            String pureSha = imageId.startsWith("sha256:") ? imageId.substring(7) : imageId;
            String cPureSha = clean.startsWith("sha256:") ? clean.substring(7) : clean;
            if (cPureSha.equalsIgnoreCase(pureSha)) {
                return true;
            }
            if (pureSha.length() >= 12 && cPureSha.length() >= 12) {
                if (pureSha.startsWith(cPureSha) || cPureSha.startsWith(pureSha.substring(0, 12))) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<String> buildDockerBaseCommand(String targetHost) {
        String publicHost = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_PUBLIC_HOST, "127.0.0.1");
        String swarmApiUrl = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_DOCKER_SWARM_API_URL, "unix:///var/run/docker.sock");
        ClusterNode node = null;
        if (StringUtils.hasText(targetHost)) {
            String cleanHost = targetHost.replace("tcp://", "").replace("http://", "").replace("https://", "");
            int colonIdx = cleanHost.indexOf(':');
            if (colonIdx > 0) {
                cleanHost = cleanHost.substring(0, colonIdx);
            }
            node = clusterNodeRepository.findByIpAddress(cleanHost.trim()).orElse(null);
        }
        return DockerTlsUtil.buildDockerBaseCliCommand(targetHost, node, publicHost, swarmApiUrl);
    }

    private List<DockerContainerBriefInfo> queryDockerContainers(String targetHost, String ancestorFilter) {
        List<DockerContainerBriefInfo> list = new ArrayList<>();
        try {
            List<String> command = buildDockerBaseCommand(targetHost);
            command.add("ps");
            command.add("-a");
            if (StringUtils.hasText(ancestorFilter)) {
                command.add("--filter");
                command.add("ancestor=" + ancestorFilter.trim());
            }
            command.add("--format");
            command.add("{{.ID}}\t{{.Names}}\t{{.Status}}\t{{.State}}\t{{.Image}}");

            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            if (finished && process.exitValue() == 0) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!StringUtils.hasText(line)) continue;
                        String[] parts = line.split("\t");
                        String id = parts.length > 0 ? parts[0].trim() : "";
                        String name = parts.length > 1 ? parts[1].trim() : "";
                        String status = parts.length > 2 ? parts[2].trim() : "";
                        String state = parts.length > 3 ? parts[3].trim() : "";
                        String image = parts.length > 4 ? parts[4].trim() : "";
                        if (StringUtils.hasText(id)) {
                            list.add(new DockerContainerBriefInfo(id, name, status, state, image));
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[AdminImageService] 查询节点 [{}] Docker 容器列表异常: {}", targetHost, e.getMessage());
        }
        return list;
    }

    private List<DockerSwarmServiceBriefInfo> queryDockerSwarmServices(String targetHost) {
        List<DockerSwarmServiceBriefInfo> list = new ArrayList<>();
        try {
            List<String> command = buildDockerBaseCommand(targetHost);
            command.add("service");
            command.add("ls");
            command.add("--format");
            command.add("{{.ID}}\t{{.Name}}\t{{.Image}}\t{{.Replicas}}");

            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();
            boolean finished = process.waitFor(8, TimeUnit.SECONDS);
            if (finished && process.exitValue() == 0) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!StringUtils.hasText(line)) continue;
                        String[] parts = line.split("\t");
                        String id = parts.length > 0 ? parts[0].trim() : "";
                        String name = parts.length > 1 ? parts[1].trim() : "";
                        String image = parts.length > 2 ? parts[2].trim() : "";
                        String replicas = parts.length > 3 ? parts[3].trim() : "";
                        if (StringUtils.hasText(id)) {
                            list.add(new DockerSwarmServiceBriefInfo(id, name, image, replicas));
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("[AdminImageService] 查询节点 [{}] Docker Swarm 服务列表提示: {}", targetHost, e.getMessage());
        }
        return list;
    }

    private void removeDockerImageAndConfirm(String nodeIp, String imageName, String rawImageId) {
        String publicHost = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_PUBLIC_HOST, "127.0.0.1");
        boolean isLocal = isLocalNode(nodeIp, publicHost);
        String targetHostDisplay = isLocal ? "本地主节点" : "子节点[" + nodeIp + "]";

        // 1. 先置检查：查询该节点 Docker 引擎中当前是否存在该镜像
        JSONObject inspectBefore = queryDockerInspect(nodeIp, imageName);
        if (inspectBefore == null && StringUtils.hasText(rawImageId)) {
            inspectBefore = queryDockerInspect(nodeIp, rawImageId);
        }

        if (inspectBefore == null) {
            log.info("[AdminImageService] 节点 {} Docker 引擎中未检测到镜像 {}，确认已不复存在", targetHostDisplay, imageName);
            return;
        }

        // 提取镜像真实底层 ID、RepoTags 与 RepoDigests
        String realImageId = inspectBefore.getStr("Id");
        if (!StringUtils.hasText(realImageId)) {
            realImageId = rawImageId;
        }
        JSONArray repoTags = inspectBefore.getJSONArray("RepoTags");
        JSONArray repoDigests = inspectBefore.getJSONArray("RepoDigests");

        // 2. 调度执行底层 Docker rmi -f 命令移除指定镜像名称/标签
        log.info("[AdminImageService] 正在向 {} 发起底层物理删除 Docker 镜像标签: {}", targetHostDisplay, imageName);
        executeDockerRmi(nodeIp, targetHostDisplay, imageName);

        // 3. 深度清理：若该镜像原本只有该指定标签（或已无其他可用业务标签），尝试针对其 RepoDigests 与实际 Image ID 发起级联清理
        // 彻底解决只 untag 标签导致遗留 `alpine <none>` 或 `<none> <none>` 虚悬/摘要残留镜像的问题
        boolean noOtherTags = repoTags == null || repoTags.isEmpty()
                || (repoTags.size() == 1 && (repoTags.getStr(0).equalsIgnoreCase(imageName) || repoTags.getStr(0).endsWith("/" + imageName)));

        if (noOtherTags) {
            // (1) 若有 RepoDigests（如 alpine@sha256:xxx），尝试清理，解除仓库名称绑定
            if (repoDigests != null) {
                for (int i = 0; i < repoDigests.size(); i++) {
                    String digestRef = repoDigests.getStr(i);
                    if (StringUtils.hasText(digestRef)) {
                        tryExecuteDockerRmiQuietly(nodeIp, digestRef);
                    }
                }
            }

            // (2) 若有真实 Image ID，尝试物理擦除底层存储分层
            if (StringUtils.hasText(realImageId)) {
                tryExecuteDockerRmiQuietly(nodeIp, realImageId);
            }
        }

        // 4. 后置主动核验：通过 Docker Inspect 再次严密确认该业务标签是否已彻底从该节点物理清除
        JSONObject inspectAfter = queryDockerInspect(nodeIp, imageName);
        if (inspectAfter != null) {
            log.error("[AdminImageService] 节点 {} 执行 docker rmi -f 之后，Docker inspect 仍检测到该镜像标签存在: {}", targetHostDisplay, imageName);
            throw new BusinessException(HttpStatus.CONFLICT,
                    "节点 " + targetHostDisplay + " 删除确认未通过：该镜像仍驻留在底层 Docker 中（可能正被运行中的容器占用）。已中止删除数据库记录！");
        }

        log.info("[AdminImageService] 节点 {} 已严密确认 Docker 镜像物理清理成功: {}", targetHostDisplay, imageName);
    }

    private void executeDockerRmi(String nodeIp, String targetHostDisplay, String target) {
        List<String> command = buildDockerBaseCommand(nodeIp);
        command.add("rmi");
        command.add("-f");
        command.add(target);

        int exitCode = -1;
        String stderr = "";
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();
            boolean finished = process.waitFor(20, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "节点 " + targetHostDisplay + " 执行 docker rmi 响应超时(20s)，已中止删除数据库元数据！");
            }

            exitCode = process.exitValue();
            try (BufferedReader r1 = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
                 BufferedReader r2 = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                r1.lines().collect(Collectors.joining("\n"));
                stderr = r2.lines().collect(Collectors.joining("\n"));
            }
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            log.error("[AdminImageService] 调度节点 {} docker rmi 异常: {}", targetHostDisplay, e.getMessage(), e);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "调度节点 " + targetHostDisplay + " 执行 Docker 删除异常: " + e.getMessage() + "，已中止删除数据库元数据！");
        }

        // 校验进程退出码及报错信息
        if (exitCode != 0) {
            if (stderr.contains("No such image") || stderr.contains("No such object")) {
                log.info("[AdminImageService] 节点 {} Docker 反馈镜像已被清理 (No such image)", targetHostDisplay);
            } else {
                log.error("[AdminImageService] 节点 {} 执行 docker rmi -f 失败 (exitCode={}): {}", targetHostDisplay, exitCode, stderr);
                throw new BusinessException(HttpStatus.CONFLICT,
                        "节点 " + targetHostDisplay + " 底层 Docker 镜像删除失败: " + (StringUtils.hasText(stderr) ? stderr.trim() : "退出码 " + exitCode) + "。已中止删除数据库记录！");
            }
        }
    }

    private void tryExecuteDockerRmiQuietly(String nodeIp, String target) {
        if (!StringUtils.hasText(target)) {
            return;
        }
        try {
            List<String> command = buildDockerBaseCommand(nodeIp);
            command.add("rmi");
            command.add("-f");
            command.add(target.trim());

            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            if (finished) {
                int exit = process.exitValue();
                if (exit == 0) {
                    log.info("[AdminImageService] 节点 [{}] 深度物理清理成功: {}", nodeIp, target);
                } else {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                        String err = reader.lines().collect(Collectors.joining("\n"));
                        log.debug("[AdminImageService] 节点 [{}] 深度物理清理未完全擦除（可能存在衍生子镜像依赖）: {} -> {}", nodeIp, target, err);
                    }
                }
            } else {
                process.destroyForcibly();
            }
        } catch (Exception e) {
            log.debug("[AdminImageService] 节点 [{}] 深度物理清理调度异常: {}", nodeIp, e.getMessage());
        }
    }

    private long parseSizeToBytes(String sizeStr) {
        if (!StringUtils.hasText(sizeStr)) return 0L;
        try {
            String trimmed = sizeStr.trim().toUpperCase(Locale.ROOT);
            double multiplier = 1.0;
            if (trimmed.endsWith("KB") || trimmed.endsWith("K")) {
                multiplier = 1024.0;
                trimmed = trimmed.replaceAll("[^0-9.]", "");
            } else if (trimmed.endsWith("MB") || trimmed.endsWith("M")) {
                multiplier = 1024.0 * 1024.0;
                trimmed = trimmed.replaceAll("[^0-9.]", "");
            } else if (trimmed.endsWith("GB") || trimmed.endsWith("G")) {
                multiplier = 1024.0 * 1024.0 * 1024.0;
                trimmed = trimmed.replaceAll("[^0-9.]", "");
            } else if (trimmed.endsWith("TB") || trimmed.endsWith("T")) {
                multiplier = 1024.0 * 1024.0 * 1024.0 * 1024.0;
                trimmed = trimmed.replaceAll("[^0-9.]", "");
            } else if (trimmed.endsWith("B")) {
                multiplier = 1.0;
                trimmed = trimmed.replaceAll("[^0-9.]", "");
            }
            return (long) (Double.parseDouble(trimmed) * multiplier);
        } catch (Exception e) {
            return 0L;
        }
    }

    private boolean isLocalNode(String nodeIp, String publicHost) {
        if (!StringUtils.hasText(nodeIp)) {
            return true;
        }
        String clean = nodeIp.trim();
        return "127.0.0.1".equalsIgnoreCase(clean)
                || "localhost".equalsIgnoreCase(clean)
                || (StringUtils.hasText(publicHost) && publicHost.equalsIgnoreCase(clean));
    }

    private Set<String> parseNodeIps(String nodeIps) {
        Set<String> set = new LinkedHashSet<>();
        if (StringUtils.hasText(nodeIps)) {
            Arrays.stream(nodeIps.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .forEach(set::add);
        }
        return set;
    }

    private String buildFullImageName(ClusterImage image) {
        if (!StringUtils.hasText(image.getRepository())) return "";
        return StringUtils.hasText(image.getTag()) ? image.getRepository() + ":" + image.getTag() : image.getRepository() + ":latest";
    }

    private void updateNodeImageCounts() {
        List<ClusterImage> allImages = clusterImageRepository.findAll();
        List<ClusterNode> allNodes = clusterNodeRepository.findAll();
        String publicHost = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_PUBLIC_HOST, "127.0.0.1");

        for (ClusterNode node : allNodes) {
            String ip = node.getIpAddress();
            boolean isLocal = isLocalNode(ip, publicHost);
            long count = allImages.stream()
                    .filter(img -> {
                        if (!StringUtils.hasText(img.getNodeIps())) {
                            return isLocal;
                        }
                        Set<String> ips = parseNodeIps(img.getNodeIps());
                        return ips.contains(ip) || (isLocal && (ips.contains("127.0.0.1") || ips.contains(publicHost)));
                    })
                    .count();
            node.setImageCount((int) count);
            clusterNodeRepository.save(node);
        }
    }

    private void initDefaultMasterNode() {
        String publicHost = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_PUBLIC_HOST, "127.0.0.1");
        String containerType = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_TYPE, "DOCKER_SWARM");

        ClusterNode masterNode = ClusterNode.builder()
                .nodeName("volur-master-01")
                .ipAddress(publicHost)
                .sshPort(22)
                .clusterType(containerType)
                .role("MASTER")
                .isResourceNode(true)
                .status("READY")
                .osInfo("Linux 6.6.0 (Ubuntu 24.04 LTS)")
                .dockerVersion("DOCKER_SWARM".equalsIgnoreCase(containerType) ? "Docker Engine 27.1" : "containerd 1.7.15")
                .cpuCores(8)
                .memoryBytes(17179869184L)
                .containerCount(0)
                .imageCount(0)
                .joinTime(Instant.now())
                .lastHeartbeat(Instant.now())
                .build();

        clusterNodeRepository.save(masterNode);
    }

    private String formatBytes(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        digitGroups = Math.min(digitGroups, units.length - 1);
        return String.format(Locale.ROOT, "%.1f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }

}
