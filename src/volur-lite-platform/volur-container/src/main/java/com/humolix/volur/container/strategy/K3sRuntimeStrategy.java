package com.humolix.volur.container.strategy;

import com.humolix.volur.challenge.domain.entity.Challenge;
import com.humolix.volur.challenge.domain.entity.Containerizable;
import com.humolix.volur.container.domain.dto.*;
import com.humolix.volur.container.domain.enums.InstanceStartupStatus;
import com.humolix.volur.container.domain.vo.AdminContainerTerminalInfoVO;
import com.humolix.volur.container.client.DockerRegistryClient;
import com.humolix.volur.container.repository.ClusterImageRepository;
import com.humolix.volur.container.repository.ClusterNodeRepository;
import com.humolix.volur.container.util.ContainerPortAllocator;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.system.constant.SystemSettingKeys;
import com.humolix.volur.system.service.SystemSettingService;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicy;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class K3sRuntimeStrategy implements ContainerRuntimeStrategy {

    private final SystemSettingService systemSettingService;
    private final ClusterNodeRepository clusterNodeRepository;
    private final ContainerPortAllocator containerPortAllocator;
    private final DockerRegistryClient dockerRegistryClient;
    private final ClusterImageRepository clusterImageRepository;

    private static final String DEFAULT_NAMESPACE = "volur-challenges";
    private static final String DEFAULT_PUBLIC_HOST = "127.0.0.1";
    private static final String DEFAULT_KUBECONFIG_PATH = "/etc/rancher/k3s/k3s.yaml";

    private KubernetesClient k8sClient;

    private String resolveNamespace() {
        return systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_K3S_NAMESPACE, DEFAULT_NAMESPACE);
    }

    private String resolveKubeconfigPath() {
        return systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_K3S_KUBECONFIG_PATH, DEFAULT_KUBECONFIG_PATH);
    }

    private String resolvePublicHost() {
        return systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_PUBLIC_HOST, DEFAULT_PUBLIC_HOST);
    }

    private String resolveEffectiveImageName(String rawImageName) {
        if (!StringUtils.hasText(rawImageName)) {
            return rawImageName;
        }
        String clean = rawImageName.trim();
        String publicRegistryHost = dockerRegistryClient.getPublicRegistryHost();

        if (clean.startsWith(publicRegistryHost + "/")) {
            return clean;
        }

        String repo = clean;
        String tag = "latest";
        int colonIdx = clean.lastIndexOf(':');
        if (colonIdx > 0 && !clean.substring(colonIdx + 1).contains("/")) {
            repo = clean.substring(0, colonIdx);
            tag = clean.substring(colonIdx + 1);
        }

        if (dockerRegistryClient.isAvailable()) {
            if (isImageInLocalRegistry(repo, tag)) {
                return publicRegistryHost + "/" + repo + ":" + tag;
            }

            int firstSlash = repo.indexOf('/');
            if (firstSlash > 0) {
                String domainPart = repo.substring(0, firstSlash);
                if (domainPart.contains(".") || domainPart.contains(":")) {
                    String subRepo = repo.substring(firstSlash + 1);
                    if (isImageInLocalRegistry(subRepo, tag)) {
                        return publicRegistryHost + "/" + subRepo + ":" + tag;
                    }
                }
            }
        }

        return clean;
    }

    private boolean isImageInLocalRegistry(String repo, String tag) {
        boolean inDb = clusterImageRepository.findByRepositoryAndTag(repo, tag)
                .map(img -> Boolean.TRUE.equals(img.getInRegistry()))
                .orElse(false);
        if (inDb) {
            return true;
        }
        return dockerRegistryClient.getManifest(repo, tag) != null;
    }

    public synchronized void resetClient() {
        if (k8sClient != null) {
            try {
                k8sClient.close();
            } catch (Exception ignored) {}
            k8sClient = null;
        }
    }

    public synchronized KubernetesClient getClient() {
        if (k8sClient != null) {
            return k8sClient;
        }

        String kubeconfigPath = resolveKubeconfigPath();
        String namespace = resolveNamespace();

        File kubeconfigFile = new File(kubeconfigPath);
        if (!kubeconfigFile.exists() || !kubeconfigFile.isFile()) {
            log.warn("[K3s] kubeconfig 配置文件不存在: {}，跳过客户端初始化", kubeconfigPath);
            return null;
        }

        try {
            String kubeconfigContent = Files.readString(Path.of(kubeconfigPath));
            Config config = buildEffectiveConfig(kubeconfigContent);
            if (config == null) {
                log.warn("[K3s] 未能根据 kubeconfig 解析出可达的 API 端点，跳过客户端初始化");
                return null;
            }

            k8sClient = new KubernetesClientBuilder().withConfig(config).build();
            // Test connection
            k8sClient.namespaces().list();

            // Ensure namespace and network isolation policy exist
            ensureNamespaceAndNetworkPolicy(k8sClient, namespace);

            log.info("[K3s] 成功连接至 K3s 集群. Namespace: {}, MasterUrl: {}", namespace, config.getMasterUrl());
        } catch (Exception e) {
            log.error("[K3s] K3s 集群连接建立失败: {}", e.getMessage());
            resetClient();
        }

        return k8sClient;
    }

    /**
     * 解析 kubeconfig 中的 Server URL 并针对 Docker 容器环境探测宿主机可达地址
     */
    private Config buildEffectiveConfig(String kubeconfigContent) {
        if (!StringUtils.hasText(kubeconfigContent)) {
            return null;
        }

        Config baseConfig = Config.fromKubeconfig(kubeconfigContent);
        String rawMasterUrl = baseConfig.getMasterUrl();
        if (!StringUtils.hasText(rawMasterUrl)) {
            log.warn("[K3s] kubeconfig 中未配置集群 Server URL");
            return null;
        }

        URI uri;
        try {
            uri = URI.create(rawMasterUrl.contains("://") ? rawMasterUrl : ("https://" + rawMasterUrl));
        } catch (Exception e) {
            log.error("[K3s] 解析 kubeconfig Server URL 异常: {}", rawMasterUrl, e);
            return null;
        }

        String rawHost = uri.getHost() != null ? uri.getHost() : "127.0.0.1";
        int port = uri.getPort() != -1 ? uri.getPort() : 6443;

        String reachableHost = resolveReachableHost(rawHost, port);
        if (reachableHost == null) {
            log.warn("[K3s] 无法通过 TCP 探测到任何可达的 K3s API 端口 ({}:{})", rawHost, port);
            return null;
        }

        String effectiveMasterUrl = "https://" + reachableHost + ":" + port;
        log.info("[K3s] 解析到有效的 K3s API Server 端点: {} (原始端点: {})", effectiveMasterUrl, rawMasterUrl);

        return new ConfigBuilder(baseConfig)
                .withMasterUrl(effectiveMasterUrl)
                .withTrustCerts(true)
                .build();
    }

    private String resolveReachableHost(String rawHost, int port) {
        List<String> candidates = new ArrayList<>();
        boolean isLoopback = "127.0.0.1".equals(rawHost) || "localhost".equalsIgnoreCase(rawHost);
        if (!isLoopback && StringUtils.hasText(rawHost)) {
            candidates.add(rawHost.trim());
        }

        // 尝试原始环回地址（在宿主机直接运行或 host 网络模式下有效）
        candidates.add("127.0.0.1");

        // 针对 Docker 容器环境：host.docker.internal (通过 docker-compose extra_hosts 注入)
        candidates.add("host.docker.internal");

        // 针对 Linux Docker 容器：自动解析容器默认路由网关 IP
        String containerGateway = getContainerDefaultGateway();
        if (StringUtils.hasText(containerGateway) && !candidates.contains(containerGateway)) {
            candidates.add(containerGateway);
        }

        // 尝试平台配置的宿主机/公网 Host
        String publicHost = resolvePublicHost();
        if (StringUtils.hasText(publicHost) && !candidates.contains(publicHost) && !"127.0.0.1".equals(publicHost)) {
            candidates.add(publicHost.trim());
        }

        for (String candidate : candidates) {
            if (isPortReachable(candidate, port, 1000)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean isPortReachable(String host, int port, int timeoutMs) {
        if (!StringUtils.hasText(host)) return false;
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String getContainerDefaultGateway() {
        File routeFile = new File("/proc/net/route");
        if (!routeFile.exists() || !routeFile.canRead()) {
            return null;
        }
        try (BufferedReader reader = Files.newBufferedReader(routeFile.toPath())) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.trim().split("\\s+");
                if (tokens.length > 2 && "00000000".equals(tokens[1])) {
                    String hexGateway = tokens[2];
                    if (hexGateway.length() == 8) {
                        int b1 = Integer.parseInt(hexGateway.substring(6, 8), 16);
                        int b2 = Integer.parseInt(hexGateway.substring(4, 6), 16);
                        int b3 = Integer.parseInt(hexGateway.substring(2, 4), 16);
                        int b4 = Integer.parseInt(hexGateway.substring(0, 2), 16);
                        return b1 + "." + b2 + "." + b3 + "." + b4;
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private Set<Integer> getExistingK3sNodePorts(KubernetesClient client) {
        Set<Integer> ports = new HashSet<>();
        if (client == null) {
            return ports;
        }
        try {
            ServiceList serviceList = client.services().inAnyNamespace().list();
            if (serviceList != null && serviceList.getItems() != null) {
                for (Service s : serviceList.getItems()) {
                    if (s.getSpec() != null && s.getSpec().getPorts() != null) {
                        for (ServicePort sp : s.getSpec().getPorts()) {
                            if (sp.getNodePort() != null && sp.getNodePort() > 0) {
                                ports.add(sp.getNodePort());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[K3s] 获取集群已占用 NodePort 列表失败: {}", e.getMessage());
        }
        return ports;
    }

    @Override
    public ContainerRunResultDTO startContainer(String resourceName, Challenge challenge, String dynamicFlag, Integer cpuLimit, Integer memoryLimit, String targetNodeIp) {
        String sanitizedName = sanitizeResourceName(resourceName);
        KubernetesClient client = getClient();
        String namespace = resolveNamespace();
        String publicHost = resolvePublicHost();
        String targetHost = StringUtils.hasText(targetNodeIp) ? targetNodeIp.trim() : publicHost;
        int portStart = systemSettingService.getInt(SystemSettingKeys.CONTAINER_PORT_RANGE_START, 20000);
        int portEnd = systemSettingService.getInt(SystemSettingKeys.CONTAINER_PORT_RANGE_END, 30000);

        if (client != null && challenge instanceof Containerizable containerizable) {
            int targetPort = 0;
            try {
                ensureNamespaceAndNetworkPolicy(client, namespace);
                String imageName = containerizable.getImageName();
                int containerPort = containerizable.getContainerPort() != null ? containerizable.getContainerPort() : 80;

                Set<Integer> existingK3sPorts = getExistingK3sNodePorts(client);
                targetPort = containerPortAllocator.allocateAvailablePort(targetHost, portStart, portEnd, existingK3sPorts);

                // 1. Build Pod
                ResourceRequirements resources = new ResourceRequirementsBuilder()
                        .addToLimits("cpu", new Quantity((cpuLimit != null ? cpuLimit : 1) + ""))
                        .addToLimits("memory", new Quantity((memoryLimit != null ? memoryLimit : 128) + "Mi"))
                        .addToRequests("cpu", new Quantity("100m"))
                        .addToRequests("memory", new Quantity("64Mi"))
                        .build();

                List<EnvVar> envVars = StringUtils.hasText(dynamicFlag)
                        ? List.of(new EnvVarBuilder().withName("volurflag").withValue(dynamicFlag).build())
                        : Collections.emptyList();

                var podSpecBuilder = new PodSpecBuilder()
                        .withRestartPolicy("Never")
                        .withAutomountServiceAccountToken(false)
                        .addNewContainer()
                        .withName("challenge-container")
                        .withImage(resolveEffectiveImageName(imageName))
                        .withImagePullPolicy("IfNotPresent")
                        .withEnv(envVars)
                        .withResources(resources)
                        .addNewPort()
                        .withContainerPort(containerPort)
                        .endPort()
                        .endContainer();

                applyK3sNodeSelector(podSpecBuilder, targetNodeIp);

                Pod pod = new PodBuilder()
                        .withNewMetadata()
                        .withName(sanitizedName)
                        .withNamespace(namespace)
                        .withLabels(Map.of("app", sanitizedName, "challenge-id", String.valueOf(challenge.getId())))
                        .endMetadata()
                        .withSpec(podSpecBuilder.build())
                        .build();

                client.pods().inNamespace(namespace).resource(pod).create();

                // 2. Build NodePort Service
                Service service = new ServiceBuilder()
                        .withNewMetadata()
                        .withName(sanitizedName)
                        .withNamespace(namespace)
                        .withLabels(Map.of("app", sanitizedName))
                        .endMetadata()
                        .withNewSpec()
                        .withType("NodePort")
                        .withSelector(Map.of("app", sanitizedName))
                        .addNewPort()
                        .withName("http")
                        .withPort(containerPort)
                        .withTargetPort(new IntOrString(containerPort))
                        .withNodePort(targetPort)
                        .endPort()
                        .endSpec()
                        .build();

                Service createdService = client.services().inNamespace(namespace).resource(service).create();
                Integer nodePort = null;
                if (createdService.getSpec() != null && createdService.getSpec().getPorts() != null && !createdService.getSpec().getPorts().isEmpty()) {
                    nodePort = createdService.getSpec().getPorts().getFirst().getNodePort();
                }

                if (nodePort == null) {
                    nodePort = targetPort;
                }

                String accessUrl = "http://" + targetHost + ":" + nodePort;
                return new ContainerRunResultDTO(sanitizedName, accessUrl, nodePort, InstanceStartupStatus.RUNNING);
            } catch (BusinessException be) {
                throw be;
            } catch (Exception e) {
                log.error("[K3s] 调度 K3s Pod/Service {} 失败: {}", sanitizedName, e.getMessage(), e);
                String msg = e.getMessage() != null ? e.getMessage() : "";
                if (msg.contains("provided port is not in the valid range") || msg.contains("valid ports is")) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST,
                            "K3s Service 端口 " + targetPort + " 超出集群 NodePort 允许范围。如需使用自定义范围 [" + portStart + "-" + portEnd + "]，请在 K3s 启动参数中添加: --kube-apiserver-arg=service-node-port-range=" + portStart + "-" + portEnd + "；或在集群设置中调整端口范围");
                }
                throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "K3s Pod/Service 调度失败: " + e.getMessage());
            }
        }

        if (client == null) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "K3s 集群未连接或不可用，请确认 /etc/rancher/k3s/k3s.yaml 已正确挂载且集群正常运行");
        }
        throw new BusinessException(HttpStatus.BAD_REQUEST, "题目未实现 Containerizable 接口，无法启动动态容器");
    }

    @Override
    public ContainerRunResultDTO startManualContainer(AdminContainerManualStartDTO request, String resourceName, Challenge challenge, String dynamicFlag) {
        String baseName = (request != null && StringUtils.hasText(request.getContainerName()))
                ? request.getContainerName().trim()
                : resourceName;
        String sanitizedName = sanitizeResourceName(baseName);

        String imageName = (request != null && StringUtils.hasText(request.getImageName()))
                ? request.getImageName().trim()
                : (challenge instanceof Containerizable c ? c.getImageName() : "");

        if (!StringUtils.hasText(imageName)) {
            throw new IllegalArgumentException("必须指定运行的 Docker 镜像名称");
        }

        KubernetesClient client = getClient();
        String namespace = resolveNamespace();
        String publicHost = resolvePublicHost();
        String targetNodeIp = (request != null && StringUtils.hasText(request.getNodeIp())) ? request.getNodeIp().trim() : null;
        String targetHost = StringUtils.hasText(targetNodeIp) ? targetNodeIp : publicHost;
        int portStart = systemSettingService.getInt(SystemSettingKeys.CONTAINER_PORT_RANGE_START, 20000);
        int portEnd = systemSettingService.getInt(SystemSettingKeys.CONTAINER_PORT_RANGE_END, 30000);

        Integer cpu = (request != null && request.getCpuLimit() != null && request.getCpuLimit() > 0)
                ? request.getCpuLimit()
                : (challenge instanceof com.humolix.volur.challenge.domain.entity.ctf.CtfChallenge c ? c.getCpuLimit() : 1);
        Integer mem = (request != null && request.getMemoryLimit() != null && request.getMemoryLimit() > 0)
                ? request.getMemoryLimit()
                : (challenge instanceof com.humolix.volur.challenge.domain.entity.ctf.CtfChallenge c ? c.getMemoryLimit() : 256);

        int primaryPort = 0;

        if (client != null) {
            try {
                ensureNamespaceAndNetworkPolicy(client, namespace);
                Set<Integer> existingK3sPorts = getExistingK3sNodePorts(client);

                // 1. 资源配额限制
                ResourceRequirements resources = new ResourceRequirementsBuilder()
                        .addToLimits("cpu", new Quantity(cpu + ""))
                        .addToLimits("memory", new Quantity(mem + "Mi"))
                        .addToRequests("cpu", new Quantity("100m"))
                        .addToRequests("memory", new Quantity("64Mi"))
                        .build();

                // 2. 自定义环境变量 (Environment variables)
                List<EnvVar> envVars = new ArrayList<>();
                if (request != null && request.getEnvironments() != null) {
                    for (ContainerEnvVarDTO env : request.getEnvironments()) {
                        if (StringUtils.hasText(env.getName())) {
                            envVars.add(new EnvVarBuilder()
                                    .withName(env.getName().trim())
                                    .withValue(env.getValue() != null ? env.getValue() : "")
                                    .build());
                        }
                    }
                }
                if (StringUtils.hasText(dynamicFlag)) {
                    envVars.add(new EnvVarBuilder().withName("volurflag").withValue(dynamicFlag).build());
                }

                // 3. 数据卷挂载 (Volumes)
                List<Volume> podVolumes = new ArrayList<>();
                List<VolumeMount> volumeMounts = new ArrayList<>();
                if (request != null && request.getVolumes() != null) {
                    int volIdx = 0;
                    for (ContainerVolumeMappingDTO vol : request.getVolumes()) {
                        if (StringUtils.hasText(vol.getHostPath()) && StringUtils.hasText(vol.getContainerPath())) {
                            String volName = "vol-" + volIdx++;
                            podVolumes.add(new VolumeBuilder()
                                    .withName(volName)
                                    .withHostPath(new HostPathVolumeSourceBuilder().withPath(vol.getHostPath().trim()).build())
                                    .build());
                            volumeMounts.add(new VolumeMountBuilder()
                                    .withName(volName)
                                    .withMountPath(vol.getContainerPath().trim())
                                    .withReadOnly("ro".equalsIgnoreCase(vol.getMode()))
                                    .build());
                        }
                    }
                }

                // 4. 端口映射 (Ports)
                List<ContainerPort> containerPorts = new ArrayList<>();
                List<ServicePort> servicePorts = new ArrayList<>();
                if (request != null && request.getPorts() != null && !request.getPorts().isEmpty()) {
                    int portIdx = 0;
                    for (ContainerPortMappingDTO portCfg : request.getPorts()) {
                        if (portCfg.getContainerPort() != null && portCfg.getContainerPort() > 0) {
                            String proto = StringUtils.hasText(portCfg.getProtocol()) ? portCfg.getProtocol().trim().toUpperCase() : "TCP";
                            containerPorts.add(new ContainerPortBuilder()
                                    .withName("p" + portIdx)
                                    .withContainerPort(portCfg.getContainerPort())
                                    .withProtocol(proto)
                                    .build());

                            int hPort;
                            if (portCfg.getHostPort() != null && portCfg.getHostPort() > 0) {
                                containerPortAllocator.verifyPortAvailable(targetHost, portCfg.getHostPort(), existingK3sPorts);
                                hPort = portCfg.getHostPort();
                            } else {
                                hPort = containerPortAllocator.allocateAvailablePort(targetHost, portStart, portEnd, existingK3sPorts);
                                portCfg.setHostPort(hPort);
                            }

                            if (portIdx == 0) {
                                primaryPort = hPort;
                            }

                            servicePorts.add(new ServicePortBuilder()
                                    .withName("sp-" + portIdx)
                                    .withPort(portCfg.getContainerPort())
                                    .withTargetPort(new IntOrString(portCfg.getContainerPort()))
                                    .withNodePort(hPort)
                                    .withProtocol(proto)
                                    .build());
                            portIdx++;
                        }
                    }
                }

                if (containerPorts.isEmpty()) {
                    int cPort = (challenge instanceof Containerizable c && c.getContainerPort() != null)
                            ? c.getContainerPort()
                            : 80;
                    int hPort = containerPortAllocator.allocateAvailablePort(targetHost, portStart, portEnd, existingK3sPorts);
                    primaryPort = hPort;
                    containerPorts.add(new ContainerPortBuilder().withName("http").withContainerPort(cPort).build());
                    servicePorts.add(new ServicePortBuilder().withName("http").withPort(cPort).withTargetPort(new IntOrString(cPort)).withNodePort(hPort).build());
                    if (request != null) {
                        request.setPorts(new ArrayList<>(List.of(new ContainerPortMappingDTO(hPort, cPort, "TCP"))));
                    }
                }

                // 5. 构建 Pod 规范
                var podSpecBuilder = new PodSpecBuilder()
                        .withRestartPolicy("Never")
                        .withAutomountServiceAccountToken(false)
                        .addNewContainer()
                        .withName("manual-container")
                        .withImage(resolveEffectiveImageName(imageName))
                        .withImagePullPolicy("IfNotPresent")
                        .withEnv(envVars)
                        .withResources(resources)
                        .withPorts(containerPorts)
                        .withVolumeMounts(volumeMounts)
                        .endContainer()
                        .withVolumes(podVolumes);

                applyK3sNodeSelector(podSpecBuilder, targetNodeIp);

                Pod pod = new PodBuilder()
                        .withNewMetadata()
                        .withName(sanitizedName)
                        .withNamespace(namespace)
                        .withLabels(Map.of("app", sanitizedName, "managed-by", "volur-manual"))
                        .endMetadata()
                        .withSpec(podSpecBuilder.build())
                        .build();

                client.pods().inNamespace(namespace).resource(pod).create();

                // 6. 构建 Service 服务代理
                Service service = new ServiceBuilder()
                        .withNewMetadata()
                        .withName(sanitizedName)
                        .withNamespace(namespace)
                        .withLabels(Map.of("app", sanitizedName))
                        .endMetadata()
                        .withNewSpec()
                        .withType("NodePort")
                        .withSelector(Map.of("app", sanitizedName))
                        .withPorts(servicePorts)
                        .endSpec()
                        .build();

                Service createdService = client.services().inNamespace(namespace).resource(service).create();
                if (createdService.getSpec() != null && createdService.getSpec().getPorts() != null && !createdService.getSpec().getPorts().isEmpty()) {
                    Integer allocatedNodePort = createdService.getSpec().getPorts().getFirst().getNodePort();
                    if (allocatedNodePort != null && allocatedNodePort > 0) {
                        primaryPort = allocatedNodePort;
                    }
                }

                String accessUrl = "http://" + targetHost + ":" + primaryPort;
                log.info("[K3s] 手动自定义容器调度成功: pod={}, url={}", sanitizedName, accessUrl);
                return new ContainerRunResultDTO(sanitizedName, accessUrl, primaryPort, InstanceStartupStatus.RUNNING);
            } catch (BusinessException be) {
                throw be;
            } catch (Exception e) {
                log.error("[K3s] 自定义容器调度异常: {}", e.getMessage(), e);
                String msg = e.getMessage() != null ? e.getMessage() : "";
                if (msg.contains("provided port is not in the valid range") || msg.contains("valid ports is")) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST,
                            "K3s Service 端口超出集群 NodePort 允许范围。如需使用自定义范围 [" + portStart + "-" + portEnd + "]，请在 K3s 启动参数中添加: --kube-apiserver-arg=service-node-port-range=" + portStart + "-" + portEnd + "；或在集群设置中调整端口范围");
                }
                throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "K3s 自定义容器启动失败: " + e.getMessage());
            }
        }

        throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "K3s 集群未连接或不可用，请确认 /etc/rancher/k3s/k3s.yaml 已正确挂载且集群正常运行");
    }

    /**
     * 确保 K3s 靶机命名空间存在并下发网络隔离安全策略 (NetworkPolicy)
     * 1. Ingress: 允许外部流量通过 NodePort/集群入口访问靶机服务端口
     * 2. Egress: 允许 DNS 解析 (UDP/TCP 53) 并严格阻断私网探测 (RFC1918、Link-Local、云元数据等)，实现靶机隔离
     */
    private void ensureNamespaceAndNetworkPolicy(KubernetesClient client, String namespace) {
        if (client == null || !StringUtils.hasText(namespace)) {
            return;
        }
        try {
            // 1. 确保 Namespace 存在
            if (client.namespaces().withName(namespace).get() == null) {
                client.namespaces().resource(new NamespaceBuilder()
                        .withNewMetadata().withName(namespace).endMetadata()
                        .build()).create();
                log.info("[K3s] 成功创建靶机命名空间: {}", namespace);
            }

            // 2. 下发命名空间级别默认安全隔离策略 (NetworkPolicy)
            String policyName = "volur-challenge-isolation";
            NetworkPolicy policy = new NetworkPolicyBuilder()
                    .withNewMetadata()
                    .withName(policyName)
                    .withNamespace(namespace)
                    .endMetadata()
                    .withNewSpec()
                    .withNewPodSelector().endPodSelector()
                    .withPolicyTypes("Ingress", "Egress")
                    // 允许外部入站流量访问靶机服务端口
                    .addNewIngress().endIngress()
                    // 允许 DNS 域名解析 (UDP/TCP 53)
                    .addNewEgress()
                        .addNewPort()
                            .withProtocol("UDP")
                            .withPort(new IntOrString(53))
                        .endPort()
                        .addNewPort()
                            .withProtocol("TCP")
                            .withPort(new IntOrString(53))
                        .endPort()
                    .endEgress()
                    // 允许公网出网，但严格排除所有内网/私有网段与云厂商元数据地址，阻断靶机横向移动与 SSRF
                    .addNewEgress()
                        .addNewTo()
                            .withNewIpBlock()
                                .withCidr("0.0.0.0/0")
                                .withExcept(
                                        "10.0.0.0/8",       // 私网 Class A & K3s Pod/Service CIDR
                                        "172.16.0.0/12",    // 私网 Class B & Docker 默认网络
                                        "192.168.0.0/16",   // 私网 Class C & 局域网
                                        "169.254.0.0/16",   // 链路本地地址及云元数据 (AWS/GCP/Aliyun 169.254.169.254)
                                        "127.0.0.0/8",      // 回环网段
                                        "100.64.0.0/10",    // 运营商级 NAT (CGNAT)
                                        "224.0.0.0/4"       // 组播网段
                                )
                            .endIpBlock()
                        .endTo()
                    .endEgress()
                    .endSpec()
                    .build();

            client.network().networkPolicies().inNamespace(namespace).resource(policy).createOrReplace();
            log.info("[K3s] 成功下发命名空间 [{}] 安全隔离策略 (NetworkPolicy: {})", namespace, policyName);
        } catch (Exception e) {
            log.warn("[K3s] 下发 Namespace 或 NetworkPolicy 策略告警: {}", e.getMessage());
        }
    }

    private void applyK3sNodeSelector(PodSpecBuilder podSpecBuilder, String targetNodeIp) {
        if (podSpecBuilder == null) return;
        if (StringUtils.hasText(targetNodeIp) && clusterNodeRepository != null) {
            var nodeOpt = clusterNodeRepository.findByIpAddress(targetNodeIp.trim());
            if (nodeOpt.isPresent()) {
                var node = nodeOpt.get();
                if ("MASTER".equalsIgnoreCase(node.getRole())) {
                    podSpecBuilder.withNodeSelector(Map.of("node-role.kubernetes.io/control-plane", "true"));
                } else {
                    podSpecBuilder.withNodeSelector(Map.of("kubernetes.io/hostname", node.getNodeName().trim()));
                }
                return;
            }
        }

        boolean resourceIsolation = systemSettingService.getBoolean(SystemSettingKeys.CONTAINER_RESOURCE_ISOLATION_ENABLED, true);
        if (resourceIsolation && clusterNodeRepository != null && clusterNodeRepository.countByRole("WORKER") > 0) {
            podSpecBuilder.withNodeSelector(Map.of("node-role.kubernetes.io/worker", "true"));
        }
    }

    @Override
    public void stopContainer(String resourceName) {
        String sanitizedName = sanitizeResourceName(resourceName);
        KubernetesClient client = getClient();
        String namespace = resolveNamespace();
        if (client != null) {
            try {
                client.services().inNamespace(namespace).withName(sanitizedName).delete();
                client.pods().inNamespace(namespace).withName(sanitizedName).delete();
                log.info("Successfully cleaned up K3s Pod and Service: {}", sanitizedName);
            } catch (Exception e) {
                log.warn("Failed to cleanup K3s resource {}: {}", sanitizedName, e.getMessage());
            }
        }
    }

    @Override
    public com.humolix.volur.container.domain.vo.AdminContainerTestConnectionVO testConnection(
            com.humolix.volur.container.domain.dto.AdminContainerTestConnectionDTO request) {
        long startTime = System.currentTimeMillis();
        String kubeconfigPath = (request != null && org.springframework.util.StringUtils.hasText(request.getK3sKubeconfigPath()))
                ? request.getK3sKubeconfigPath().trim()
                : resolveKubeconfigPath();
        String namespace = (request != null && org.springframework.util.StringUtils.hasText(request.getK3sNamespace()))
                ? request.getK3sNamespace().trim()
                : resolveNamespace();

        // 1. 严格检查 kubeconfig 配置文件是否存在（严禁无 kubeconfig 时降级直连 Master URL）
        File kubeconfigFile = new File(kubeconfigPath);
        if (!kubeconfigFile.exists() || !kubeconfigFile.isFile()) {
            long latency = System.currentTimeMillis() - startTime;
            return com.humolix.volur.container.domain.vo.AdminContainerTestConnectionVO.builder()
                    .connected(false)
                    .latencyMs(latency)
                    .message("K3s kubeconfig 配置文件不存在 (" + kubeconfigPath + ")，请确认宿主机 /etc/rancher/k3s/k3s.yaml 是否已正确挂载至容器")
                    .build();
        }

        String kubeconfigContent;
        try {
            kubeconfigContent = Files.readString(Path.of(kubeconfigPath));
            if (!StringUtils.hasText(kubeconfigContent)) {
                throw new IllegalArgumentException("kubeconfig 文件内容为空");
            }
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            return com.humolix.volur.container.domain.vo.AdminContainerTestConnectionVO.builder()
                    .connected(false)
                    .latencyMs(latency)
                    .message("读取 K3s kubeconfig 文件失败: " + e.getMessage())
                    .build();
        }

        // 2. 快速 TCP Socket 预检（自适应 Docker 容器与宿主机网络，避免 Fabric8 长时重试阻塞）
        Config baseConfig;
        try {
            baseConfig = Config.fromKubeconfig(kubeconfigContent);
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            return com.humolix.volur.container.domain.vo.AdminContainerTestConnectionVO.builder()
                    .connected(false)
                    .latencyMs(latency)
                    .message("解析 K3s kubeconfig 配置格式异常: " + e.getMessage())
                    .build();
        }

        String rawMasterUrl = baseConfig.getMasterUrl();
        URI uri;
        try {
            uri = URI.create(rawMasterUrl.contains("://") ? rawMasterUrl : ("https://" + rawMasterUrl));
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            return com.humolix.volur.container.domain.vo.AdminContainerTestConnectionVO.builder()
                    .connected(false)
                    .latencyMs(latency)
                    .message("kubeconfig 中 Server URL 格式非法: " + rawMasterUrl)
                    .build();
        }

        String rawHost = uri.getHost() != null ? uri.getHost() : "127.0.0.1";
        int port = uri.getPort() != -1 ? uri.getPort() : 6443;

        String reachableHost = resolveReachableHost(rawHost, port);
        if (reachableHost == null) {
            long latency = System.currentTimeMillis() - startTime;
            log.warn("[K3s] 集群端口探测不可达 {}:{}", rawHost, port);
            return com.humolix.volur.container.domain.vo.AdminContainerTestConnectionVO.builder()
                    .connected(false)
                    .latencyMs(latency)
                    .message("未能连接到宿主机 K3s API 端口 (" + port + ")。容器内已尝试探测 " + rawHost +
                            "、host.docker.internal 及网关地址，连接均被拒绝或超时，请确认宿主机 K3s 正在运行")
                    .build();
        }

        String effectiveMasterUrl = "https://" + reachableHost + ":" + port;

        // 3. 异步超时熔断探针（限制在 4000ms 内响应，防止前端阻塞超时）
        try {
            Config config = new ConfigBuilder(baseConfig)
                    .withMasterUrl(effectiveMasterUrl)
                    .withTrustCerts(true)
                    .withConnectionTimeout(2500)
                    .withRequestTimeout(3500)
                    .build();

            CompletableFuture<com.humolix.volur.container.domain.vo.AdminContainerTestConnectionVO> probeFuture = CompletableFuture.supplyAsync(() -> {
                try (KubernetesClient testClient = new KubernetesClientBuilder().withConfig(config).build()) {
                    var ver = testClient.getKubernetesVersion();
                    int nodes = testClient.nodes().list().getItems().size();
                    long latency = System.currentTimeMillis() - startTime;
                    String verStr = ver != null ? (ver.getMajor() + "." + ver.getMinor()) : "v1.28";

                    // 重置全局单例 client，确保后续操作能基于生效配置重新握手
                    resetClient();

                    return com.humolix.volur.container.domain.vo.AdminContainerTestConnectionVO.builder()
                            .connected(true)
                            .clusterVersion("Kubernetes / K3s " + verStr)
                            .nodeCount(nodes)
                            .latencyMs(latency)
                            .message("K3s 集群连接成功 (命名空间: " + namespace + ", 节点数: " + nodes + ", 接入端点: " + effectiveMasterUrl + ")")
                            .build();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });

            return probeFuture.get(4000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.warn("[K3s] 连通性测试异常: {}", e.getMessage());
            long latency = System.currentTimeMillis() - startTime;
            String errMsg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            return com.humolix.volur.container.domain.vo.AdminContainerTestConnectionVO.builder()
                    .connected(false)
                    .latencyMs(latency)
                    .message("未能连接到 K3s 集群 (" + effectiveMasterUrl + "): " + errMsg)
                    .build();
        }
    }

    @Override
    public com.humolix.volur.container.domain.vo.AdminContainerExecResultVO execCommand(
            String containerId,
            com.humolix.volur.container.domain.dto.AdminContainerExecDTO request,
            String fallbackFlag) {
        long startTime = System.currentTimeMillis();
        String cmd = (request != null && org.springframework.util.StringUtils.hasText(request.getCommand())) ? request.getCommand().trim() : "id";
        String workingDir = (request != null && org.springframework.util.StringUtils.hasText(request.getWorkingDir())) ? request.getWorkingDir().trim() : "/app";
        String sanitizedName = sanitizeResourceName(containerId);

        KubernetesClient client = getClient();
        if (client == null) {
            return com.humolix.volur.container.domain.vo.AdminContainerExecResultVO.builder()
                    .output("")
                    .errorOutput("K3s 集群未连接，无法执行终端命令")
                    .exitCode(1)
                    .workingDir(workingDir)
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }

        String namespace = resolveNamespace();
        try {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            java.io.ByteArrayOutputStream err = new java.io.ByteArrayOutputStream();

            var execWatch = client.pods().inNamespace(namespace).withName(sanitizedName)
                    .writingOutput(out)
                    .writingError(err)
                    .exec("sh", "-c", cmd);

            boolean done = execWatch.exitCode().isDone();
            int exitCode = done ? execWatch.exitCode().get() : 0;
            long elapsed = System.currentTimeMillis() - startTime;

            return com.humolix.volur.container.domain.vo.AdminContainerExecResultVO.builder()
                    .output(out.toString(java.nio.charset.StandardCharsets.UTF_8))
                    .errorOutput(err.toString(java.nio.charset.StandardCharsets.UTF_8))
                    .exitCode(exitCode)
                    .workingDir(workingDir)
                    .executionTimeMs(elapsed)
                    .build();
        } catch (Exception e) {
            log.warn("[K3s] Pod exec 执行异常: {}", e.getMessage());
            return com.humolix.volur.container.domain.vo.AdminContainerExecResultVO.builder()
                    .output("")
                    .errorOutput("K3s Pod 命令执行异常: " + e.getMessage())
                    .exitCode(1)
                    .workingDir(workingDir)
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    @Override
    public InstanceStartupStatus checkInstanceStatus(String containerId) {
        String sanitizedName = sanitizeResourceName(containerId);
        KubernetesClient client = getClient();
        String namespace = resolveNamespace();
        if (client != null) {
            try {
                Pod pod = client.pods().inNamespace(namespace).withName(sanitizedName).get();
                if (pod != null && pod.getStatus() != null) {
                    String phase = pod.getStatus().getPhase();
                    if ("Running".equalsIgnoreCase(phase)) {
                        return InstanceStartupStatus.RUNNING;
                    } else if ("Pending".equalsIgnoreCase(phase)) {
                        return InstanceStartupStatus.STARTING;
                    } else if ("Failed".equalsIgnoreCase(phase)) {
                        return InstanceStartupStatus.FAILED;
                    } else if ("Succeeded".equalsIgnoreCase(phase)) {
                        return InstanceStartupStatus.STOPPED;
                    }
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    @Override
    public AdminContainerTerminalInfoVO getTerminalInfo(String containerId, String fallbackFlag) {
        String sanitizedName = sanitizeResourceName(containerId);
        String shortId = sanitizedName.length() > 12 ? sanitizedName.substring(0, 12) : sanitizedName;
        KubernetesClient client = getClient();
        String namespace = resolveNamespace();
        if (client != null) {
            try {
                Pod pod = client.pods().inNamespace(namespace).withName(sanitizedName).get();
                if (pod != null && pod.getStatus() != null) {
                    boolean isRunning = "Running".equalsIgnoreCase(pod.getStatus().getPhase());
                    String prompt = "root@" + shortId + ":/app#";
                    return AdminContainerTerminalInfoVO.builder()
                            .ready(isRunning)
                            .containerShortId(shortId)
                            .currentUser("root")
                            .workingDir("/app")
                            .prompt(prompt)
                            .taskState(pod.getStatus().getPhase())
                            .message(isRunning ? "连接就绪" : "Pod 尚未就绪 (Phase: " + pod.getStatus().getPhase() + ")")
                            .build();
                }
            } catch (Exception e) {
                log.warn("[K3s] 获取 Pod 终端信息异常: {}", e.getMessage());
            }
        }
        return AdminContainerTerminalInfoVO.builder()
                .ready(false)
                .containerShortId(shortId)
                .currentUser("root")
                .workingDir("/app")
                .taskState("Unknown")
                .message("Pod 实例尚未就绪或 K3s 集群连接不可用")
                .build();
    }

    private String sanitizeResourceName(String name) {
        if (name == null) return "pod-" + System.currentTimeMillis();
        return name.toLowerCase().replaceAll("[^a-z0-9-]", "-");
    }

    public record K3sDiscoveredNode(
            String nodeName,
            String ipAddress,
            String role,
            String status,
            String osInfo,
            String containerRuntimeVersion,
            int cpuCores,
            long memoryBytes,
            String uid
    ) {}

    /**
     * 扫描 K3s 集群中所有注册节点并提取硬件、角色与存活状态
     */
    public List<K3sDiscoveredNode> scanK3sNodes() {
        KubernetesClient client = getClient();
        if (client == null) {
            log.warn("[K3s] 无法建立 K3s 客户端连接，跳过节点扫描");
            return Collections.emptyList();
        }

        List<K3sDiscoveredNode> list = new ArrayList<>();
        try {
            var k8sNodes = client.nodes().list().getItems();
            for (Node k8sNode : k8sNodes) {
                String nodeName = k8sNode.getMetadata() != null ? k8sNode.getMetadata().getName() : "unknown";
                String uid = k8sNode.getMetadata() != null ? k8sNode.getMetadata().getUid() : "";

                // 提取 IP 地址：优先 InternalIP，其次 ExternalIP
                String ip = null;
                if (k8sNode.getStatus() != null && k8sNode.getStatus().getAddresses() != null) {
                    for (NodeAddress addr : k8sNode.getStatus().getAddresses()) {
                        if ("InternalIP".equalsIgnoreCase(addr.getType()) && StringUtils.hasText(addr.getAddress())) {
                            ip = addr.getAddress().trim();
                            break;
                        }
                    }
                    if (ip == null) {
                        for (NodeAddress addr : k8sNode.getStatus().getAddresses()) {
                            if ("ExternalIP".equalsIgnoreCase(addr.getType()) && StringUtils.hasText(addr.getAddress())) {
                                ip = addr.getAddress().trim();
                                break;
                            }
                        }
                    }
                    if (ip == null && !k8sNode.getStatus().getAddresses().isEmpty()) {
                        ip = k8sNode.getStatus().getAddresses().getFirst().getAddress();
                    }
                }
                if (!StringUtils.hasText(ip)) {
                    ip = resolvePublicHost();
                }

                // 角色研判：判断是否为 Control-Plane / Master
                String role = "WORKER";
                Map<String, String> labels = k8sNode.getMetadata() != null ? k8sNode.getMetadata().getLabels() : Collections.emptyMap();
                if (labels != null) {
                    if (labels.containsKey("node-role.kubernetes.io/control-plane")
                            || labels.containsKey("node-role.kubernetes.io/master")
                            || "true".equalsIgnoreCase(labels.get("node-role.kubernetes.io/control-plane"))
                            || "true".equalsIgnoreCase(labels.get("node-role.kubernetes.io/master"))) {
                        role = "MASTER";
                    }
                }

                // 节点存活性研判
                String status = "OFFLINE";
                if (k8sNode.getStatus() != null && k8sNode.getStatus().getConditions() != null) {
                    for (NodeCondition cond : k8sNode.getStatus().getConditions()) {
                        if ("Ready".equalsIgnoreCase(cond.getType())) {
                            status = "True".equalsIgnoreCase(cond.getStatus()) ? "READY" : "OFFLINE";
                            break;
                        }
                    }
                }

                // 节点系统与运行时信息
                String osInfo = "Linux";
                String runtimeVersion = "containerd";
                if (k8sNode.getStatus() != null && k8sNode.getStatus().getNodeInfo() != null) {
                    NodeSystemInfo ni = k8sNode.getStatus().getNodeInfo();
                    String osImage = StringUtils.hasText(ni.getOsImage()) ? ni.getOsImage().trim() : "Linux";
                    String arch = StringUtils.hasText(ni.getArchitecture()) ? ni.getArchitecture().trim() : "x86_64";
                    String kernel = StringUtils.hasText(ni.getKernelVersion()) ? ni.getKernelVersion().trim() : "";
                    osInfo = StringUtils.hasText(kernel) ? (osImage + " (" + arch + ", " + kernel + ")") : (osImage + " (" + arch + ")");
                    if (osInfo.length() > 125) {
                        osInfo = osInfo.substring(0, 122) + "...";
                    }
                    if (StringUtils.hasText(ni.getContainerRuntimeVersion())) {
                        runtimeVersion = ni.getContainerRuntimeVersion().trim();
                    }
                }

                // 计算资源容量 (CPU / 内存)
                int cpuCores = 1;
                long memoryBytes = 1073741824L;
                if (k8sNode.getStatus() != null && k8sNode.getStatus().getCapacity() != null) {
                    Map<String, Quantity> cap = k8sNode.getStatus().getCapacity();
                    if (cap.containsKey("cpu") && cap.get("cpu") != null) {
                        cpuCores = parseCpuCores(cap.get("cpu").getAmount());
                    }
                    if (cap.containsKey("memory") && cap.get("memory") != null) {
                        Quantity memQuantity = cap.get("memory");
                        try {
                            memoryBytes = Quantity.getAmountInBytes(memQuantity).longValue();
                        } catch (Exception ignored) {
                            memoryBytes = parseMemoryBytes(memQuantity.getAmount());
                        }
                    }
                }

                list.add(new K3sDiscoveredNode(nodeName, ip, role, status, osInfo, runtimeVersion, cpuCores, memoryBytes, uid));
            }
        } catch (Exception e) {
            log.error("[K3s] 扫描 K3s 集群节点列表异常: {}", e.getMessage(), e);
        }

        return list;
    }

    public static int parseCpuCores(String amount) {
        if (!StringUtils.hasText(amount)) return 1;
        amount = amount.trim();
        try {
            if (amount.endsWith("m")) {
                int millis = Integer.parseInt(amount.substring(0, amount.length() - 1));
                return Math.max(1, millis / 1000);
            }
            return Math.max(1, Integer.parseInt(amount));
        } catch (Exception e) {
            return 1;
        }
    }

    public static long parseMemoryBytes(String amount) {
        if (!StringUtils.hasText(amount)) return 1073741824L;
        amount = amount.trim();
        try {
            if (amount.endsWith("Ki")) {
                return Long.parseLong(amount.substring(0, amount.length() - 2)) * 1024L;
            } else if (amount.endsWith("Mi")) {
                return Long.parseLong(amount.substring(0, amount.length() - 2)) * 1024L * 1024L;
            } else if (amount.endsWith("Gi")) {
                return Long.parseLong(amount.substring(0, amount.length() - 2)) * 1024L * 1024L * 1024L;
            } else if (amount.endsWith("Ti")) {
                return Long.parseLong(amount.substring(0, amount.length() - 2)) * 1024L * 1024L * 1024L * 1024L;
            } else if (amount.endsWith("K") || amount.endsWith("k")) {
                return Long.parseLong(amount.substring(0, amount.length() - 1)) * 1000L;
            } else if (amount.endsWith("M") || amount.endsWith("m")) {
                return Long.parseLong(amount.substring(0, amount.length() - 1)) * 1000L * 1000L;
            } else if (amount.endsWith("G") || amount.endsWith("g")) {
                return Long.parseLong(amount.substring(0, amount.length() - 1)) * 1000L * 1000L * 1000L;
            } else {
                return Long.parseLong(amount);
            }
        } catch (Exception e) {
            return 1073741824L;
        }
    }

    /**
     * 探测解析 K3s Node Token
     */
    public String resolveK3sToken() {
        String token = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_K3S_TOKEN, "");
        if (StringUtils.hasText(token)) {
            return token.trim();
        }
        try {
            Path p = Path.of("/var/lib/rancher/k3s/server/node-token");
            if (Files.exists(p) && Files.isReadable(p)) {
                String fileToken = Files.readString(p).trim();
                if (StringUtils.hasText(fileToken)) {
                    return fileToken;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

}
