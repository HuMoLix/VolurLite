package com.humolix.volur.container.strategy;

import com.humolix.volur.challenge.domain.entity.Challenge;
import com.humolix.volur.challenge.domain.entity.Containerizable;
import com.humolix.volur.container.domain.dto.*;
import com.humolix.volur.container.domain.entity.ClusterNode;
import com.humolix.volur.container.domain.enums.InstanceStartupStatus;
import com.humolix.volur.container.domain.vo.AdminContainerTerminalInfoVO;
import com.humolix.volur.container.domain.vo.AdminContainerTestConnectionVO;
import com.humolix.volur.container.repository.ClusterNodeRepository;
import com.humolix.volur.container.util.ContainerPortAllocator;
import com.humolix.volur.container.util.DockerTlsUtil;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.system.constant.SystemSettingKeys;
import com.humolix.volur.system.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Docker Swarm 动态靶机集群调度与生命周期策略实现
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DockerSwarmRuntimeStrategy implements ContainerRuntimeStrategy {

    private final SystemSettingService systemSettingService;
    private final ClusterNodeRepository clusterNodeRepository;
    private final ContainerPortAllocator containerPortAllocator;

    @Override
    public ContainerRunResultDTO startContainer(String resourceName, Challenge challenge, String dynamicFlag, Integer cpuLimit, Integer memoryLimit, String targetNodeIp) {
        String sanitizedName = sanitizeServiceName(resourceName);
        String publicHost = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_PUBLIC_HOST, "127.0.0.1");
        String targetHost = StringUtils.hasText(targetNodeIp) ? targetNodeIp.trim() : publicHost;
        int portStart = systemSettingService.getInt(SystemSettingKeys.CONTAINER_PORT_RANGE_START, 20000);
        int portEnd = systemSettingService.getInt(SystemSettingKeys.CONTAINER_PORT_RANGE_END, 30000);
        int targetPort = containerPortAllocator.allocateAvailablePort(targetHost, portStart, portEnd, Collections.emptySet());

        if (challenge instanceof Containerizable containerizable) {
            String imageName = containerizable.getImageName();
            int containerPort = containerizable.getContainerPort() != null ? containerizable.getContainerPort() : 80;
            String rawNetwork = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_DOCKER_SWARM_NETWORK, "volur-swarm-network");
            String network = StringUtils.hasText(rawNetwork) ? rawNetwork.trim() : "volur-swarm-network";
            String swarmApiUrl = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_DOCKER_SWARM_API_URL, "unix:///var/run/docker.sock");
            try {
                ensureOverlayNetwork(network, swarmApiUrl);

                // 构建 docker service create 命令参数
                List<String> command = new ArrayList<>();
                command.add("docker");
                if (StringUtils.hasText(swarmApiUrl) && !swarmApiUrl.startsWith("unix://")) {
                    command.add("-H");
                    command.add(swarmApiUrl.trim());
                }
                command.add("service");
                command.add("create");
                command.add("--detach=true");
                command.add("--no-resolve-image");
                command.add("--name");
                command.add(sanitizedName);
                command.add("--replicas");
                command.add("1");
                command.add("--publish");
                command.add("published=" + targetPort + ",target=" + containerPort + ",protocol=tcp,mode=host");

                if (StringUtils.hasText(network)) {
                    command.add("--network");
                    command.add(network.trim());
                }

                // 调度节点约束：优先定向绑定指定目标节点
                String nodeConstraint = resolveSwarmNodeConstraint(targetNodeIp, swarmApiUrl);
                if (StringUtils.hasText(nodeConstraint)) {
                    command.add("--constraint");
                    command.add(nodeConstraint);
                } else {
                    boolean resourceIsolation = systemSettingService.getBoolean(SystemSettingKeys.CONTAINER_RESOURCE_ISOLATION_ENABLED, true);
                    if (resourceIsolation && clusterNodeRepository != null && clusterNodeRepository.countByRole("WORKER") > 0) {
                        command.add("--constraint");
                        command.add("node.role == worker");
                    }
                }

                if (cpuLimit != null && cpuLimit > 0) {
                    command.add("--limit-cpu");
                    command.add(String.valueOf(cpuLimit));
                }

                if (memoryLimit != null && memoryLimit > 0) {
                    command.add("--limit-memory");
                    command.add(memoryLimit + "M");
                }

                if (StringUtils.hasText(dynamicFlag)) {
                    command.add("--env");
                    command.add("volurflag=" + dynamicFlag);
                }

                command.add(imageName);

                log.info("[DockerSwarm] 正在通过 Docker Swarm 编排服务: {} (镜像: {}, 目标节点: {}, 端口: {}->{})",
                        sanitizedName, imageName, targetHost, targetPort, containerPort);

                ProcessBuilder pb = new ProcessBuilder(command);
                pb.redirectErrorStream(true);
                Process process = pb.start();

                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                }

                boolean finished = process.waitFor(8, TimeUnit.SECONDS);
                if (finished && process.exitValue() == 0) {
                    String accessUrl = "http://" + targetHost + ":" + targetPort;
                    log.info("[DockerSwarm] 动态靶机服务创建成功: id={}, url={}, output={}",
                            sanitizedName, accessUrl, output.toString().trim());
                    InstanceStartupStatus initialStatus = waitForServiceReady(sanitizedName, swarmApiUrl, 3500);
                    return new ContainerRunResultDTO(sanitizedName, accessUrl, targetPort, initialStatus);
                } else {
                    String errOutput = output.toString().trim();
                    log.error("[DockerSwarm] 集群环境命令执行未就绪 (exit={}): {}",
                            finished ? process.exitValue() : "timeout", errOutput);
                    throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "Docker Swarm 靶机服务启动失败: " + errOutput);
                }
            } catch (BusinessException be) {
                throw be;
            } catch (Exception e) {
                log.error("[DockerSwarm] 尝试调度 Swarm 集群异常: {}", e.getMessage(), e);
                throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "Docker Swarm 调度异常: " + e.getMessage());
            }
        }

        throw new BusinessException(HttpStatus.BAD_REQUEST, "题目未实现 Containerizable 接口，无法启动动态容器");
    }

    @Override
    public ContainerRunResultDTO startManualContainer(AdminContainerManualStartDTO request, String resourceName, Challenge challenge, String dynamicFlag) {
        String sanitizedName = sanitizeServiceName(resourceName);
        String publicHost = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_PUBLIC_HOST, "127.0.0.1");
        int portStart = systemSettingService.getInt(SystemSettingKeys.CONTAINER_PORT_RANGE_START, 20000);
        int portEnd = systemSettingService.getInt(SystemSettingKeys.CONTAINER_PORT_RANGE_END, 30000);

        String imageName = (request != null && StringUtils.hasText(request.getImageName()))
                ? request.getImageName().trim()
                : (challenge instanceof Containerizable c ? c.getImageName() : "");

        if (!StringUtils.hasText(imageName)) {
            throw new IllegalArgumentException("必须指定运行的 Docker 镜像名称");
        }

        String targetNodeIp = (request != null && StringUtils.hasText(request.getNodeIp())) ? request.getNodeIp().trim() : null;
        String targetHost = StringUtils.hasText(targetNodeIp) ? targetNodeIp : publicHost;

        int primaryPort = 0;
        List<String> publishArgs = new ArrayList<>();
        if (request != null && request.getPorts() != null && !request.getPorts().isEmpty()) {
            boolean isFirst = true;
            for (ContainerPortMappingDTO portCfg : request.getPorts()) {
                if (portCfg.getContainerPort() != null && portCfg.getContainerPort() > 0) {
                    int hPort;
                    if (portCfg.getHostPort() != null && portCfg.getHostPort() > 0) {
                        containerPortAllocator.verifyPortAvailable(targetHost, portCfg.getHostPort(), Collections.emptySet());
                        hPort = portCfg.getHostPort();
                    } else {
                        hPort = containerPortAllocator.allocateAvailablePort(targetHost, portStart, portEnd, Collections.emptySet());
                        portCfg.setHostPort(hPort);
                    }
                    if (isFirst) {
                        primaryPort = hPort;
                        isFirst = false;
                    }
                    String proto = StringUtils.hasText(portCfg.getProtocol()) ? portCfg.getProtocol().trim().toLowerCase() : "tcp";
                    publishArgs.add("--publish");
                    publishArgs.add("published=" + hPort + ",target=" + portCfg.getContainerPort() + ",protocol=" + proto + ",mode=host");
                }
            }
        } else {
            int containerPort = (challenge instanceof Containerizable c && c.getContainerPort() != null)
                    ? c.getContainerPort()
                    : 80;
            primaryPort = containerPortAllocator.allocateAvailablePort(targetHost, portStart, portEnd, Collections.emptySet());
            publishArgs.add("--publish");
            publishArgs.add("published=" + primaryPort + ",target=" + containerPort + ",protocol=tcp,mode=host");
            if (request != null) {
                request.setPorts(new ArrayList<>(List.of(new ContainerPortMappingDTO(primaryPort, containerPort, "TCP"))));
            }
        }

        String rawNetwork = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_DOCKER_SWARM_NETWORK, "volur-swarm-network");
        String network = StringUtils.hasText(rawNetwork) ? rawNetwork.trim() : "volur-swarm-network";
        String swarmApiUrl = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_DOCKER_SWARM_API_URL, "unix:///var/run/docker.sock");

        ensureOverlayNetwork(network, swarmApiUrl);

        try {
            List<String> command = new ArrayList<>();
            command.add("docker");
            if (StringUtils.hasText(swarmApiUrl) && !swarmApiUrl.startsWith("unix://")) {
                command.add("-H");
                command.add(swarmApiUrl.trim());
            }
            command.add("service");
            command.add("create");
            command.add("--detach=true");
            command.add("--no-resolve-image");
            command.add("--name");
            command.add(sanitizedName);
            command.add("--replicas");
            command.add("1");

            // 端口映射
            command.addAll(publishArgs);

            // 覆盖网络
            if (StringUtils.hasText(network)) {
                command.add("--network");
                command.add(network.trim());
            }

            // 数据卷挂载 (Volumes)
            if (request != null && request.getVolumes() != null) {
                for (ContainerVolumeMappingDTO vol : request.getVolumes()) {
                    if (StringUtils.hasText(vol.getHostPath()) && StringUtils.hasText(vol.getContainerPath())) {
                        String readonly = "ro".equalsIgnoreCase(vol.getMode()) ? ",readonly" : "";
                        command.add("--mount");
                        command.add("type=bind,source=" + vol.getHostPath().trim() + ",target=" + vol.getContainerPath().trim() + readonly);
                    }
                }
            }

            // 环境变量 (Environment variables)
            if (request != null && request.getEnvironments() != null) {
                for (ContainerEnvVarDTO env : request.getEnvironments()) {
                    if (StringUtils.hasText(env.getName())) {
                        command.add("--env");
                        command.add(env.getName().trim() + "=" + (env.getValue() != null ? env.getValue() : ""));
                    }
                }
            }

            if (StringUtils.hasText(dynamicFlag)) {
                command.add("--env");
                command.add("volurflag=" + dynamicFlag);
            }

            // 调度节点约束：优先定向绑定指定目标节点
            String nodeConstraint = resolveSwarmNodeConstraint(targetNodeIp, swarmApiUrl);
            if (StringUtils.hasText(nodeConstraint)) {
                command.add("--constraint");
                command.add(nodeConstraint);
            } else {
                boolean resourceIsolation = systemSettingService.getBoolean(SystemSettingKeys.CONTAINER_RESOURCE_ISOLATION_ENABLED, true);
                if (resourceIsolation && clusterNodeRepository != null && clusterNodeRepository.countByRole("WORKER") > 0) {
                    command.add("--constraint");
                    command.add("node.role == worker");
                }
            }

            // CPU 与 内存限制
            Integer cpu = (request != null && request.getCpuLimit() != null && request.getCpuLimit() > 0)
                    ? request.getCpuLimit()
                    : (challenge instanceof com.humolix.volur.challenge.domain.entity.ctf.CtfChallenge c ? c.getCpuLimit() : 1);
            Integer mem = (request != null && request.getMemoryLimit() != null && request.getMemoryLimit() > 0)
                    ? request.getMemoryLimit()
                    : (challenge instanceof com.humolix.volur.challenge.domain.entity.ctf.CtfChallenge c ? c.getMemoryLimit() : 256);

            if (cpu != null && cpu > 0) {
                command.add("--limit-cpu");
                command.add(String.valueOf(cpu));
            }
            if (mem != null && mem > 0) {
                command.add("--limit-memory");
                command.add(mem + "M");
            }

            command.add(imageName);

            log.info("[DockerSwarm] 正在通过 Docker Swarm 启动自定义容器: {} (镜像: {}, 目标节点: {}, 端口: {})",
                    sanitizedName, imageName, targetHost, primaryPort);
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(8, TimeUnit.SECONDS);
            if (finished && process.exitValue() == 0) {
                String accessUrl = "http://" + targetHost + ":" + primaryPort;
                log.info("[DockerSwarm] 自定义容器服务创建成功: id={}, url={}, output={}",
                        sanitizedName, accessUrl, output.toString().trim());
                InstanceStartupStatus initialStatus = waitForServiceReady(sanitizedName, swarmApiUrl, 3500);
                return new ContainerRunResultDTO(sanitizedName, accessUrl, primaryPort, initialStatus);
            } else {
                String errOutput = output.toString().trim();
                log.error("[DockerSwarm] Swarm 启动命令失败 (exit={}): {}",
                        finished ? process.exitValue() : "timeout", errOutput);
                throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "Docker Swarm 自定义容器启动失败: " + errOutput);
            }
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            log.error("[DockerSwarm] 启动自定义容器异常: {}", e.getMessage(), e);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "Docker Swarm 启动异常: " + e.getMessage());
        }
    }

    private String resolveSwarmNodeConstraint(String targetNodeIp, String swarmApiUrl) {
        if (!StringUtils.hasText(targetNodeIp)) {
            return null;
        }
        String cleanIp = targetNodeIp.trim();
        String publicHost = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_PUBLIC_HOST, "127.0.0.1");

        // 1. 如果为本地主节点
        if ("127.0.0.1".equals(cleanIp) || "localhost".equalsIgnoreCase(cleanIp) || publicHost.equalsIgnoreCase(cleanIp)) {
            return "node.role == manager";
        }

        String expectedClusterNodeId = null;
        String expectedNodeName = null;
        if (clusterNodeRepository != null) {
            var nodeOpt = clusterNodeRepository.findByIpAddress(cleanIp);
            if (nodeOpt.isPresent()) {
                var node = nodeOpt.get();
                if ("MASTER".equalsIgnoreCase(node.getRole())) {
                    return "node.role == manager";
                }
                expectedClusterNodeId = node.getClusterNodeId();
                expectedNodeName = node.getNodeName();
            }
        }

        // 2. 通过 docker node ls 查询 Swarm 集群节点，严格匹配目标节点的 Swarm 标识与活跃状态
        boolean foundInSwarm = false;
        String matchedConstraint = null;
        try {
            List<String> listCmd = new ArrayList<>();
            listCmd.add("docker");
            if (StringUtils.hasText(swarmApiUrl) && !swarmApiUrl.startsWith("unix://")) {
                listCmd.add("-H");
                listCmd.add(swarmApiUrl.trim());
            }
            listCmd.add("node");
            listCmd.add("ls");
            listCmd.add("--format");
            listCmd.add("{{.ID}}\t{{.Hostname}}\t{{.Status}}");

            ProcessBuilder pb = new ProcessBuilder(listCmd);
            Process p = pb.start();
            if (p.waitFor(5, TimeUnit.SECONDS) && p.exitValue() == 0) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!StringUtils.hasText(line)) continue;
                        String[] parts = line.split("\t");
                        String nodeId = parts.length > 0 ? parts[0].trim() : "";
                        String hostname = parts.length > 1 ? parts[1].trim() : "";
                        String status = parts.length > 2 ? parts[2].trim() : "";

                        // 核验 Swarm 节点的底层连接 IP 地址
                        String nodeAddr = querySwarmNodeAddr(nodeId, swarmApiUrl);
                        boolean isMatch = cleanIp.equalsIgnoreCase(nodeAddr)
                                || cleanIp.equalsIgnoreCase(hostname)
                                || cleanIp.equalsIgnoreCase(nodeId)
                                || (StringUtils.hasText(expectedClusterNodeId) && expectedClusterNodeId.equalsIgnoreCase(nodeId))
                                || (StringUtils.hasText(expectedNodeName) && expectedNodeName.equalsIgnoreCase(hostname));

                        if (isMatch) {
                            foundInSwarm = true;
                            if (!"Ready".equalsIgnoreCase(status)) {
                                throw new BusinessException(HttpStatus.BAD_REQUEST, "目标工作节点 (" + cleanIp + ") 在 Swarm 集群中处于离线或非就绪状态 (" + status + ")，无法调度运行容器");
                            }
                            matchedConstraint = "node.id == " + nodeId;
                            break;
                        }
                    }
                }
            }
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            log.debug("[DockerSwarm] 探测 Swarm 节点标识提示: {}", e.getMessage());
        }

        if (foundInSwarm && matchedConstraint != null) {
            return matchedConstraint;
        }

        // 3. 从 ClusterNode 表中查询该节点详细信息，给出精准且具建设性的诊断拦截
        String nodeDisplayName = cleanIp;
        if (StringUtils.hasText(expectedNodeName)) {
            nodeDisplayName = expectedNodeName + " (" + cleanIp + ")";
        }

        throw new BusinessException(HttpStatus.BAD_REQUEST, "目标工作节点 [" + nodeDisplayName + "] 尚未成功加入 Docker Swarm 集群 (或处于非就绪状态)。请在集群管理中检查网络互通并确保节点成功执行 docker swarm join 进入集群！");
    }

    private String querySwarmNodeAddr(String nodeId, String swarmApiUrl) {
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add("docker");
            if (StringUtils.hasText(swarmApiUrl) && !swarmApiUrl.startsWith("unix://")) {
                cmd.add("-H");
                cmd.add(swarmApiUrl.trim());
            }
            cmd.add("node");
            cmd.add("inspect");
            cmd.add(nodeId);
            cmd.add("--format");
            cmd.add("{{.Status.Addr}}");

            ProcessBuilder pb = new ProcessBuilder(cmd);
            Process p = pb.start();
            if (p.waitFor(3, TimeUnit.SECONDS) && p.exitValue() == 0) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                    String line = reader.readLine();
                    if (StringUtils.hasText(line)) {
                        return line.trim();
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    @Override
    public void stopContainer(String resourceName) {
        if (!StringUtils.hasText(resourceName)) {
            return;
        }
        String sanitizedName = sanitizeServiceName(resourceName);
        String swarmApiUrl = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_DOCKER_SWARM_API_URL, "unix:///var/run/docker.sock");

        try {
            List<String> command = new ArrayList<>();
            command.add("docker");
            if (StringUtils.hasText(swarmApiUrl) && !swarmApiUrl.startsWith("unix://")) {
                command.add("-H");
                command.add(swarmApiUrl.trim());
            }
            command.add("service");
            command.add("rm");
            command.add(sanitizedName);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            process.waitFor(10, TimeUnit.SECONDS);
            log.info("[DockerSwarm] 已提交清理 Swarm 动态服务: {}", sanitizedName);
        } catch (Exception e) {
            log.warn("[DockerSwarm] 清理 Swarm 服务 {} 异常: {}", sanitizedName, e.getMessage());
        }
    }

    @Override
    public AdminContainerTestConnectionVO testConnection(
            AdminContainerTestConnectionDTO request) {
        long startTime = System.currentTimeMillis();
        String swarmApiUrl = (request != null && StringUtils.hasText(request.getDockerSwarmApiUrl()))
                ? request.getDockerSwarmApiUrl().trim()
                : systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_DOCKER_SWARM_API_URL, "unix:///var/run/docker.sock");

        try {
            List<String> command = new ArrayList<>();
            command.add("docker");
            if (StringUtils.hasText(swarmApiUrl) && !swarmApiUrl.startsWith("unix://")) {
                command.add("-H");
                command.add(swarmApiUrl);
            }
            command.add("info");
            command.add("--format");
            command.add("{{.ServerVersion}}|{{.Swarm.LocalNodeState}}|{{.Swarm.Nodes}}");

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            boolean finished = process.waitFor(6, TimeUnit.SECONDS);
            long latency = System.currentTimeMillis() - startTime;
            if (finished && process.exitValue() == 0) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line = reader.readLine();
                    if (StringUtils.hasText(line)) {
                        String[] parts = line.split("\\|");
                        String version = parts.length > 0 ? parts[0] : "Unknown";
                        String state = parts.length > 1 ? parts[1] : "unknown";
                        int nodes = (parts.length > 2 && parts[2].matches("\\d+")) ? Integer.parseInt(parts[2]) : 1;
                        boolean isSwarmActive = "active".equalsIgnoreCase(state);

                        return com.humolix.volur.container.domain.vo.AdminContainerTestConnectionVO.builder()
                                .connected(true)
                                .clusterVersion("Docker Engine " + version)
                                .nodeCount(nodes)
                                .latencyMs(latency)
                                .message(isSwarmActive
                                        ? "Docker Swarm 集群已成功连接 (节点状态: active, 节点数: " + nodes + ")"
                                        : "Docker 守护进程连接正常，但 Swarm 集群当前状态为: " + state)
                                .build();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[DockerSwarm] 连通性测试执行异常: {}", e.getMessage());
        }

        long latency = System.currentTimeMillis() - startTime;
        return com.humolix.volur.container.domain.vo.AdminContainerTestConnectionVO.builder()
                .connected(false)
                .latencyMs(latency)
                .message("未能连接到 Docker Swarm API (" + swarmApiUrl + ")，请确认 Docker 守护进程运行正常且 API 地址配置正确")
                .build();
    }

    @Override
    public com.humolix.volur.container.domain.vo.AdminContainerExecResultVO execCommand(
            String containerId,
            com.humolix.volur.container.domain.dto.AdminContainerExecDTO request,
            String fallbackFlag) {
        long startTime = System.currentTimeMillis();
        String cmd = (request != null && StringUtils.hasText(request.getCommand())) ? request.getCommand().trim() : "id";
        String shell = (request != null && StringUtils.hasText(request.getShell())) ? request.getShell().trim() : "/bin/sh";
        String workingDir = (request != null && StringUtils.hasText(request.getWorkingDir())) ? request.getWorkingDir().trim() : "/app";

        String swarmApiUrl = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_DOCKER_SWARM_API_URL, "unix:///var/run/docker.sock");
        String sanitizedName = sanitizeServiceName(containerId);

        try {
            // 查找属于该服务任务的实际运行容器与所在节点信息 (支持跨节点 Worker 调度)
            SwarmTaskContainerInfo taskInfo = findSwarmTaskContainerInfo(sanitizedName, swarmApiUrl);

            if (taskInfo == null || !StringUtils.hasText(taskInfo.containerId())) {
                // 没有实际运行的容器任务，检查该服务在 Swarm 里的具体任务状态
                String taskState = getServiceTaskStatus(sanitizedName, swarmApiUrl);
                if (StringUtils.hasText(taskState)) {
                    log.warn("[DockerSwarm] 服务 {} 尚未生成运行容器，当前 Swarm 任务状态: {}", sanitizedName, taskState);
                    return com.humolix.volur.container.domain.vo.AdminContainerExecResultVO.builder()
                            .output("")
                            .errorOutput("[Docker Swarm 提示] 容器实例尚未处于运行就绪状态 (Task State: " + taskState + ")。\n"
                                    + "常见原因:\n"
                                    + "1. 镜像在节点 Docker 中不存在，且未能成功拉取 (如镜像名称错误或网络超时)；\n"
                                    + "2. 容器正在启动初始化中，请稍等数秒后重试；\n"
                                    + "3. 可在宿主机执行 `docker service ps " + sanitizedName + "` 查看底层排查日志。")
                            .exitCode(1)
                            .workingDir(workingDir)
                            .executionTimeMs(System.currentTimeMillis() - startTime)
                            .build();
                }

                return com.humolix.volur.container.domain.vo.AdminContainerExecResultVO.builder()
                        .output("")
                        .errorOutput("未找到属于该服务的运行中容器任务，服务尚未就绪或已停止")
                        .exitCode(1)
                        .workingDir(workingDir)
                        .executionTimeMs(System.currentTimeMillis() - startTime)
                        .build();
            }

            String actualContainerId = taskInfo.containerId();
            String shortId = actualContainerId.substring(0, Math.min(12, actualContainerId.length()));
            ContainerInspectInfo inspect = inspectContainerConfig(taskInfo, swarmApiUrl);
            String user = StringUtils.hasText(inspect.user()) ? inspect.user() : "root";
            String currentWd = StringUtils.hasText(workingDir) ? workingDir : (StringUtils.hasText(inspect.workingDir()) ? inspect.workingDir() : "/app");

            String execShellCmd = cmd;
            boolean isCd = cmd.trim().startsWith("cd ") || "cd".equals(cmd.trim());
            if (isCd) {
                String targetDir = cmd.trim().length() > 2 ? cmd.trim().substring(2).trim() : "~";
                execShellCmd = "cd " + targetDir + " && pwd";
            }

            List<String> execCmd = buildDockerCliCommand(taskInfo, swarmApiUrl);
            execCmd.add("exec");
            execCmd.add("-i");
            if (StringUtils.hasText(currentWd)) {
                execCmd.add("-w");
                execCmd.add(currentWd);
            }
            execCmd.add(actualContainerId);
            execCmd.add(shell);
            execCmd.add("-c");
            execCmd.add(execShellCmd);

            ProcessBuilder execPb = new ProcessBuilder(execCmd);
            Process execProc = execPb.start();

            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();

            Thread tOut = new Thread(() -> {
                try (BufferedReader r = new BufferedReader(new InputStreamReader(execProc.getInputStream(), StandardCharsets.UTF_8))) {
                    String l;
                    while ((l = r.readLine()) != null) stdout.append(l).append("\n");
                } catch (Exception ignored) {}
            });
            Thread tErr = new Thread(() -> {
                try (BufferedReader r = new BufferedReader(new InputStreamReader(execProc.getErrorStream(), StandardCharsets.UTF_8))) {
                    String l;
                    while ((l = r.readLine()) != null) stderr.append(l).append("\n");
                } catch (Exception ignored) {}
            });
            tOut.start();
            tErr.start();

            boolean execFinished = execProc.waitFor(10, TimeUnit.SECONDS);
            tOut.join(1000);
            tErr.join(1000);

            if (execFinished) {
                long elapsed = System.currentTimeMillis() - startTime;
                String finalWd = currentWd;
                String outStr = stdout.toString();
                if (isCd && execProc.exitValue() == 0 && StringUtils.hasText(outStr)) {
                    finalWd = outStr.trim();
                    outStr = "";
                }
                String symbol = "root".equals(user) ? "#" : "$";
                String prompt = user + "@" + shortId + ":" + finalWd + symbol;

                return com.humolix.volur.container.domain.vo.AdminContainerExecResultVO.builder()
                        .output(outStr)
                        .errorOutput(stderr.toString())
                        .exitCode(execProc.exitValue())
                        .workingDir(finalWd)
                        .containerShortId(shortId)
                        .currentUser(user)
                        .prompt(prompt)
                        .executionTimeMs(elapsed)
                        .build();
            }
        } catch (Exception e) {
            log.warn("[DockerSwarm] exec 执行异常: {}", e.getMessage());
            return com.humolix.volur.container.domain.vo.AdminContainerExecResultVO.builder()
                    .output("")
                    .errorOutput("Docker exec 执行异常: " + e.getMessage())
                    .exitCode(1)
                    .workingDir(workingDir)
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }

        return com.humolix.volur.container.domain.vo.AdminContainerExecResultVO.builder()
                .output("")
                .errorOutput("命令执行超时或未能获取容器响应")
                .exitCode(1)
                .workingDir(workingDir)
                .executionTimeMs(System.currentTimeMillis() - startTime)
                .build();
    }

    private String sanitizeServiceName(String name) {
        if (name == null) return "swarm-svc-" + System.currentTimeMillis();
        String clean = name.toLowerCase().replaceAll("[^a-z0-9_-]", "-");
        if (!clean.startsWith("swarm-")) {
            clean = "swarm-" + clean;
        }
        return clean;
    }

    private void ensureOverlayNetwork(String network, String swarmApiUrl) {
        if (!StringUtils.hasText(network) || "bridge".equalsIgnoreCase(network) || "host".equalsIgnoreCase(network)) {
            return;
        }
        try {
            List<String> inspectCmd = new ArrayList<>();
            inspectCmd.add("docker");
            if (StringUtils.hasText(swarmApiUrl) && !swarmApiUrl.startsWith("unix://")) {
                inspectCmd.add("-H");
                inspectCmd.add(swarmApiUrl.trim());
            }
            inspectCmd.add("network");
            inspectCmd.add("inspect");
            inspectCmd.add(network.trim());

            ProcessBuilder pb = new ProcessBuilder(inspectCmd);
            Process p = pb.start();
            if (p.waitFor(3, TimeUnit.SECONDS) && p.exitValue() == 0) {
                return;
            }

            List<String> createCmd = new ArrayList<>();
            createCmd.add("docker");
            if (StringUtils.hasText(swarmApiUrl) && !swarmApiUrl.startsWith("unix://")) {
                createCmd.add("-H");
                createCmd.add(swarmApiUrl.trim());
            }
            createCmd.add("network");
            createCmd.add("create");
            createCmd.add("--driver");
            createCmd.add("overlay");
            createCmd.add("--attachable");
            createCmd.add(network.trim());

            ProcessBuilder createPb = new ProcessBuilder(createCmd);
            Process createP = createPb.start();
            createP.waitFor(5, TimeUnit.SECONDS);
            log.info("[DockerSwarm] 自动为 Swarm 集群创建 Overlay 基础网络: {}", network);
        } catch (Exception e) {
            log.warn("[DockerSwarm] 检查/创建 Overlay 网络异常: {}", e.getMessage());
        }
    }

    private record SwarmTaskContainerInfo(String containerId, String nodeIp, ClusterNode clusterNode) {}

    private SwarmTaskContainerInfo findSwarmTaskContainerInfo(String serviceName, String swarmApiUrl) {
        // 1. 先从 Swarm 正在运行的任务中获取 Task ID 并 inspect 其对应的 NodeID 与 ContainerID (支持跨节点定位)
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add("docker");
            if (StringUtils.hasText(swarmApiUrl) && !swarmApiUrl.startsWith("unix://")) {
                cmd.add("-H");
                cmd.add(swarmApiUrl.trim());
            }
            cmd.add("service");
            cmd.add("ps");
            cmd.add(serviceName);
            cmd.add("--filter");
            cmd.add("desired-state=running");
            cmd.add("--no-trunc");
            cmd.add("--format");
            cmd.add("{{.ID}}");

            ProcessBuilder pb = new ProcessBuilder(cmd);
            Process p = pb.start();
            if (p.waitFor(5, TimeUnit.SECONDS) && p.exitValue() == 0) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                    String taskId;
                    while ((taskId = reader.readLine()) != null) {
                        if (!StringUtils.hasText(taskId)) continue;
                        SwarmTaskContainerInfo info = inspectSwarmTask(taskId.trim(), swarmApiUrl);
                        if (info != null && StringUtils.hasText(info.containerId())) {
                            return info;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("[DockerSwarm] 查询 Swarm Task 提示: {}", e.getMessage());
        }

        // 2. 本地 docker ps 查询 (用于主节点本地部署容器或单机环境)
        String localId = queryContainerIdByFilter("label=com.docker.swarm.service.name=" + serviceName, swarmApiUrl);
        if (!StringUtils.hasText(localId)) {
            localId = queryContainerIdByFilter("name=" + serviceName, swarmApiUrl);
        }
        if (StringUtils.hasText(localId)) {
            return new SwarmTaskContainerInfo(localId, "127.0.0.1", null);
        }

        return null;
    }

    private SwarmTaskContainerInfo inspectSwarmTask(String taskId, String swarmApiUrl) {
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add("docker");
            if (StringUtils.hasText(swarmApiUrl) && !swarmApiUrl.startsWith("unix://")) {
                cmd.add("-H");
                cmd.add(swarmApiUrl.trim());
            }
            cmd.add("inspect");
            cmd.add(taskId);
            cmd.add("--format");
            cmd.add("{{.NodeID}}:::{{.Status.ContainerStatus.ContainerID}}");

            ProcessBuilder pb = new ProcessBuilder(cmd);
            Process p = pb.start();
            if (p.waitFor(5, TimeUnit.SECONDS) && p.exitValue() == 0) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                    String line = reader.readLine();
                    if (StringUtils.hasText(line) && line.contains(":::")) {
                        String[] parts = line.split(":::", -1);
                        String swarmNodeId = parts.length > 0 ? parts[0].trim() : "";
                        String containerId = parts.length > 1 ? parts[1].trim() : "";

                        if (StringUtils.hasText(containerId)) {
                            ClusterNode matchedNode = findClusterNodeBySwarmNodeId(swarmNodeId);
                            if (matchedNode != null && "WORKER".equalsIgnoreCase(matchedNode.getRole())) {
                                return new SwarmTaskContainerInfo(containerId, matchedNode.getIpAddress(), matchedNode);
                            }
                            return new SwarmTaskContainerInfo(containerId, "127.0.0.1", matchedNode);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("[DockerSwarm] inspect Swarm Task 异常: {}", e.getMessage());
        }
        return null;
    }

    private ClusterNode findClusterNodeBySwarmNodeId(String swarmNodeId) {
        if (!StringUtils.hasText(swarmNodeId) || clusterNodeRepository == null) {
            return null;
        }
        String cleanId = swarmNodeId.trim();

        // 1. 优先通过 cluster_node 表的 clusterNodeId 匹配
        for (ClusterNode node : clusterNodeRepository.findAll()) {
            if (cleanId.equalsIgnoreCase(node.getClusterNodeId())) {
                return node;
            }
        }

        // 2. 如果未直接匹配到，尝试通过 docker node inspect 获取该节点的主机名与地址再次匹配
        String swarmApiUrl = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_DOCKER_SWARM_API_URL, "unix:///var/run/docker.sock");
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add("docker");
            if (StringUtils.hasText(swarmApiUrl) && !swarmApiUrl.startsWith("unix://")) {
                cmd.add("-H");
                cmd.add(swarmApiUrl.trim());
            }
            cmd.add("node");
            cmd.add("inspect");
            cmd.add(cleanId);
            cmd.add("--format");
            cmd.add("{{.Description.Hostname}}:::{{.Status.Addr}}");

            ProcessBuilder pb = new ProcessBuilder(cmd);
            Process p = pb.start();
            if (p.waitFor(3, TimeUnit.SECONDS) && p.exitValue() == 0) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                    String line = reader.readLine();
                    if (StringUtils.hasText(line) && line.contains(":::")) {
                        String[] parts = line.split(":::", -1);
                        String hostname = parts.length > 0 ? parts[0].trim() : "";
                        String addr = parts.length > 1 ? parts[1].trim() : "";

                        for (ClusterNode node : clusterNodeRepository.findAll()) {
                            if (hostname.equalsIgnoreCase(node.getNodeName())
                                    || addr.equalsIgnoreCase(node.getIpAddress())
                                    || hostname.equalsIgnoreCase(node.getIpAddress())) {
                                return node;
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        return null;
    }

    private List<String> buildDockerCliCommand(SwarmTaskContainerInfo info, String swarmApiUrl) {
        String publicHost = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_PUBLIC_HOST, "127.0.0.1");
        String targetHost = (info != null && StringUtils.hasText(info.nodeIp())) ? info.nodeIp() : "127.0.0.1";
        ClusterNode node = (info != null) ? info.clusterNode() : null;
        if (node == null && StringUtils.hasText(targetHost) && clusterNodeRepository != null) {
            node = clusterNodeRepository.findByIpAddress(targetHost.trim()).orElse(null);
        }
        return DockerTlsUtil.buildDockerBaseCliCommand(targetHost, node, publicHost, swarmApiUrl);
    }

    private String queryContainerIdByFilter(String filter, String swarmApiUrl) {
        try {
            List<String> psCmd = new ArrayList<>();
            psCmd.add("docker");
            if (StringUtils.hasText(swarmApiUrl) && !swarmApiUrl.startsWith("unix://")) {
                psCmd.add("-H");
                psCmd.add(swarmApiUrl);
            }
            psCmd.add("ps");
            psCmd.add("--filter");
            psCmd.add(filter);
            psCmd.add("-q");

            ProcessBuilder psPb = new ProcessBuilder(psCmd);
            Process psProcess = psPb.start();
            if (psProcess.waitFor(5, TimeUnit.SECONDS) && psProcess.exitValue() == 0) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(psProcess.getInputStream(), StandardCharsets.UTF_8))) {
                    String line = reader.readLine();
                    if (StringUtils.hasText(line)) {
                        return line.trim();
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String getServiceTaskStatus(String serviceName, String swarmApiUrl) {
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add("docker");
            if (StringUtils.hasText(swarmApiUrl) && !swarmApiUrl.startsWith("unix://")) {
                cmd.add("-H");
                cmd.add(swarmApiUrl);
            }
            cmd.add("service");
            cmd.add("ps");
            cmd.add(serviceName);
            cmd.add("--filter");
            cmd.add("desired-state=running");
            cmd.add("--no-trunc");
            cmd.add("--format");
            cmd.add("{{.CurrentState}} {{.Error}}");

            ProcessBuilder pb = new ProcessBuilder(cmd);
            Process p = pb.start();
            if (p.waitFor(5, TimeUnit.SECONDS) && p.exitValue() == 0) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                    String line = reader.readLine();
                    if (StringUtils.hasText(line)) {
                        return line.trim();
                    }
                }
            }
        } catch (Exception ignored) {}

        // 兜底查询非 running 状态 (如失败或排队原因)
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add("docker");
            if (StringUtils.hasText(swarmApiUrl) && !swarmApiUrl.startsWith("unix://")) {
                cmd.add("-H");
                cmd.add(swarmApiUrl);
            }
            cmd.add("service");
            cmd.add("ps");
            cmd.add(serviceName);
            cmd.add("--no-trunc");
            cmd.add("--format");
            cmd.add("{{.CurrentState}} {{.Error}}");

            ProcessBuilder pb = new ProcessBuilder(cmd);
            Process p = pb.start();
            if (p.waitFor(5, TimeUnit.SECONDS) && p.exitValue() == 0) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                    String line = reader.readLine();
                    if (StringUtils.hasText(line)) {
                        return line.trim();
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    @Override
    public InstanceStartupStatus checkInstanceStatus(String containerId) {
        String sanitizedName = sanitizeServiceName(containerId);
        String swarmApiUrl = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_DOCKER_SWARM_API_URL, "unix:///var/run/docker.sock");
        String taskState = getServiceTaskStatus(sanitizedName, swarmApiUrl);
        if (StringUtils.hasText(taskState)) {
            if (taskState.startsWith("Running")) {
                return InstanceStartupStatus.RUNNING;
            }
            if (taskState.startsWith("Preparing") || taskState.startsWith("Starting") || taskState.startsWith("Allocated") || taskState.startsWith("Assigned") || taskState.startsWith("Ready")) {
                return InstanceStartupStatus.STARTING;
            }
            if (taskState.contains("Failed") || taskState.contains("Rejected") || taskState.contains("No such image")) {
                return InstanceStartupStatus.FAILED;
            }
            if (taskState.startsWith("Shutdown") || taskState.startsWith("Complete")) {
                return InstanceStartupStatus.STOPPED;
            }
        }
        String directId = queryContainerIdByFilter("name=" + sanitizedName, swarmApiUrl);
        if (StringUtils.hasText(directId)) {
            return InstanceStartupStatus.RUNNING;
        }
        return null;
    }

    @Override
    public AdminContainerTerminalInfoVO getTerminalInfo(String containerId, String fallbackFlag) {
        String sanitizedName = sanitizeServiceName(containerId);
        String swarmApiUrl = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_DOCKER_SWARM_API_URL, "unix:///var/run/docker.sock");
        SwarmTaskContainerInfo taskInfo = findSwarmTaskContainerInfo(sanitizedName, swarmApiUrl);

        if (taskInfo == null || !StringUtils.hasText(taskInfo.containerId())) {
            String taskState = getServiceTaskStatus(sanitizedName, swarmApiUrl);
            String stateDesc = StringUtils.hasText(taskState) ? taskState : "Preparing";
            return AdminContainerTerminalInfoVO.builder()
                    .ready(false)
                    .taskState(stateDesc)
                    .message("容器实例尚未就绪 (Task State: " + stateDesc + ")")
                    .build();
        }

        String actualContainerId = taskInfo.containerId();
        ContainerInspectInfo inspect = inspectContainerConfig(taskInfo, swarmApiUrl);
        String shortId = actualContainerId.substring(0, Math.min(12, actualContainerId.length()));
        String user = StringUtils.hasText(inspect.user()) ? inspect.user() : "root";
        String wd = StringUtils.hasText(inspect.workingDir()) ? inspect.workingDir() : "/app";
        String symbol = "root".equals(user) ? "#" : "$";
        String prompt = user + "@" + shortId + ":" + wd + symbol;

        return AdminContainerTerminalInfoVO.builder()
                .ready(true)
                .containerShortId(shortId)
                .currentUser(user)
                .workingDir(wd)
                .prompt(prompt)
                .taskState("Running")
                .message("连接就绪")
                .build();
    }

    private record ContainerInspectInfo(String user, String workingDir, String hostname) {}

    private ContainerInspectInfo inspectContainerConfig(SwarmTaskContainerInfo info, String swarmApiUrl) {
        String containerId = info != null ? info.containerId() : "";
        try {
            List<String> cmd = buildDockerCliCommand(info, swarmApiUrl);
            cmd.add("inspect");
            cmd.add("--format");
            cmd.add("{{.Config.User}}:::{{.Config.WorkingDir}}:::{{.Config.Hostname}}");
            cmd.add(containerId);

            ProcessBuilder pb = new ProcessBuilder(cmd);
            Process p = pb.start();
            if (p.waitFor(3, TimeUnit.SECONDS) && p.exitValue() == 0) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                    String line = reader.readLine();
                    if (StringUtils.hasText(line)) {
                        String[] parts = line.split(":::", -1);
                        String user = parts.length > 0 && StringUtils.hasText(parts[0]) ? parts[0].trim() : "root";
                        String wd = parts.length > 1 && StringUtils.hasText(parts[1]) ? parts[1].trim() : "/app";
                        String host = parts.length > 2 && StringUtils.hasText(parts[2]) ? parts[2].trim() : containerId.substring(0, Math.min(12, containerId.length()));
                        return new ContainerInspectInfo(user, wd, host);
                    }
                }
            }
        } catch (Exception ignored) {}
        String shortId = containerId.length() > 12 ? containerId.substring(0, 12) : containerId;
        return new ContainerInspectInfo("root", "/app", shortId);
    }

    private InstanceStartupStatus waitForServiceReady(String serviceName, String swarmApiUrl, long maxWaitMs) {
        long deadline = System.currentTimeMillis() + maxWaitMs;
        while (System.currentTimeMillis() < deadline) {
            String taskState = getServiceTaskStatus(serviceName, swarmApiUrl);
            if (StringUtils.hasText(taskState)) {
                if (taskState.startsWith("Running")) {
                    return InstanceStartupStatus.RUNNING;
                }
                if (taskState.contains("Failed") || taskState.contains("Rejected") || taskState.contains("No such image")) {
                    log.warn("[DockerSwarm] 容器服务任务异常失败: service={}, state={}", serviceName, taskState);
                    return InstanceStartupStatus.FAILED;
                }
            }
            try {
                Thread.sleep(400);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return InstanceStartupStatus.STARTING;
    }

}
