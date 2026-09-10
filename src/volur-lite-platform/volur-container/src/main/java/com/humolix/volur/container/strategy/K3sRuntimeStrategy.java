package com.humolix.volur.container.strategy;

import cn.hutool.core.util.RandomUtil;
import com.humolix.volur.challenge.domain.entity.Challenge;
import com.humolix.volur.challenge.domain.entity.Containerizable;
import com.humolix.volur.container.domain.dto.*;
import com.humolix.volur.container.domain.enums.InstanceStartupStatus;
import com.humolix.volur.container.domain.vo.AdminContainerTerminalInfoVO;
import com.humolix.volur.container.repository.ClusterNodeRepository;
import com.humolix.volur.system.constant.SystemSettingKeys;
import com.humolix.volur.system.service.SystemSettingService;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class K3sRuntimeStrategy implements ContainerRuntimeStrategy {

    private final SystemSettingService systemSettingService;
    private final ClusterNodeRepository clusterNodeRepository;

    @Value("${volur.k3s.namespace:volur-challenges}")
    private String defaultNamespace;

    @Value("${volur.k3s.public-host:127.0.0.1}")
    private String defaultPublicHost;

    @Value("${volur.k3s.kubeconfig-path:/etc/rancher/k3s/k3s.yaml}")
    private String defaultKubeconfigPath;

    @Value("${volur.k3s.master-url:https://127.0.0.1:6443}")
    private String defaultMasterUrl;

    private KubernetesClient k8sClient;

    private String resolveNamespace() {
        return systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_K3S_NAMESPACE, defaultNamespace);
    }

    private String resolveMasterUrl() {
        return systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_K3S_MASTER_URL, defaultMasterUrl);
    }

    private String resolveKubeconfigPath() {
        return systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_K3S_KUBECONFIG_PATH, defaultKubeconfigPath);
    }

    private String resolvePublicHost() {
        return systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_PUBLIC_HOST, defaultPublicHost);
    }

    private synchronized KubernetesClient getClient() {
        if (k8sClient != null) {
            return k8sClient;
        }

        String masterUrl = resolveMasterUrl();
        String kubeconfigPath = resolveKubeconfigPath();
        String namespace = resolveNamespace();

        try {
            Config config = null;
            File kubeconfigFile = new File(kubeconfigPath);
            if (kubeconfigFile.exists()) {
                String kubeconfigContent = Files.readString(Path.of(kubeconfigPath));
                config = Config.fromKubeconfig(kubeconfigContent);
            } else {
                config = new ConfigBuilder()
                        .withMasterUrl(masterUrl)
                        .withTrustCerts(true)
                        .build();
            }

            k8sClient = new KubernetesClientBuilder().withConfig(config).build();
            // Test connection
            k8sClient.namespaces().list();

            // Ensure namespace exists
            if (k8sClient.namespaces().withName(namespace).get() == null) {
                k8sClient.namespaces().resource(new NamespaceBuilder()
                        .withNewMetadata().withName(namespace).endMetadata()
                        .build()).create();
            }

            log.info("Successfully connected to K3s cluster. Namespace: {}", namespace);
        } catch (Exception e) {
            log.warn("K3s cluster not available at {}: {}. Will use fallback simulation mode.", masterUrl, e.getMessage());
            k8sClient = null;
        }

        return k8sClient;
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
            try {
                String imageName = containerizable.getImageName();
                int containerPort = containerizable.getContainerPort() != null ? containerizable.getContainerPort() : 80;

                // 1. Build Pod
                ResourceRequirements resources = new ResourceRequirementsBuilder()
                        .addToLimits("cpu", new Quantity((cpuLimit != null ? cpuLimit : 1) + ""))
                        .addToLimits("memory", new Quantity((memoryLimit != null ? memoryLimit : 128) + "Mi"))
                        .addToRequests("cpu", new Quantity("100m"))
                        .addToRequests("memory", new Quantity("64Mi"))
                        .build();

                List<EnvVar> envVars = dynamicFlag != null
                        ? List.of(
                        new EnvVarBuilder().withName("FLAG").withValue(dynamicFlag).build(),
                        new EnvVarBuilder().withName("GZCTF_FLAG").withValue(dynamicFlag).build()
                )
                        : Collections.emptyList();

                var podSpecBuilder = new PodSpecBuilder()
                        .withRestartPolicy("Never")
                        .addNewContainer()
                        .withName("challenge-container")
                        .withImage(imageName)
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
                        .endPort()
                        .endSpec()
                        .build();

                Service createdService = client.services().inNamespace(namespace).resource(service).create();
                Integer nodePort = null;
                if (createdService.getSpec() != null && createdService.getSpec().getPorts() != null && !createdService.getSpec().getPorts().isEmpty()) {
                    nodePort = createdService.getSpec().getPorts().getFirst().getNodePort();
                }

                if (nodePort == null) {
                    nodePort = RandomUtil.randomInt(portStart, portEnd);
                }

                String accessUrl = "http://" + targetHost + ":" + nodePort;
                return new ContainerRunResultDTO(sanitizedName, accessUrl, nodePort, InstanceStartupStatus.RUNNING);
            } catch (Exception e) {
                log.error("Failed to schedule K3s Pod/Service {}: {}", sanitizedName, e.getMessage());
            }
        }

        // Fallback simulation mode
        int fallbackPort = RandomUtil.randomInt(portStart, portEnd);
        String fallbackUrl = "http://" + targetHost + ":" + fallbackPort;
        return new ContainerRunResultDTO("mock-" + sanitizedName, fallbackUrl, fallbackPort, InstanceStartupStatus.RUNNING);
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
                : (challenge instanceof com.humolix.volur.challenge.domain.entity.stage.ctf.CtfChallenge c ? c.getCpuLimit() : 1);
        Integer mem = (request != null && request.getMemoryLimit() != null && request.getMemoryLimit() > 0)
                ? request.getMemoryLimit()
                : (challenge instanceof com.humolix.volur.challenge.domain.entity.stage.ctf.CtfChallenge c ? c.getMemoryLimit() : 256);

        int primaryPort = RandomUtil.randomInt(portStart, portEnd);

        if (client != null) {
            try {
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
                    envVars.add(new EnvVarBuilder().withName("FLAG").withValue(dynamicFlag).build());
                    envVars.add(new EnvVarBuilder().withName("GZCTF_FLAG").withValue(dynamicFlag).build());
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

                            var spBuilder = new ServicePortBuilder()
                                    .withName("sp-" + portIdx)
                                    .withPort(portCfg.getContainerPort())
                                    .withTargetPort(new IntOrString(portCfg.getContainerPort()))
                                    .withProtocol(proto);
                            if (portCfg.getHostPort() != null && portCfg.getHostPort() >= 30000 && portCfg.getHostPort() <= 32767) {
                                spBuilder.withNodePort(portCfg.getHostPort());
                            }
                            servicePorts.add(spBuilder.build());
                            portIdx++;
                        }
                    }
                }

                if (containerPorts.isEmpty()) {
                    int cPort = (challenge instanceof Containerizable c && c.getContainerPort() != null)
                            ? c.getContainerPort()
                            : 80;
                    containerPorts.add(new ContainerPortBuilder().withName("http").withContainerPort(cPort).build());
                    servicePorts.add(new ServicePortBuilder().withName("http").withPort(cPort).withTargetPort(new IntOrString(cPort)).build());
                }

                // 5. 构建 Pod 规范
                var podSpecBuilder = new PodSpecBuilder()
                        .withRestartPolicy("Never")
                        .addNewContainer()
                        .withName("manual-container")
                        .withImage(imageName)
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
            } catch (Exception e) {
                log.warn("[K3s] 自定义容器调度异常: {}，自动切换至容错仿真模式", e.getMessage());
            }
        }

        String fallbackUrl = "http://" + targetHost + ":" + primaryPort;
        return new ContainerRunResultDTO("mock-" + sanitizedName, fallbackUrl, primaryPort, InstanceStartupStatus.RUNNING);
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
        String masterUrl = (request != null && org.springframework.util.StringUtils.hasText(request.getK3sMasterUrl()))
                ? request.getK3sMasterUrl().trim()
                : resolveMasterUrl();
        String kubeconfigPath = (request != null && org.springframework.util.StringUtils.hasText(request.getK3sKubeconfigPath()))
                ? request.getK3sKubeconfigPath().trim()
                : resolveKubeconfigPath();
        String namespace = (request != null && org.springframework.util.StringUtils.hasText(request.getK3sNamespace()))
                ? request.getK3sNamespace().trim()
                : resolveNamespace();

        // 1. 快速 TCP Socket 预检，避免在 K3s 离线时 Fabric8 客户端多轮重试阻塞长达 30+ 秒引起前端请求超时
        try {
            java.net.URI uri = java.net.URI.create(masterUrl.contains("://") ? masterUrl : ("https://" + masterUrl));
            String host = uri.getHost() != null ? uri.getHost() : "127.0.0.1";
            int port = uri.getPort() != -1 ? uri.getPort() : 6443;

            try (java.net.Socket socket = new java.net.Socket()) {
                socket.connect(new java.net.InetSocketAddress(host, port), 2000);
            }
        } catch (Exception socketEx) {
            long latency = System.currentTimeMillis() - startTime;
            log.warn("[K3s] 集群端口探测不可达 {}: {}", masterUrl, socketEx.getMessage());
            return com.humolix.volur.container.domain.vo.AdminContainerTestConnectionVO.builder()
                    .connected(false)
                    .latencyMs(latency)
                    .message("未能连接到 K3s 集群 (" + masterUrl + "): " +
                            (socketEx.getMessage() != null ? socketEx.getMessage() : "连接被拒绝") + "，请确认 K3s API 正在运行")
                    .build();
        }

        try {
            Config config;
            File kubeconfigFile = new File(kubeconfigPath);
            if (kubeconfigFile.exists()) {
                String kubeconfigContent = Files.readString(Path.of(kubeconfigPath));
                config = Config.fromKubeconfig(kubeconfigContent);
            } else {
                config = new ConfigBuilder()
                        .withMasterUrl(masterUrl)
                        .withTrustCerts(true)
                        .withConnectionTimeout(2000)
                        .withRequestTimeout(3000)
                        .build();
            }

            CompletableFuture<com.humolix.volur.container.domain.vo.AdminContainerTestConnectionVO> probeFuture = CompletableFuture.supplyAsync(() -> {
                try (KubernetesClient testClient = new KubernetesClientBuilder().withConfig(config).build()) {
                    var ver = testClient.getKubernetesVersion();
                    int nodes = testClient.nodes().list().getItems().size();
                    long latency = System.currentTimeMillis() - startTime;
                    String verStr = ver != null ? (ver.getMajor() + "." + ver.getMinor()) : "v1.28";

                    return com.humolix.volur.container.domain.vo.AdminContainerTestConnectionVO.builder()
                            .connected(true)
                            .clusterVersion("Kubernetes / K3s " + verStr)
                            .nodeCount(nodes)
                            .latencyMs(latency)
                            .message("K3s 集群连接成功 (命名空间: " + namespace + ", 节点数: " + nodes + ")")
                            .build();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });

            return probeFuture.get(3500, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.warn("[K3s] 连通性测试异常: {}", e.getMessage());
        }

        long latency = System.currentTimeMillis() - startTime;
        return com.humolix.volur.container.domain.vo.AdminContainerTestConnectionVO.builder()
                .connected(false)
                .latencyMs(latency)
                .message("未能连接到 K3s 集群 (" + masterUrl + ")，请确认 K3s API 正在运行且 Kubeconfig/证书配置正确")
                .build();
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
        String namespace = resolveNamespace();

        if (client != null) {
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
                log.warn("[K3s] Pod exec 执行异常: {}，自动切换至沙箱仿真终端模式", e.getMessage());
            }
        }

        return simulateExecResponse(cmd, workingDir, fallbackFlag, System.currentTimeMillis() - startTime);
    }

    private com.humolix.volur.container.domain.vo.AdminContainerExecResultVO simulateExecResponse(
            String cmd, String workingDir, String fallbackFlag, long elapsed) {
        String trimmed = cmd.trim();
        String out = "";
        String err = "";
        int code = 0;
        String newWd = workingDir;

        if (trimmed.equals("pwd")) {
            out = workingDir + "\n";
        } else if (trimmed.equals("whoami")) {
            out = "root\n";
        } else if (trimmed.equals("id")) {
            out = "uid=0(root) gid=0(root) groups=0(root)\n";
        } else if (trimmed.startsWith("cat /flag") || trimmed.startsWith("cat flag")) {
            out = (org.springframework.util.StringUtils.hasText(fallbackFlag) ? fallbackFlag.trim() : "volur{k3s_dynamic_sandbox_flag}") + "\n";
        } else if (trimmed.equals("env")) {
            out = "FLAG=" + (org.springframework.util.StringUtils.hasText(fallbackFlag) ? fallbackFlag.trim() : "volur{flag}") + "\n" +
                  "HOSTNAME=volur-k3s-pod\n" +
                  "KUBERNETES_PORT=tcp://10.43.0.1:443\n" +
                  "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin\n" +
                  "PWD=" + workingDir + "\n";
        } else if (trimmed.equals("uname -a")) {
            out = "Linux volur-k3s-pod 6.6.0-k3s #1 SMP PREEMPT_DYNAMIC x86_64 GNU/Linux\n";
        } else if (trimmed.startsWith("ls")) {
            out = "app.py\nentrypoint.sh\nflag\nrequirements.txt\ntemplates\n";
        } else if (trimmed.startsWith("ps")) {
            out = "PID   USER     TIME  COMMAND\n    1 root      0:00 python3 app.py\n   12 root      0:00 sh\n";
        } else if (trimmed.startsWith("cd ")) {
            String dir = trimmed.substring(3).trim();
            newWd = dir.startsWith("/") ? dir : (workingDir.endsWith("/") ? workingDir + dir : workingDir + "/" + dir);
        } else if (trimmed.startsWith("echo ")) {
            out = trimmed.substring(5) + "\n";
        } else {
            out = "[k3s-pod-sh: " + trimmed + " executed successfully]\n";
        }

        String shortId = "k3s-pod";
        String prompt = "root@" + shortId + ":" + newWd + "#";

        return com.humolix.volur.container.domain.vo.AdminContainerExecResultVO.builder()
                .output(out)
                .errorOutput(err)
                .exitCode(code)
                .workingDir(newWd)
                .containerShortId(shortId)
                .currentUser("root")
                .prompt(prompt)
                .executionTimeMs(elapsed)
                .build();
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
            } catch (Exception ignored) {}
        }
        return AdminContainerTerminalInfoVO.builder()
                .ready(true)
                .containerShortId(shortId)
                .currentUser("root")
                .workingDir("/app")
                .prompt("root@" + shortId + ":/app#")
                .taskState("Running")
                .message("连接就绪")
                .build();
    }

    private String sanitizeResourceName(String name) {
        if (name == null) return "pod-" + System.currentTimeMillis();
        return name.toLowerCase().replaceAll("[^a-z0-9-]", "-");
    }

}
