package com.humolix.volur.container.service.admin.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.humolix.volur.container.client.DockerRegistryClient;
import com.humolix.volur.container.domain.dto.AdminImageBackupDTO;
import com.humolix.volur.container.domain.dto.AdminImagePageRequestDTO;
import com.humolix.volur.container.domain.dto.AdminImagePullRequestDTO;
import com.humolix.volur.container.domain.dto.AdminImageTransferDTO;
import com.humolix.volur.container.domain.entity.ClusterImage;
import com.humolix.volur.container.domain.entity.ClusterNode;
import com.humolix.volur.container.domain.entity.CtfChallengeInstance;
import com.humolix.volur.container.domain.enums.InstanceStartupStatus;
import com.humolix.volur.container.domain.record.DiscoveredDockerImage;
import com.humolix.volur.container.domain.record.DockerContainerBriefInfo;
import com.humolix.volur.container.domain.record.DockerSwarmServiceBriefInfo;
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
import com.humolix.volur.system.domain.enums.AsyncTaskType;
import com.humolix.volur.system.domain.vo.AdminAsyncTaskSubmitVO;
import com.humolix.volur.system.service.SystemSettingService;
import com.humolix.volur.system.service.admin.AdminAsyncTaskService;
import com.humolix.volur.system.service.admin.AsyncTaskContext;
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

import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.UnixDomainSocketAddress;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
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
    private final AdminAsyncTaskService adminAsyncTaskService;
    private final DockerRegistryClient dockerRegistryClient;

    @Override
    public AdminClusterImageStatisticsVO getStatistics() {
        boolean registryOnline = dockerRegistryClient.isAvailable();

        List<ClusterImage> registryImages = clusterImageRepository.findByInRegistryTrue();
        long totalImages = registryImages.size();
        long totalSizeBytes = registryImages.stream()
                .mapToLong(img -> img.getSizeBytes() != null ? img.getSizeBytes() : 0L)
                .sum();

        return clusterImageMapper.toStatisticsVO(
                totalImages,
                totalSizeBytes,
                registryOnline,
                totalImages
        );
    }

    @Override
    public PageResponse<AdminClusterImageVO> listImages(AdminImagePageRequestDTO request) {
        Pageable pageable = request.toPageable(ClusterImage.class, "createdAt");

        Specification<ClusterImage> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            // 严禁展示宿主机或非托管环境的杂项镜像，仅展示托管在私有 Registry 中的镜像
            predicates.add(cb.isTrue(root.get("inRegistry")));

            if (StringUtils.hasText(request.getKeyword())) {
                String kw = "%" + request.getKeyword().trim().toLowerCase() + "%";
                Predicate repoPred = cb.like(cb.lower(root.get("repository")), kw);
                Predicate tagPred = cb.like(cb.lower(root.get("tag")), kw);
                predicates.add(cb.or(repoPred, tagPred));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<ClusterImage> page = clusterImageRepository.findAll(spec, pageable);
        List<AdminClusterImageVO> voList = clusterImageMapper.toVOList(page.getContent());

        return new PageResponse<>(voList, page.getTotalElements(), page.getTotalPages(), page.getNumber() + 1);
    }

    @Override
    public AdminClusterImageDetailVO getImageDetail(Long id) {
        ClusterImage image = clusterImageRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "镜像记录不存在"));

        String fullImageName = buildFullImageName(image);
        String publicHost = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_PUBLIC_HOST, "127.0.0.1");
        String chosenNode = StringUtils.hasText(publicHost) ? publicHost.trim() : "127.0.0.1";

        // 1. 查询 Docker Inspect 深度配置 (优先本地缓存/直接全名/digest)
        JSONObject inspectJson = queryDockerInspect(chosenNode, fullImageName);
        if (inspectJson == null && StringUtils.hasText(image.getImageId())) {
            inspectJson = queryDockerInspect(chosenNode, image.getImageId());
        }
        if (inspectJson == null) {
            String regTag = dockerRegistryClient.getPublicRegistryHost() + "/" + fullImageName;
            inspectJson = queryDockerInspect(chosenNode, regTag);
        }

        // 2. 查询 Docker History / Layer 分层信息
        List<DockerImageLayerVO> layers = queryDockerHistory(chosenNode, fullImageName);
        if (layers.isEmpty() && StringUtils.hasText(image.getImageId())) {
            layers = queryDockerHistory(chosenNode, image.getImageId());
        }
        if (layers.isEmpty()) {
            String regTag = dockerRegistryClient.getPublicRegistryHost() + "/" + fullImageName;
            layers = queryDockerHistory(chosenNode, regTag);
        }

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

        return clusterImageMapper.toDetailVO(
                image,
                fullImageName,
                os,
                architecture,
                dockerVersion,
                author,
                workingDir,
                user,
                entrypoint,
                cmd,
                env,
                exposedPorts,
                labels,
                layers
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminAsyncTaskSubmitVO pullImage(AdminImagePullRequestDTO request) {
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

        String masterIp = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_PUBLIC_HOST, "127.0.0.1");

        // 预先创建或更新数据库状态为 PULLING，便于管理员前端列表即时看到拉取中状态
        Optional<ClusterImage> existingOpt = clusterImageRepository.findByRepositoryAndTag(repository, tag);
        ClusterImage image;
        if (existingOpt.isPresent()) {
            image = existingOpt.get();
            image.setStatus("PULLING");
            image.setUpdatedAt(Instant.now());
        } else {
            image = ClusterImage.builder()
                    .repository(repository)
                    .tag(tag)
                    .status("PULLING")
                    .inRegistry(false)
                    .sizeBytes(0L)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
        }
        ClusterImage savedImage = clusterImageRepository.save(image);
        Long imageId = savedImage.getId();

        String taskName = String.format("镜像拉取入库: %s:%s", repository, tag);
        log.info("[AdminImageService] 正在提交镜像拉取入库异步任务: {}:{}", repository, tag);

        return adminAsyncTaskService.submitTask(
                AsyncTaskType.IMAGE_PULL,
                taskName,
                null,
                "ADMIN",
                request,
                context -> {
                    boolean needAuth = StringUtils.hasText(request.getUsername()) && StringUtils.hasText(request.getPassword());
                    String targetAuthServer = needAuth ? resolveAuthServer(rawImage, request.getAuthServer()) : "";
                    try {
                        context.updateProgress(10, "正在连接主控节点并准备拉取源镜像: " + rawImage + "...");
                        if (context.isCancelled()) return;

                        if (needAuth) {
                            context.updateProgress(15, "正在主控节点验证并登录镜像源 (" + targetAuthServer + ")...");
                            executeDockerLogin(masterIp, targetAuthServer, request.getUsername(), request.getPassword());
                        }

                        context.updateProgress(30, "正在主控节点执行 Docker Pull 拉取源镜像: " + rawImage + "...");
                        executeDockerPull(masterIp, rawImage, 600, context);
                        if (context.isCancelled()) return;

                        long sizeBytes = 0L;
                        String digest = "";
                        if (!dockerRegistryClient.isAvailable()) {
                            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "中心私有镜像仓库未就绪或无法连通，无法执行镜像入库");
                        }

                        context.updateProgress(60, "源镜像拉取成功，正在打标并推送到中心私有 Registry...");
                        String publicRegistryHost = dockerRegistryClient.getPublicRegistryHost();
                        String registryTag = publicRegistryHost + "/" + repository + ":" + tag;
                        log.info("[AdminImageService] 正在将镜像打标并推送到中心私有 Registry: {}...", registryTag);
                        tagDockerImageOnNode(masterIp, rawImage, registryTag);
                        String pushErr = executeDockerPushWithOutput(masterIp, registryTag);
                        if (StringUtils.hasText(pushErr)) {
                            log.error("[AdminImageService] 推送镜像至中心私有 Registry 失败: {}", pushErr);
                            String hint = pushErr.toLowerCase().contains("http") ? " (请检查宿主机 Docker 守护进程 /etc/docker/daemon.json 是否已配置 insecure-registries: [\"" + publicRegistryHost + "\"] 并重启 Docker)" : "";
                            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "推送镜像至中心私有 Registry 失败: " + pushErr + hint);
                        }

                        context.updateProgress(80, "正在获取 Registry Manifest 与镜像分层元数据...");
                        DockerRegistryClient.RegistryManifestInfo manifest = dockerRegistryClient.getManifest(repository, tag);
                        if (manifest != null && manifest.sizeBytes() > 0) {
                            sizeBytes = manifest.sizeBytes();
                            digest = manifest.digest();
                        } else {
                            JSONObject inspect = queryDockerInspect(masterIp, registryTag);
                            if (inspect == null) {
                                inspect = queryDockerInspect(masterIp, rawImage);
                            }
                            if (inspect != null) {
                                sizeBytes = inspect.getLong("Size", 0L);
                                if (!StringUtils.hasText(digest)) {
                                    digest = inspect.getStr("Id", "");
                                }
                            }
                        }

                        if (context.isCancelled()) return;

                        context.updateProgress(90, "正在同步更新数据库镜像元数据与集群拓扑...");
                        ClusterImage latest = clusterImageRepository.findById(imageId).orElse(null);
                        if (latest != null) {
                            latest.setInRegistry(true);
                            latest.setStatus("READY");
                            if (sizeBytes > 0) {
                                latest.setSizeBytes(sizeBytes);
                            }
                            if (StringUtils.hasText(digest)) {
                                latest.setImageId(digest);
                            }
                            latest.setUpdatedAt(Instant.now());
                            clusterImageRepository.save(latest);
                        }

                        updateNodeImageCounts();
                        context.updateProgress(100, "镜像拉取与中心入库已顺利完成: " + repository + ":" + tag);
                        log.info("[AdminImageService] 异步镜像拉取与中心仓库推送任务已完成: {}:{}", repository, tag);
                    } catch (Exception ex) {
                        log.error("[AdminImageService] 异步拉取镜像入库失败: {}", ex.getMessage(), ex);
                        ClusterImage latest = clusterImageRepository.findById(imageId).orElse(null);
                        if (latest != null) {
                            latest.setStatus("FAILED");
                            latest.setUpdatedAt(Instant.now());
                            clusterImageRepository.save(latest);
                        }
                        if (ex instanceof BusinessException) {
                            throw ex;
                        }
                        throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "镜像拉取入库失败: " + ex.getMessage());
                    } finally {
                        if (needAuth) {
                            executeDockerLogoutQuietly(masterIp, targetAuthServer);
                        }
                    }
                }
        );
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

        // 阶段 1：平台实例依赖校验 (若有赛题或正在运行的容器关联则拒绝删除)
        checkPlatformContainerDependencies(image, null);

        // 阶段 2：从中心私有 Registry 物理删除 Manifest
        if (Boolean.TRUE.equals(image.getInRegistry())) {
            try {
                if (dockerRegistryClient.isAvailable()) {
                    dockerRegistryClient.deleteManifestByTag(image.getRepository(), image.getTag());
                    log.info("[AdminImageService] 镜像已从中心 Registry 物理删除: {}:{}", image.getRepository(), image.getTag());
                }
            } catch (Exception e) {
                log.warn("[AdminImageService] 从中心 Registry 删除镜像版本异常: {}", e.getMessage());
            }
        }

        // 阶段 3：清理本地 Docker 引擎缓存标签
        tryExecuteDockerRmiQuietly("127.0.0.1", fullImageName);
        tryExecuteDockerRmiQuietly("127.0.0.1", dockerRegistryClient.getPublicRegistryHost() + "/" + fullImageName);

        // 阶段 4：从数据库彻底清除元数据
        clusterImageRepository.delete(image);
        log.info("[AdminImageService] 镜像已物理清除，删除数据库元数据: {}", fullImageName);

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
        List<ClusterImage> registryImages = clusterImageRepository.findByInRegistryTrue();
        if (registryImages.isEmpty()) {
            return;
        }

        int migrated = 0;
        for (ClusterImage img : registryImages) {
            triggerDockerPullOnNode(targetIp, buildFullImageName(img));
            migrated++;
        }

        workerNode.setImageCount(registryImages.size());
        clusterNodeRepository.save(workerNode);

        log.info("[AdminImageService] 自动镜像迁移完成: 已将中心托管的 {} 个资源镜像成功同步至节点: {} ({})",
                migrated, workerNode.getNodeName(), targetIp);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeNodeFromImages(String nodeIp, boolean onlyChallenge) {
        if (!StringUtils.hasText(nodeIp)) {
            return;
        }
        String cleanIp = nodeIp.trim();
        updateNodeImageCounts();
        log.info("[AdminImageService] 已成功从镜像管理中剔除节点 [{}] 的镜像拓扑关联 (onlyChallenge={})",
                cleanIp, onlyChallenge);
    }

    /**
     * 程序启动与主动触发时：
     * 1. 扫描中心私有镜像仓库 (Docker Registry V2) 的 Repositories 与 Tags；
     * 2. 查询各镜像 Manifest，提取 Digest 与体积，保存/更新为 Registry 托管镜像；
     * 3. 核心校验对账：彻底清理 MySQL 中未在 Registry 中托管或已在仓库中删除的历史与宿主机孤儿镜像。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void loadAndSyncClusterImages() {
        log.info("[AdminImageService] 开始执行 Docker 中心私有 Registry 镜像动态加载与元数据核验...");

        boolean registryReachable = dockerRegistryClient.isAvailable();
        if (!registryReachable) {
            log.warn("[AdminImageService] 中心私有镜像仓库未就绪或无法连通，跳过镜像扫描与清理以保护已有镜像记录");
            return;
        }

        Set<String> registryDiscoveredKeys = new HashSet<>();
        log.info("[AdminImageService] 正在通过 Registry API 扫描中心私有镜像仓库 catalog...");
        try {
            List<String> repos = dockerRegistryClient.listRepositories();
            for (String repo : repos) {
                List<String> tags = dockerRegistryClient.listTags(repo);
                for (String tag : tags) {
                    String key = repo + ":" + tag;
                    registryDiscoveredKeys.add(key);

                    DockerRegistryClient.RegistryManifestInfo manifest = dockerRegistryClient.getManifest(repo, tag);
                    long sizeBytes = manifest != null ? manifest.sizeBytes() : 0L;
                    String digest = manifest != null ? manifest.digest() : "";

                    if (sizeBytes <= 0) {
                        String masterIp = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_PUBLIC_HOST, "127.0.0.1");
                        String publicRegistryHost = dockerRegistryClient.getPublicRegistryHost();
                        JSONObject inspect = queryDockerInspect(masterIp, publicRegistryHost + "/" + repo + ":" + tag);
                        if (inspect == null) {
                            inspect = queryDockerInspect(masterIp, repo + ":" + tag);
                        }
                        if (inspect != null) {
                            sizeBytes = inspect.getLong("Size", 0L);
                            if (!StringUtils.hasText(digest)) {
                                digest = inspect.getStr("Id", "");
                            }
                        }
                    }

                    Optional<ClusterImage> existingOpt = clusterImageRepository.findByRepositoryAndTag(repo, tag);
                    if (existingOpt.isPresent()) {
                        ClusterImage img = existingOpt.get();
                        img.setInRegistry(true);
                        img.setStatus("READY");
                        if (sizeBytes > 0) {
                            img.setSizeBytes(sizeBytes);
                        }
                        if (StringUtils.hasText(digest)) {
                            img.setImageId(digest);
                        }
                        img.setUpdatedAt(Instant.now());
                        clusterImageRepository.save(img);
                    } else {
                        ClusterImage img = ClusterImage.builder()
                                .repository(repo)
                                .tag(tag)
                                .imageId(digest)
                                .sizeBytes(sizeBytes)
                                .inRegistry(true)
                                .status("READY")
                                .createdAt(Instant.now())
                                .updatedAt(Instant.now())
                                .build();
                        clusterImageRepository.save(img);
                    }
                }
            }
            log.info("[AdminImageService] 中心 Registry 扫描完成，共索引到 {} 个镜像版本", registryDiscoveredKeys.size());
        } catch (Exception e) {
            log.warn("[AdminImageService] 扫描中心 Registry 出现异常: {}", e.getMessage());
        }

        if (registryDiscoveredKeys.isEmpty()) {
            log.warn("[AdminImageService] 中心 Registry 未索引到任何镜像版本，跳过孤儿镜像清理以保护已有镜像元数据");
            updateNodeImageCounts();
            return;
        }

        log.info("[AdminImageService] 正在比对核验 MySQL 镜像记录与中心 Registry 现状...");
        List<ClusterImage> allDbImages = clusterImageRepository.findAll();
        int deletedCount = 0;
        String masterIp = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_PUBLIC_HOST, "127.0.0.1");
        String publicRegistryHost = dockerRegistryClient.getPublicRegistryHost();

        for (ClusterImage dbImg : allDbImages) {
            if ("PULLING".equalsIgnoreCase(dbImg.getStatus())) {
                continue;
            }
            String key = dbImg.getRepository() + ":" + dbImg.getTag();
            boolean inRegistry = registryDiscoveredKeys.contains(key);
            if (!inRegistry || !Boolean.TRUE.equals(dbImg.getInRegistry())) {
                boolean existsLocally = queryDockerInspect(masterIp, key) != null
                        || queryDockerInspect(masterIp, publicRegistryHost + "/" + key) != null;
                if (!existsLocally) {
                    clusterImageRepository.delete(dbImg);
                    deletedCount++;
                    log.info("[AdminImageService] 清除历史宿主机/非 Registry 孤儿镜像记录: {}", key);
                }
            }
        }

        log.info("[AdminImageService] 镜像对账校验完成: 彻底清理无效宿主机/孤儿镜像 {} 个", deletedCount);
        updateNodeImageCounts();
        log.info("[AdminImageService] 中心 Registry 镜像元数据对账核验已完成");
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

        List<ClusterNode> allNodes = clusterNodeRepository.findAll();
        boolean sourceExists = allNodes.stream().anyMatch(n -> n.getIpAddress().equalsIgnoreCase(sourceIp));
        if (!sourceExists) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "源节点 " + sourceIp + " 不存在于集群中");
        }

        boolean targetExists = allNodes.stream().anyMatch(n -> n.getIpAddress().equalsIgnoreCase(targetIp));
        if (!targetExists) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "目标节点 " + targetIp + " 不存在于集群中");
        }

        if (allNodes.size() > 1) {
            boolean targetIsMaster = allNodes.stream()
                    .anyMatch(n -> "MASTER".equalsIgnoreCase(n.getRole()) && n.getIpAddress().equalsIgnoreCase(targetIp));
            if (targetIsMaster) {
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        "集群存在多个节点，主节点保留为平台控制面，禁止将运行镜像转移至主节点！");
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

                        context.updateProgress(75, "目标节点镜像拉取完成，正在更新集群镜像状态...");
                        ClusterImage latest = clusterImageRepository.findById(imageId).orElse(null);
                        if (latest != null) {
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

                            context.updateProgress(80, "目标节点镜像拉取完成，正在更新集群镜像状态...");
                            ClusterImage latest = clusterImageRepository.findById(imageId).orElse(null);
                            if (latest != null) {
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
        String chosenNode = StringUtils.hasText(publicHost) ? publicHost.trim() : "127.0.0.1";

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
    public AdminAsyncTaskSubmitVO importImage(MultipartFile file, String customRepository, String customTag) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "请选择需要上传的 Docker 镜像归档文件");
        }
        String originalFilename = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFilename)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "上传文件名无效");
        }
        String lowerName = originalFilename.trim().toLowerCase();
        if (!lowerName.endsWith(".tar") && !lowerName.endsWith(".tar.gz") && !lowerName.endsWith(".tgz")) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "仅支持导入 .tar, .tar.gz 或 .tgz 格式的 Docker 镜像归档文件");
        }
        if (!dockerRegistryClient.isAvailable()) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "中心私有镜像仓库未就绪或未启动，无法执行镜像导入");
        }

        Path tempFilePath;
        try {
            String suffix = lowerName.endsWith(".tar.gz") ? ".tar.gz" : (lowerName.endsWith(".tgz") ? ".tgz" : ".tar");
            tempFilePath = Files.createTempFile("volur-image-import-", suffix);
            file.transferTo(tempFilePath);
        } catch (IOException e) {
            log.error("[AdminImageService] 镜像上传文件落盘失败: {}", e.getMessage(), e);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "临时文件写入失败: " + e.getMessage());
        }

        String taskName = "镜像导入入库: " + originalFilename;
        String masterIp = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_PUBLIC_HOST, "127.0.0.1");

        Map<String, Object> taskParams = new HashMap<>();
        taskParams.put("filename", originalFilename);
        taskParams.put("sizeBytes", file.getSize());
        if (StringUtils.hasText(customRepository)) {
            taskParams.put("repository", customRepository.trim());
        }
        if (StringUtils.hasText(customTag)) {
            taskParams.put("tag", customTag.trim());
        }

        return adminAsyncTaskService.submitTask(
                AsyncTaskType.IMAGE_IMPORT,
                taskName,
                null,
                "ADMIN",
                taskParams,
                context -> {
                    try {
                        context.updateProgress(10, "文件已成功上传，正在准备加载 Docker 镜像: " + originalFilename + "...");
                        if (context.isCancelled()) return;

                        context.updateProgress(30, "正在主控节点 Docker 中执行镜像解压与加载 (docker load)...");
                        List<String> loadedImages = executeDockerLoadFromFile(masterIp, tempFilePath);
                        if (context.isCancelled()) return;

                        if (loadedImages.isEmpty() && !StringUtils.hasText(customRepository)) {
                            throw new BusinessException(HttpStatus.BAD_REQUEST, "未能在镜像文件中检测到有效镜像名称，请手动指定仓库名称与标签后重新导入");
                        }

                        String sourceImage = !loadedImages.isEmpty() ? loadedImages.getFirst() : "";
                        String targetRepo;
                        String targetTag;

                        if (StringUtils.hasText(customRepository)) {
                            targetRepo = customRepository.trim();
                            targetTag = StringUtils.hasText(customTag) ? customTag.trim() : "latest";
                        } else {
                            int lastColon = sourceImage.lastIndexOf(':');
                            if (lastColon > 0 && !sourceImage.substring(lastColon + 1).contains("/")) {
                                targetRepo = sourceImage.substring(0, lastColon);
                                targetTag = sourceImage.substring(lastColon + 1);
                            } else {
                                targetRepo = sourceImage;
                                targetTag = "latest";
                            }
                        }

                        context.updateProgress(65, "镜像加载完成，正在打标并推送至中心私有 Registry...");
                        String publicRegistryHost = dockerRegistryClient.getPublicRegistryHost();
                        String registryTag = publicRegistryHost + "/" + targetRepo + ":" + targetTag;

                        tagDockerImageOnNode(masterIp, StringUtils.hasText(sourceImage) ? sourceImage : targetRepo + ":" + targetTag, registryTag);
                        boolean pushSuccess = triggerDockerPushOnNode(masterIp, registryTag);
                        if (!pushSuccess) {
                            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "推送镜像至中心私有 Registry 失败: " + registryTag);
                        }
                        if (context.isCancelled()) return;

                        context.updateProgress(85, "正在获取 Registry Manifest 并同步数据库元数据...");
                        DockerRegistryClient.RegistryManifestInfo manifest = dockerRegistryClient.getManifest(targetRepo, targetTag);
                        long sizeBytes = manifest != null ? manifest.sizeBytes() : Files.size(tempFilePath);
                        String digest = manifest != null ? manifest.digest() : "";

                        Optional<ClusterImage> existingOpt = clusterImageRepository.findByRepositoryAndTag(targetRepo, targetTag);
                        ClusterImage image;
                        if (existingOpt.isPresent()) {
                            image = existingOpt.get();
                            image.setInRegistry(true);
                            image.setStatus("READY");
                            if (sizeBytes > 0) image.setSizeBytes(sizeBytes);
                            if (StringUtils.hasText(digest)) image.setImageId(digest);
                            image.setUpdatedAt(Instant.now());
                        } else {
                            image = ClusterImage.builder()
                                    .repository(targetRepo)
                                    .tag(targetTag)
                                    .imageId(digest)
                                    .sizeBytes(sizeBytes)
                                    .inRegistry(true)
                                    .status("READY")
                                    .createdAt(Instant.now())
                                    .updatedAt(Instant.now())
                                    .build();
                        }
                        clusterImageRepository.save(image);
                        updateNodeImageCounts();

                        context.updateProgress(100, "镜像导入与中心私有 Registry 入库顺利完成: " + targetRepo + ":" + targetTag);
                        log.info("[AdminImageService] 异步镜像导入任务成功完成: {}:{}", targetRepo, targetTag);
                    } catch (Exception ex) {
                        log.error("[AdminImageService] 异步导入镜像任务异常: {}", ex.getMessage(), ex);
                        throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "镜像导入入库失败: " + ex.getMessage());
                    } finally {
                        try {
                            Files.deleteIfExists(tempFilePath);
                        } catch (Exception ignored) {}
                    }
                }
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pushImageToRegistry(Long id) {
        if (id == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "镜像 ID 不能为空");
        }
        ClusterImage image = clusterImageRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "镜像记录不存在"));

        if (!dockerRegistryClient.isAvailable()) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "中心私有镜像仓库未就绪或未启动，无法推送镜像");
        }

        String fullImageName = buildFullImageName(image);
        if (!StringUtils.hasText(fullImageName)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "无法解析镜像仓库与标签");
        }

        // 挑选一个拥有此镜像物理副本的源节点
        String publicHost = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_PUBLIC_HOST, "127.0.0.1");
        String canonicalMaster = StringUtils.hasText(publicHost) ? publicHost.trim() : "127.0.0.1";

        String chosenNode = null;
        if (queryDockerInspect(canonicalMaster, fullImageName) != null) {
            chosenNode = canonicalMaster;
        } else if (!"127.0.0.1".equalsIgnoreCase(canonicalMaster) && queryDockerInspect("127.0.0.1", fullImageName) != null) {
            chosenNode = "127.0.0.1";
        } else {
            List<ClusterNode> allNodes = clusterNodeRepository.findAll();
            for (ClusterNode node : allNodes) {
                String ip = node.getIpAddress();
                if (StringUtils.hasText(ip) && queryDockerInspect(ip.trim(), fullImageName) != null) {
                    chosenNode = ip.trim();
                    break;
                }
            }
        }

        if (chosenNode == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "集群内所有在线节点均未找到镜像物理文件 [" + fullImageName + "]，请先确保至少有一个节点包含此镜像");
        }

        String publicRegistryHost = dockerRegistryClient.getPublicRegistryHost();
        String targetRegistryTag = publicRegistryHost + "/" + fullImageName;

        log.info("[AdminImageService] 正在将镜像 [{}] 从节点 [{}] 推送至中心 Registry: {}...",
                fullImageName, chosenNode, targetRegistryTag);

        // 1. 打标
        tagDockerImageOnNode(chosenNode, fullImageName, targetRegistryTag);

        // 2. 推送
        boolean pushSuccess = triggerDockerPushOnNode(chosenNode, targetRegistryTag);
        if (!pushSuccess) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "推送镜像至中心 Registry 失败，请检查节点 Docker 与 Registry 网络连通性");
        }

        // 3. 标记元数据
        image.setInRegistry(true);
        image.setUpdatedAt(Instant.now());
        clusterImageRepository.save(image);

        log.info("[AdminImageService] 镜像 [{}] 已成功推送至中心私有镜像仓库并完成元数据同步", fullImageName);
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

        JSONObject inspectTarget = queryDockerInspect(targetIp, imageName);
        if (inspectTarget != null) {
            log.info("[AdminImageService] 目标节点 [{}] Docker 引擎已实际存在镜像 [{}]，校验通过", targetIp, imageName);
            return;
        }

        String publicRegistryHost = dockerRegistryClient.getPublicRegistryHost();
        String registryImageTag = publicRegistryHost + "/" + imageName;

        JSONObject inspectRegistryTag = queryDockerInspect(targetIp, registryImageTag);
        if (inspectRegistryTag != null) {
            tagDockerImageOnNode(targetIp, registryImageTag, imageName);
            log.info("[AdminImageService] 目标节点 [{}] 已存在仓库打标镜像 [{}]，已补全标签 [{}]，校验通过", targetIp, registryImageTag, imageName);
            return;
        }

        boolean pulledFromRegistry = false;
        boolean inRegistry = (imageOpt.isPresent() && Boolean.TRUE.equals(imageOpt.get().getInRegistry()))
                || (dockerRegistryClient.isAvailable() && dockerRegistryClient.getManifest(repo, tag) != null);
        if (dockerRegistryClient.isAvailable() && !inRegistry && repo.contains("/")) {
            int firstSlash = repo.indexOf('/');
            String domainPart = repo.substring(0, firstSlash);
            if (domainPart.contains(".") || domainPart.contains(":")) {
                String subRepo = repo.substring(firstSlash + 1);
                if (dockerRegistryClient.getManifest(subRepo, tag) != null) {
                    inRegistry = true;
                    registryImageTag = publicRegistryHost + "/" + subRepo + ":" + tag;
                }
            }
        }
        if (dockerRegistryClient.isAvailable() && inRegistry) {
            log.info("[AdminImageService] 镜像 [{}] 已索引于中心 Registry，尝试通过 Registry 快速下发至节点 [{}] (源: {})...",
                    imageName, targetIp, registryImageTag);
            try {
                triggerDockerPullOnNode(targetIp, registryImageTag);
                if (queryDockerInspect(targetIp, registryImageTag) != null) {
                    tagDockerImageOnNode(targetIp, registryImageTag, imageName);
                    pulledFromRegistry = true;
                    log.info("[AdminImageService] 节点 [{}] 成功从中心 Registry 下发并打标镜像 [{}]", targetIp, imageName);
                }
            } catch (Exception e) {
                log.warn("[AdminImageService] 节点 [{}] 从中心 Registry 拉取镜像失败: {}，准备回退至节点流式复制", targetIp, e.getMessage());
            }
        }

        if (pulledFromRegistry) {
            updateNodeImageCounts();
            return;
        }

        String sourceNodeIp = null;
        List<ClusterNode> readyNodes = clusterNodeRepository.findByStatus("READY");
        String publicHost = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_PUBLIC_HOST, "127.0.0.1");

        if (!targetIp.equalsIgnoreCase(publicHost) && (queryDockerInspect(publicHost, imageName) != null || queryDockerInspect(publicHost, registryImageTag) != null)) {
            sourceNodeIp = publicHost;
        } else if (!targetIp.equals("127.0.0.1") && (queryDockerInspect("127.0.0.1", imageName) != null || queryDockerInspect("127.0.0.1", registryImageTag) != null)) {
            sourceNodeIp = "127.0.0.1";
        } else {
            for (ClusterNode node : readyNodes) {
                String ip = node.getIpAddress();
                if (StringUtils.hasText(ip) && !ip.equalsIgnoreCase(targetIp)) {
                    if (queryDockerInspect(ip.trim(), imageName) != null || queryDockerInspect(ip.trim(), registryImageTag) != null) {
                        sourceNodeIp = ip.trim();
                        break;
                    }
                }
            }
        }

        if (sourceNodeIp == null) {
            log.warn("[AdminImageService] 镜像 [{}] 在集群内其它在线节点均不存在，尝试直接在目标节点 [{}] 执行 pull", imageName, targetIp);
            triggerDockerPullOnNode(targetIp, imageName);
            return;
        }

        log.info("[AdminImageService] 检测到目标节点 [{}] 缺少镜像 [{}]，正在从节点 [{}] 复制镜像副本...",
                targetIp, imageName, sourceNodeIp);
        replicateImageToNode(sourceNodeIp, targetIp, imageName);

        if (imageOpt.isPresent()) {
            ClusterImage img = imageOpt.get();
            img.setUpdatedAt(Instant.now());
            clusterImageRepository.save(img);
            updateNodeImageCounts();
        } else {
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
        String publicRegistryHost = dockerRegistryClient.getPublicRegistryHost();
        String registryImageTag = publicRegistryHost + "/" + safeImageName;

        String actualSourceTag = safeImageName;
        if (queryDockerInspect(sourceNodeIp, safeImageName) == null && queryDockerInspect(sourceNodeIp, registryImageTag) != null) {
            actualSourceTag = registryImageTag;
        }

        log.info("[AdminImageService] 开始执行跨节点镜像复制: {} (源实际标签: {}, {} -> {})", safeImageName, actualSourceTag, sourceNodeIp, targetNodeIp);

        List<String> saveCmd = buildDockerBaseCommand(sourceNodeIp);
        saveCmd.add("save");
        saveCmd.add(actualSourceTag);

        List<String> loadCmd = buildDockerBaseCommand(targetNodeIp);
        loadCmd.add("load");

        Process saveProcess = null;
        Process loadProcess = null;
        StringBuilder saveErr = new StringBuilder();
        StringBuilder loadErr = new StringBuilder();
        Thread saveErrThread = null;
        Thread loadErrThread = null;

        try {
            ProcessBuilder savePb = new ProcessBuilder(saveCmd);
            ProcessBuilder loadPb = new ProcessBuilder(loadCmd);

            saveProcess = savePb.start();
            loadProcess = loadPb.start();

            Process finalSave = saveProcess;
            Process finalLoad = loadProcess;

            saveErrThread = new Thread(() -> {
                try (BufferedReader r = new BufferedReader(new InputStreamReader(finalSave.getErrorStream(), StandardCharsets.UTF_8))) {
                    String l;
                    while ((l = r.readLine()) != null) saveErr.append(l).append("\n");
                } catch (Exception ignored) {}
            });
            loadErrThread = new Thread(() -> {
                try (BufferedReader r = new BufferedReader(new InputStreamReader(finalLoad.getErrorStream(), StandardCharsets.UTF_8))) {
                    String l;
                    while ((l = r.readLine()) != null) loadErr.append(l).append("\n");
                } catch (Exception ignored) {}
            });

            saveErrThread.setDaemon(true);
            loadErrThread.setDaemon(true);
            saveErrThread.start();
            loadErrThread.start();

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
                String errorDetails = String.format("源节点 save(exit=%s, err=%s), 目标节点 load(exit=%s, err=%s)",
                        saveFinished ? saveProcess.exitValue() : "TIMEOUT", saveErr.toString().trim(),
                        loadFinished ? loadProcess.exitValue() : "TIMEOUT", loadErr.toString().trim());
                log.error("[AdminImageService] 跨节点镜像复制失败: {}", errorDetails);
                throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "跨节点复制镜像失败: " + errorDetails);
            }

            if (!actualSourceTag.equals(safeImageName)) {
                tagDockerImageOnNode(targetNodeIp, actualSourceTag, safeImageName);
            }

            log.info("[AdminImageService] 跨节点镜像复制成功: {} 成功加载至目标节点 {}", safeImageName, targetNodeIp);
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            if (saveErrThread != null) {
                try { saveErrThread.join(1000); } catch (Exception ignored) {}
            }
            if (loadErrThread != null) {
                try { loadErrThread.join(1000); } catch (Exception ignored) {}
            }
            String detail = "";
            String se = saveErr.toString().trim();
            String le = loadErr.toString().trim();
            if (StringUtils.hasText(se) || StringUtils.hasText(le)) {
                detail = String.format(" (源节点输出: [%s], 目标节点输出: [%s])", se, le);
            }
            log.error("[AdminImageService] 跨节点镜像复制异常: {}{}", e.getMessage(), detail, e);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "跨节点复制镜像异常: " + e.getMessage() + detail);
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

                        DockerImageLayerVO layerVO = clusterImageMapper.toLayerVO(
                                totalLayers - i,
                                layerId,
                                instruction,
                                rawCreatedBy,
                                sizeBytes,
                                createdAt,
                                ""
                        );
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
        } else {
            // 优先通过 Unix Domain Socket 原生通信直连 Docker 守护进程 API
            String socketPath = (StringUtils.hasText(swarmApiUrl) && swarmApiUrl.startsWith("unix://"))
                    ? swarmApiUrl.substring(7)
                    : (StringUtils.hasText(swarmApiUrl) ? swarmApiUrl : "/var/run/docker.sock");
            list = queryDockerUnixSocket(socketPath);
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

    private List<DiscoveredDockerImage> queryDockerUnixSocket(String socketPath) {
        File socketFile = new File(socketPath);
        if (!socketFile.exists()) {
            return Collections.emptyList();
        }
        List<DiscoveredDockerImage> list = new ArrayList<>();
        try {
            UnixDomainSocketAddress address = UnixDomainSocketAddress.of(socketFile.toPath());
            try (SocketChannel channel = SocketChannel.open(address)) {
                channel.configureBlocking(true);
                String request = "GET /images/json HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n";
                channel.write(ByteBuffer.wrap(request.getBytes(StandardCharsets.US_ASCII)));

                ByteBuffer buffer = ByteBuffer.allocate(64 * 1024);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                while (channel.read(buffer) > 0) {
                    buffer.flip();
                    byte[] data = new byte[buffer.remaining()];
                    buffer.get(data);
                    baos.write(data);
                    buffer.clear();
                }

                String responseStr = baos.toString(StandardCharsets.UTF_8);
                int firstBracket = responseStr.indexOf('[');
                int lastBracket = responseStr.lastIndexOf(']');
                if (firstBracket >= 0 && lastBracket > firstBracket) {
                    String jsonArrayStr = responseStr.substring(firstBracket, lastBracket + 1);
                    JSONArray array = JSONUtil.parseArray(jsonArrayStr);
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
            }
        } catch (Exception e) {
            log.debug("[AdminImageService] Unix Domain Socket 直连 Docker 镜像查询提示: {}", e.getMessage());
        }
        return list;
    }

    private boolean isLocalDockerReachable() {
        String swarmApiUrl = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_DOCKER_SWARM_API_URL, "unix:///var/run/docker.sock");
        if (StringUtils.hasText(swarmApiUrl) && (swarmApiUrl.startsWith("tcp://") || swarmApiUrl.startsWith("http://"))) {
            String httpUrl = swarmApiUrl.replace("tcp://", "http://");
            try {
                HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create(httpUrl + "/_ping")).timeout(Duration.ofSeconds(2)).GET().build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                return resp.statusCode() == 200;
            } catch (Exception e) {
                return false;
            }
        }

        String socketPath = (StringUtils.hasText(swarmApiUrl) && swarmApiUrl.startsWith("unix://"))
                ? swarmApiUrl.substring(7)
                : (StringUtils.hasText(swarmApiUrl) ? swarmApiUrl : "/var/run/docker.sock");
        File socketFile = new File(socketPath);
        if (socketFile.exists()) {
            try {
                UnixDomainSocketAddress address = UnixDomainSocketAddress.of(socketFile.toPath());
                try (SocketChannel channel = SocketChannel.open(address)) {
                    return channel.isConnected();
                }
            } catch (Exception e) {
                // fall through
            }
        }

        try {
            Process p = new ProcessBuilder("docker", "version").start();
            return p.waitFor(3, TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
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
            log.warn("[AdminImageService] 调度 Docker CLI 获取镜像列表异常 (请确认容器内已安装 docker-cli 且已挂载 docker.sock): {}", e.getMessage());
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
            image.setStatus("PULLED");
            image.setUpdatedAt(Instant.now());
        } else {
            image = clusterImageMapper.toEntity(img);
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

    private String resolveAuthServer(String image, String explicitAuthServer) {
        if (StringUtils.hasText(explicitAuthServer)) {
            return explicitAuthServer.trim();
        }
        if (!StringUtils.hasText(image)) {
            return "docker.io";
        }
        String cleanImage = image.trim();
        int slashIdx = cleanImage.indexOf('/');
        if (slashIdx != -1) {
            String firstPart = cleanImage.substring(0, slashIdx);
            if (firstPart.contains(".") || firstPart.contains(":") || "localhost".equalsIgnoreCase(firstPart)) {
                return firstPart;
            }
        }
        return "docker.io";
    }

    private void executeDockerLogin(String nodeIp, String authServer, String username, String password) {
        List<String> command = buildDockerBaseCommand(nodeIp);
        command.add("login");
        command.add(authServer);
        command.add("-u");
        command.add(username);
        command.add("--password-stdin");

        Process process = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            process = pb.start();

            StringBuilder stderr = new StringBuilder();
            StringBuilder stdout = new StringBuilder();
            Thread errThread = drainStreamAsync(process.getErrorStream(), stderr);
            Thread outThread = drainStreamAsync(process.getInputStream(), stdout);

            try (OutputStream os = process.getOutputStream()) {
                os.write((password + "\n").getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            errThread.join(2000);
            outThread.join(2000);

            if (!finished || process.exitValue() != 0) {
                String err = stderr.toString().trim();
                if (!StringUtils.hasText(err)) {
                    err = stdout.toString().trim();
                }
                log.error("[AdminImageService] Docker login 失败: server={}, exitCode={}, err={}",
                        authServer, finished ? process.exitValue() : "TIMEOUT", err);
                throw new BusinessException(HttpStatus.UNAUTHORIZED, "Docker Registry 登录认证失败 (" + authServer + "): " + err);
            }
            log.info("[AdminImageService] 节点 [{}] 成功登录私有镜像源: {}", nodeIp, authServer);
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            log.error("[AdminImageService] 执行 docker login 异常: server={}, err={}", authServer, e.getMessage(), e);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "执行镜像源登录异常: " + e.getMessage());
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    private void executeDockerLogoutQuietly(String nodeIp, String authServer) {
        Process process = null;
        try {
            List<String> command = buildDockerBaseCommand(nodeIp);
            command.add("logout");
            command.add(authServer);

            ProcessBuilder pb = new ProcessBuilder(command);
            process = pb.start();

            StringBuilder stderr = new StringBuilder();
            StringBuilder stdout = new StringBuilder();
            Thread errThread = drainStreamAsync(process.getErrorStream(), stderr);
            Thread outThread = drainStreamAsync(process.getInputStream(), stdout);

            boolean finished = process.waitFor(15, TimeUnit.SECONDS);
            errThread.join(1000);
            outThread.join(1000);

            if (finished && process.exitValue() == 0) {
                log.info("[AdminImageService] 节点 [{}] 成功登出私有镜像源: {}", nodeIp, authServer);
            } else {
                log.warn("[AdminImageService] 节点 [{}] 登出镜像源返回异常: server={}, exitCode={}",
                        nodeIp, authServer, finished ? process.exitValue() : "TIMEOUT");
            }
        } catch (Exception e) {
            log.warn("[AdminImageService] 登出镜像源 [{}] 发生异常 (已忽略): {}", authServer, e.getMessage());
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    private void executeDockerPull(String nodeIp, String imageName, int timeoutSeconds, AsyncTaskContext context) {
        List<String> command = buildDockerBaseCommand(nodeIp);
        command.add("pull");
        command.add(imageName);

        Process process = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            process = pb.start();

            StringBuilder stderr = new StringBuilder();
            StringBuilder stdout = new StringBuilder();
            Thread errThread = drainStreamAsync(process.getErrorStream(), stderr);
            Thread outThread = drainStreamAsync(process.getInputStream(), stdout);

            long startTime = System.currentTimeMillis();
            long maxDurationMs = timeoutSeconds * 1000L;

            while (process.isAlive()) {
                if (context != null && context.isCancelled()) {
                    process.destroyForcibly();
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "镜像拉取任务已被取消");
                }
                if (System.currentTimeMillis() - startTime > maxDurationMs) {
                    process.destroyForcibly();
                    throw new BusinessException(HttpStatus.GATEWAY_TIMEOUT, "拉取镜像超时 (" + timeoutSeconds + " 秒)");
                }
                try {
                    process.waitFor(1, TimeUnit.SECONDS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    process.destroyForcibly();
                    throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "镜像拉取线程被中断");
                }
            }

            errThread.join(3000);
            outThread.join(3000);

            if (process.exitValue() != 0) {
                String err = stderr.toString().trim();
                if (!StringUtils.hasText(err)) {
                    err = stdout.toString().trim();
                }
                log.error("[AdminImageService] Docker pull 失败: image={}, exitCode={}, err={}",
                        imageName, process.exitValue(), err);
                throw new BusinessException(HttpStatus.BAD_GATEWAY, "拉取镜像失败 (" + imageName + "): " + err);
            }
            log.info("[AdminImageService] 节点 [{}] 成功拉取源镜像: {}", nodeIp, imageName);
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            log.error("[AdminImageService] 执行 docker pull 异常: image={}, err={}", imageName, e.getMessage(), e);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "执行镜像拉取异常: " + e.getMessage());
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    private Thread drainStreamAsync(InputStream inputStream, StringBuilder targetBuffer) {
        Thread thread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    targetBuffer.append(line).append("\n");
                }
            } catch (Exception ignored) {
            }
        });
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private List<String> executeDockerLoadFromFile(String nodeIp, Path filePath) {
        List<String> loadedImages = new ArrayList<>();
        List<String> command = buildDockerBaseCommand(nodeIp);
        command.add("load");

        Process process = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            process = pb.start();

            Process currentProcess = process;
            StringBuilder stderr = new StringBuilder();
            Thread errThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(currentProcess.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stderr.append(line).append("\n");
                    }
                } catch (Exception ignored) {}
            });
            errThread.setDaemon(true);
            errThread.start();

            try (InputStream is = Files.newInputStream(filePath);
                 OutputStream os = process.getOutputStream()) {
                byte[] buffer = new byte[65536];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }

            try (BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = stdoutReader.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("Loaded image:")) {
                        String imgName = line.substring("Loaded image:".length()).trim();
                        if (StringUtils.hasText(imgName)) {
                            loadedImages.add(imgName);
                        }
                    } else if (line.startsWith("Loaded image ID:")) {
                        String imgId = line.substring("Loaded image ID:".length()).trim();
                        if (StringUtils.hasText(imgId)) {
                            loadedImages.add(imgId);
                        }
                    }
                }
            }

            boolean finished = process.waitFor(600, TimeUnit.SECONDS);
            errThread.join(3000);

            if (!finished || process.exitValue() != 0) {
                log.error("[AdminImageService] docker load 执行失败: exitCode={}, stderr={}",
                        finished ? process.exitValue() : "TIMEOUT", stderr);
                throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "Docker 加载镜像归档失败: " + stderr);
            }
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            log.error("[AdminImageService] 执行 docker load 异常: {}", e.getMessage(), e);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "读取与加载镜像归档异常: " + e.getMessage());
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
        return loadedImages;
    }

    private void tagDockerImageOnNode(String nodeIp, String sourceImage, String targetImage) {
        try {
            List<String> command = buildDockerBaseCommand(nodeIp);
            command.add("tag");
            command.add(sourceImage);
            command.add(targetImage);

            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();
            boolean finished = process.waitFor(15, TimeUnit.SECONDS);
            if (finished && process.exitValue() == 0) {
                log.info("[AdminImageService] 节点 [{}] Docker tag 成功: {} -> {}", nodeIp, sourceImage, targetImage);
            } else {
                log.warn("[AdminImageService] 节点 [{}] Docker tag 异常: exitCode={}", nodeIp, finished ? process.exitValue() : "TIMEOUT");
            }
        } catch (Exception e) {
            log.warn("[AdminImageService] 节点 [{}] Docker tag 发生异常: {}", nodeIp, e.getMessage());
        }
    }

    private String executeDockerPushWithOutput(String nodeIp, String registryImageName) {
        StringBuilder output = new StringBuilder();
        try {
            List<String> command = buildDockerBaseCommand(nodeIp);
            command.add("push");
            command.add(registryImageName);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            boolean finished = process.waitFor(300, TimeUnit.SECONDS);
            if (finished && process.exitValue() == 0) {
                log.info("[AdminImageService] 节点 [{}] Docker push 成功: {}", nodeIp, registryImageName);
                return null;
            } else {
                String err = output.toString().trim();
                log.error("[AdminImageService] 节点 [{}] Docker push 失败: exitCode={}, output={}", nodeIp, finished ? process.exitValue() : "TIMEOUT", err);
                return StringUtils.hasText(err) ? err : ("exitCode=" + (finished ? process.exitValue() : "TIMEOUT"));
            }
        } catch (Exception e) {
            log.error("[AdminImageService] 节点 [{}] Docker push 发生异常: {}", nodeIp, e.getMessage());
            return e.getMessage();
        }
    }

    private boolean triggerDockerPushOnNode(String nodeIp, String registryImageName) {
        return executeDockerPushWithOutput(nodeIp, registryImageName) == null;
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

    private String buildFullImageName(ClusterImage image) {
        if (!StringUtils.hasText(image.getRepository())) return "";
        return StringUtils.hasText(image.getTag()) ? image.getRepository() + ":" + image.getTag() : image.getRepository() + ":latest";
    }

    @Override
    public void updateNodeImageCounts() {
        long inRegistryCount = clusterImageRepository.countByInRegistryTrue();
        List<ClusterNode> allNodes = clusterNodeRepository.findAll();
        for (ClusterNode node : allNodes) {
            node.setImageCount((int) inRegistryCount);
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

}
