package com.humolix.volur.container.service.admin.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.humolix.volur.container.domain.dto.*;

import com.humolix.volur.container.domain.entity.ClusterNode;
import com.humolix.volur.container.domain.entity.CtfChallengeInstance;
import com.humolix.volur.container.domain.enums.InstanceStartupStatus;
import com.humolix.volur.container.domain.vo.AdminClusterNodeVO;
import com.humolix.volur.container.domain.vo.AdminClusterSettingsVO;
import com.humolix.volur.container.domain.vo.AdminContainerTestConnectionVO;
import com.humolix.volur.container.mapper.ClusterNodeMapper;
import com.humolix.volur.container.repository.ClusterImageRepository;
import com.humolix.volur.container.repository.ClusterNodeRepository;
import com.humolix.volur.container.repository.CtfChallengeInstanceRepository;
import com.humolix.volur.container.service.admin.AdminClusterService;
import com.humolix.volur.container.service.admin.AdminImageService;
import com.humolix.volur.container.strategy.DockerSwarmRuntimeStrategy;
import com.humolix.volur.container.strategy.K3sRuntimeStrategy;
import com.humolix.volur.container.util.DockerTlsUtil;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.system.constant.SystemSettingKeys;
import com.humolix.volur.system.service.SystemSettingService;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.NodeAddress;
import io.fabric8.kubernetes.api.model.NodeCondition;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminClusterServiceImpl implements AdminClusterService {

    private final SystemSettingService systemSettingService;
    private final ClusterNodeRepository clusterNodeRepository;
    private final ClusterImageRepository clusterImageRepository;
    private final CtfChallengeInstanceRepository ctfChallengeInstanceRepository;
    private final ClusterNodeMapper clusterNodeMapper;
    private final DockerSwarmRuntimeStrategy swarmStrategy;
    private final K3sRuntimeStrategy k3sStrategy;
    @Lazy
    private final AdminImageService adminImageService;

    @Override
    public AdminClusterSettingsVO getSettings() {
        String containerType = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_TYPE, "DOCKER_SWARM");
        int portStart = systemSettingService.getInt(SystemSettingKeys.CONTAINER_PORT_RANGE_START, 20000);
        int portEnd = systemSettingService.getInt(SystemSettingKeys.CONTAINER_PORT_RANGE_END, 30000);
        String publicHost = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_PUBLIC_HOST, "127.0.0.1");

        String swarmApiUrl = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_DOCKER_SWARM_API_URL, "unix:///var/run/docker.sock");
        String swarmNetwork = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_DOCKER_SWARM_NETWORK, "volur-swarm-network");
        String swarmCertPath = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_DOCKER_SWARM_CERT_PATH, "");
        boolean swarmOutbound = systemSettingService.getBoolean(SystemSettingKeys.CONTAINER_DOCKER_SWARM_OUTBOUND_ENABLED, false);

        String k3sMasterUrl = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_K3S_MASTER_URL, "https://127.0.0.1:6443");
        String k3sNamespace = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_K3S_NAMESPACE, "volur-challenges");
        String k3sKubeconfigPath = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_K3S_KUBECONFIG_PATH, "/etc/rancher/k3s/k3s.yaml");
        String k3sToken = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_K3S_TOKEN, "");

        boolean resourceIsolation = systemSettingService.getBoolean(SystemSettingKeys.CONTAINER_RESOURCE_ISOLATION_ENABLED, true);

        List<ClusterNode> allNodes = clusterNodeRepository.findAll();
        boolean hasRealMaster = allNodes.stream().anyMatch(n -> "MASTER".equalsIgnoreCase(n.getRole())
                && !"127.0.0.1".equals(n.getIpAddress())
                && !"volur-master-01".equals(n.getNodeName()));
        if (hasRealMaster) {
            allNodes.removeIf(n -> {
                if ("volur-master-01".equals(n.getNodeName()) && "127.0.0.1".equals(n.getIpAddress())) {
                    clusterNodeRepository.delete(n);
                    return true;
                }
                return false;
            });
        }
        long totalNodes = allNodes.size();
        long masterNodes = allNodes.stream().filter(n -> "MASTER".equalsIgnoreCase(n.getRole())).count();
        long workerNodes = allNodes.stream().filter(n -> "WORKER".equalsIgnoreCase(n.getRole())).count();
        long readyNodes = allNodes.stream().filter(n -> "READY".equalsIgnoreCase(n.getStatus())).count();

        return AdminClusterSettingsVO.builder()
                .containerType(containerType)
                .portRangeStart(portStart)
                .portRangeEnd(portEnd)
                .publicHost(publicHost)
                .dockerSwarmApiUrl(swarmApiUrl)
                .dockerSwarmNetwork(swarmNetwork)
                .dockerSwarmCertPath(swarmCertPath)
                .dockerSwarmOutboundEnabled(swarmOutbound)
                .k3sMasterUrl(k3sMasterUrl)
                .k3sNamespace(k3sNamespace)
                .k3sKubeconfigPath(k3sKubeconfigPath)
                .k3sToken(k3sToken)
                .resourceIsolationEnabled(resourceIsolation)
                .totalNodes((int) totalNodes)
                .masterNodes((int) masterNodes)
                .workerNodes((int) workerNodes)
                .readyNodes((int) readyNodes)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSettings(AdminClusterSettingsDTO request) {
        if (request == null) return;

        if (request.getPortRangeStart() != null && request.getPortRangeEnd() != null
                && request.getPortRangeStart() >= request.getPortRangeEnd()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "端口范围起始端口必须小于结束端口");
        }

        String oldPublicHost = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_PUBLIC_HOST, "127.0.0.1");
        String oldContainerType = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_TYPE, "DOCKER_SWARM");

        boolean containerTypeChanged = false;
        if (StringUtils.hasText(request.getContainerType())) {
            String newType = request.getContainerType().trim().toUpperCase(Locale.ROOT);
            if (!newType.equalsIgnoreCase(oldContainerType)) {
                containerTypeChanged = true;
            }
            setSetting(SystemSettingKeys.CONTAINER_TYPE, newType);
        }
        if (request.getPortRangeStart() != null) {
            setSetting(SystemSettingKeys.CONTAINER_PORT_RANGE_START, String.valueOf(request.getPortRangeStart()));
        }
        if (request.getPortRangeEnd() != null) {
            setSetting(SystemSettingKeys.CONTAINER_PORT_RANGE_END, String.valueOf(request.getPortRangeEnd()));
        }

        // 同步与校正 Master 节点 IP、镜像拓扑及实例引用
        if (StringUtils.hasText(request.getPublicHost())) {
            String newPublicHost = request.getPublicHost().trim();
            setSetting(SystemSettingKeys.CONTAINER_PUBLIC_HOST, newPublicHost);

            if (!newPublicHost.equalsIgnoreCase(oldPublicHost)) {
                log.info("[AdminClusterService] 检测到靶机公网 Host 变更: {} -> {}，开始级联同步 Master 节点、镜像拓扑与容器实例...", oldPublicHost, newPublicHost);

                // 1. 同步更新 Master 节点 IP
                List<ClusterNode> masterNodes = clusterNodeRepository.findByRole("MASTER");
                if (masterNodes.isEmpty()) {
                    initDefaultMasterNode();
                } else {
                    for (ClusterNode master : masterNodes) {
                        master.setIpAddress(newPublicHost);
                        clusterNodeRepository.save(master);
                    }
                }

                // 2. 级联同步运行中实例 IP 与 accessUrl
                List<CtfChallengeInstance> instances = ctfChallengeInstanceRepository.findByNodeIp(oldPublicHost);
                for (CtfChallengeInstance inst : instances) {
                    inst.setNodeIp(newPublicHost);
                    if (StringUtils.hasText(inst.getAccessUrl()) && inst.getAccessUrl().contains(oldPublicHost)) {
                        inst.setAccessUrl(inst.getAccessUrl().replace(oldPublicHost, newPublicHost));
                    }
                    ctfChallengeInstanceRepository.save(inst);
                }
                if (!"127.0.0.1".equalsIgnoreCase(oldPublicHost)) {
                    List<CtfChallengeInstance> localInstances = ctfChallengeInstanceRepository.findByNodeIp("127.0.0.1");
                    for (CtfChallengeInstance inst : localInstances) {
                        inst.setNodeIp(newPublicHost);
                        if (StringUtils.hasText(inst.getAccessUrl()) && inst.getAccessUrl().contains("127.0.0.1")) {
                            inst.setAccessUrl(inst.getAccessUrl().replace("127.0.0.1", newPublicHost));
                        }
                        ctfChallengeInstanceRepository.save(inst);
                    }
                }

                // 4. 刷新节点镜像计数
                try {
                    adminImageService.updateNodeImageCounts();
                } catch (Exception e) {
                    log.warn("[AdminClusterService] 更新节点镜像计数异常: {}", e.getMessage());
                }
            }
        }

        if (request.getDockerSwarmApiUrl() != null) {
            setSetting(SystemSettingKeys.CONTAINER_DOCKER_SWARM_API_URL, request.getDockerSwarmApiUrl().trim());
        }
        if (request.getDockerSwarmNetwork() != null) {
            setSetting(SystemSettingKeys.CONTAINER_DOCKER_SWARM_NETWORK, request.getDockerSwarmNetwork().trim());
        }
        if (request.getDockerSwarmCertPath() != null) {
            setSetting(SystemSettingKeys.CONTAINER_DOCKER_SWARM_CERT_PATH, request.getDockerSwarmCertPath().trim());
        }
        if (request.getDockerSwarmOutboundEnabled() != null) {
            setSetting(SystemSettingKeys.CONTAINER_DOCKER_SWARM_OUTBOUND_ENABLED, String.valueOf(request.getDockerSwarmOutboundEnabled()));
        }

        if (request.getK3sMasterUrl() != null) {
            setSetting(SystemSettingKeys.CONTAINER_K3S_MASTER_URL, request.getK3sMasterUrl().trim());
        }
        if (request.getK3sNamespace() != null) {
            setSetting(SystemSettingKeys.CONTAINER_K3S_NAMESPACE, request.getK3sNamespace().trim());
        }
        if (request.getK3sKubeconfigPath() != null) {
            setSetting(SystemSettingKeys.CONTAINER_K3S_KUBECONFIG_PATH, request.getK3sKubeconfigPath().trim());
        }
        if (request.getK3sToken() != null) {
            setSetting(SystemSettingKeys.CONTAINER_K3S_TOKEN, request.getK3sToken().trim());
        }
        if (request.getK3sKubeconfigPath() != null || request.getK3sNamespace() != null || request.getK3sMasterUrl() != null || containerTypeChanged) {
            k3sStrategy.resetClient();
        }

        if (request.getResourceIsolationEnabled() != null) {
            setSetting(SystemSettingKeys.CONTAINER_RESOURCE_ISOLATION_ENABLED, String.valueOf(request.getResourceIsolationEnabled()));
            applyResourceIsolation(request.getResourceIsolationEnabled());
        }


        String effectiveContainerType = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_TYPE, "DOCKER_SWARM");
        if ("K3S".equalsIgnoreCase(effectiveContainerType) && (containerTypeChanged || request.getK3sKubeconfigPath() != null || request.getK3sMasterUrl() != null)) {
            try {
                syncK3sClusterNodes();
            } catch (Exception e) {
                log.warn("[AdminClusterService] 切换至 K3s 模式后自动扫描机器提示: {}", e.getMessage());
            }
        }

        log.info("[AdminClusterService] 集群参数设置已全量更新保存成功");
    }

    @Override
    public AdminContainerTestConnectionVO testConnection(AdminContainerTestConnectionDTO request) {
        String type = (request != null && StringUtils.hasText(request.getContainerType()))
                ? request.getContainerType().trim()
                : systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_TYPE, "DOCKER_SWARM");

        if ("DOCKER_SWARM".equalsIgnoreCase(type)) {
            return swarmStrategy.testConnection(request);
        } else {
            return k3sStrategy.testConnection(request);
        }
    }

    @Override
    public List<AdminClusterNodeVO> listNodes() {
        List<ClusterNode> nodes = clusterNodeRepository.findAll();
        if (nodes.isEmpty()) {
            initDefaultMasterNode();
            nodes = clusterNodeRepository.findAll();
        }

        boolean hasRealMaster = nodes.stream().anyMatch(n -> "MASTER".equalsIgnoreCase(n.getRole())
                && !"127.0.0.1".equals(n.getIpAddress())
                && !"volur-master-01".equals(n.getNodeName()));
        if (hasRealMaster) {
            nodes.removeIf(n -> {
                if ("volur-master-01".equals(n.getNodeName()) && "127.0.0.1".equals(n.getIpAddress())) {
                    clusterNodeRepository.delete(n);
                    return true;
                }
                return false;
            });
        }

        nodes.sort(Comparator.comparing((ClusterNode n) -> !"MASTER".equalsIgnoreCase(n.getRole()))
                .thenComparing(ClusterNode::getJoinTime, Comparator.nullsLast(Comparator.naturalOrder())));

        return clusterNodeMapper.toVOList(nodes);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminClusterNodeVO addNode(AdminClusterNodeCreateDTO request) {
        String ip = request.getIpAddress().trim();
        if (clusterNodeRepository.existsByIpAddress(ip)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "IP 地址 " + ip + " 已存在于集群节点列表中");
        }

        if (!"WORKER".equalsIgnoreCase(request.getRole()) && !"MASTER".equalsIgnoreCase(request.getRole())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "节点角色仅支持 WORKER 或 MASTER");
        }
        if ("MASTER".equalsIgnoreCase(request.getRole())) {
            long masterCount = clusterNodeRepository.countByRole("MASTER");
            if (masterCount >= 1) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "当前集群已存在主控制节点，仅支持添加工作节点 (WORKER)");
            }
        }

        boolean isWorker = "WORKER".equalsIgnoreCase(request.getRole());
        boolean isResource = isWorker || !clusterNodeRepository.existsByRole("WORKER");

        int sshPort = request.getSshPort() != null ? request.getSshPort() : 22;
        String sshUser = StringUtils.hasText(request.getSshUser()) ? request.getSshUser().trim() : "root";
        String sshPassword = request.getSshPassword();
        String sshKey = request.getSshKey();

        boolean hasPassword = StringUtils.hasText(sshPassword);
        boolean hasKey = StringUtils.hasText(sshKey);
        if (!hasPassword && !hasKey) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "请提供 SSH 连接密码或私钥凭据（二选一）");
        }

        // 1. IP 连通性预检
        checkIpReachable(ip, sshPort);

        // 2. SSH 握手凭据核验
        log.info("[AdminClusterService] 正在验证目标节点 SSH 连接与身份认证: host={}:{}, user={}", ip, sshPort, sshUser);
        verifySshConnectivity(ip, sshPort, sshUser, sshPassword, sshKey);

        // 2.1 针对非 root 用户进行特权提权能力探测 (SSH 密码即为 sudo 密码)
        SshPrivilegeMode privMode = detectPrivilegeMode(ip, sshPort, sshUser, sshPassword, sshKey);
        if (privMode == SshPrivilegeMode.UNPRIVILEGED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "SSH 用户 [" + sshUser + "] 在目标节点未具备 sudo 提权权限或密码不匹配，请确保该账号已配置 sudoer 权限（免密 NOPASSWD 或正确密码）");
        }

        // 3. 检查容器运行时是否已就绪
        String dockerVersion = "containerd";
        if ("DOCKER_SWARM".equalsIgnoreCase(request.getClusterType())) {
            SshExecResult whichDocker = executePrivilegedSshCommand(ip, sshPort, sshUser, sshPassword, sshKey, "which docker || command -v docker", 6, privMode);
            if (!whichDocker.isSuccess() || !StringUtils.hasText(whichDocker.stdout().trim())) {
                log.warn("[AdminClusterService] 目标节点 [{}] 未检测到 Docker 运行时", ip);
                throw new BusinessException(HttpStatus.BAD_REQUEST, "目标节点未检测到 Docker 运行时，请先在目标主机上安装并启动 Docker 服务后再添加至 Swarm 集群");
            }

            // 4. 采集 Docker 引擎真实版本
            SshExecResult verRes = executePrivilegedSshCommand(ip, sshPort, sshUser, sshPassword, sshKey, "docker --version", 6, privMode);
            if (!verRes.isSuccess() || !StringUtils.hasText(verRes.stdout().trim())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "目标节点 Docker 无法响应版本查询指令，请确认 Docker 服务已正常启动");
            }
            dockerVersion = verRes.stdout().trim();
        } else {
            // K3s 模式
            SshExecResult whichK3s = executePrivilegedSshCommand(ip, sshPort, sshUser, sshPassword, sshKey, "which k3s || which containerd || which docker || which curl || which wget", 6, privMode);
            if (!whichK3s.isSuccess() || !StringUtils.hasText(whichK3s.stdout().trim())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "目标节点缺少 k3s、containerd、docker 或 curl 工具，无法完成 K3s 节点自动化入网");
            }
            SshExecResult verRes = executePrivilegedSshCommand(ip, sshPort, sshUser, sshPassword, sshKey, "k3s --version 2>/dev/null || containerd --version 2>/dev/null || docker --version 2>/dev/null", 6, privMode);
            if (verRes.isSuccess() && StringUtils.hasText(verRes.stdout().trim())) {
                dockerVersion = verRes.stdout().trim().split("\n")[0];
            } else {
                dockerVersion = "K3s Agent (containerd)";
            }
        }

        // 5. 动态收集硬件与内核信息 (杜绝预设假数据)
        String kernelRelease = "";
        String arch = "x86_64";
        SshExecResult unameRes = executePrivilegedSshCommand(ip, sshPort, sshUser, sshPassword, sshKey, "uname -r && uname -m", 6, privMode);
        if (unameRes.isSuccess() && StringUtils.hasText(unameRes.stdout())) {
            String[] unameLines = unameRes.stdout().trim().split("\n");
            if (unameLines.length > 0 && StringUtils.hasText(unameLines[0])) {
                kernelRelease = unameLines[0].trim();
            }
            if (unameLines.length > 1 && StringUtils.hasText(unameLines[1])) {
                arch = unameLines[1].trim();
            }
        }

        String osDistro = "";
        String osDistroCmd = "if [ -f /etc/os-release ]; then " +
                ". /etc/os-release && echo \"$PRETTY_NAME\"; " +
                "elif [ -f /etc/redhat-release ]; then " +
                "cat /etc/redhat-release; " +
                "elif [ -f /etc/issue ]; then " +
                "head -n 1 /etc/issue | tr -d '\\r\\n'; " +
                "else uname -s; fi";
        SshExecResult osDistroRes = executePrivilegedSshCommand(ip, sshPort, sshUser, sshPassword, sshKey, osDistroCmd, 6, privMode);
        if (osDistroRes.isSuccess() && StringUtils.hasText(osDistroRes.stdout())) {
            osDistro = osDistroRes.stdout().trim();
        }

        String osInfo;
        if (StringUtils.hasText(osDistro)) {
            osInfo = StringUtils.hasText(kernelRelease) ? (osDistro + " (" + arch + ", " + kernelRelease + ")") : (osDistro + " (" + arch + ")");
        } else {
            osInfo = "Linux " + (StringUtils.hasText(kernelRelease) ? kernelRelease + " " : "") + "(" + arch + ")";
        }
        if (osInfo.length() > 125) {
            osInfo = osInfo.substring(0, 122) + "...";
        }

        int cpuCores = 1;
        SshExecResult cpuRes = executePrivilegedSshCommand(ip, sshPort, sshUser, sshPassword, sshKey, "nproc", 5, privMode);
        if (cpuRes.isSuccess() && StringUtils.hasText(cpuRes.stdout().trim())) {
            try {
                cpuCores = Math.max(1, Integer.parseInt(cpuRes.stdout().trim()));
            } catch (NumberFormatException ignored) {}
        }

        long memoryBytes = 1073741824L;
        SshExecResult memRes = executePrivilegedSshCommand(ip, sshPort, sshUser, sshPassword, sshKey, "grep MemTotal /proc/meminfo | awk '{print $2}'", 5, privMode);
        if (memRes.isSuccess() && StringUtils.hasText(memRes.stdout().trim())) {
            try {
                long memKb = Long.parseLong(memRes.stdout().trim());
                if (memKb > 0) {
                    memoryBytes = memKb * 1024L;
                }
            } catch (NumberFormatException ignored) {}
        }

        ClusterNode node = ClusterNode.builder()
                .nodeName(request.getNodeName().trim())
                .ipAddress(ip)
                .sshPort(sshPort)
                .sshUser(sshUser)
                .sshPassword(sshPassword)
                .sshKey(sshKey)
                .clusterType(request.getClusterType().trim())
                .role(request.getRole().toUpperCase().trim())
                .isResourceNode(isResource)
                .status("READY")
                .osInfo(osInfo)
                .dockerVersion(dockerVersion)
                .cpuCores(cpuCores)
                .memoryBytes(memoryBytes)
                .containerCount(0)
                .imageCount(0)
                .joinTime(Instant.now())
                .lastHeartbeat(Instant.now())
                .build();

        // 6. 配置 2376+TLS 双向证书与并严密核验 Docker 正常运行（仅在 Docker Swarm 模式下需要）
        if ("DOCKER_SWARM".equalsIgnoreCase(request.getClusterType())) {
            setupDockerTlsAndValidate(ip, sshPort, sshUser, sshPassword, sshKey, node, privMode);
        } else {
            node.setTlsEnabled(false);
            node.setDockerPort(0);
        }

        // 7. 集群入网下发与自动化注册验证
        if ("DOCKER_SWARM".equalsIgnoreCase(request.getClusterType())) {
            String masterIp = resolveMasterIpForWorker(ip, sshPort);
            if (StringUtils.hasText(masterIp) && !"127.0.0.1".equals(masterIp) && !"localhost".equalsIgnoreCase(masterIp)) {
                String workerToken = getSwarmWorkerJoinToken(masterIp);
                if (StringUtils.hasText(workerToken)) {
                    log.info("[AdminClusterService] 正在将目标节点加入 Swarm 集群: token={}..., master={}",
                            workerToken.substring(0, Math.min(10, workerToken.length())), masterIp);
                    String swarmJoinCmd = "docker swarm leave --force 2>/dev/null; docker swarm join --token " + workerToken + " " + masterIp + ":2377";
                    SshExecResult joinRes = executePrivilegedSshCommand(ip, sshPort, sshUser, sshPassword, sshKey, swarmJoinCmd, 20, privMode);
                    log.info("[AdminClusterService] 目标节点加入 Swarm 结果: exit={}, out={}, err={}", joinRes.exitCode(), joinRes.stdout(), joinRes.stderr());

                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ignored) {}

                    String workerNodeId = checkWorkerSwarmNodeId(ip, sshPort, sshUser, sshPassword, sshKey, privMode);
                    Map<String, SwarmNodeInfo> swarmNodes = querySwarmNodesFromManager();
                    boolean joinedAndReady = (workerNodeId != null) && swarmNodes.containsKey(workerNodeId)
                            && "Ready".equalsIgnoreCase(swarmNodes.get(workerNodeId).status());

                    if (!joinedAndReady) {
                        String diag = "未知原因";
                        if (StringUtils.hasText(joinRes.stderr())) {
                            diag = joinRes.stderr().trim();
                        } else if (StringUtils.hasText(joinRes.stdout())) {
                            diag = joinRes.stdout().trim();
                        }
                        if (diag.contains("Connection refused") || diag.contains("connection refused")) {
                            diag = "连接主节点 " + masterIp + ":2377 被拒绝。主节点可能处于 macOS Hypervisor NAT 内部或 2377 端口未对外监听放通。";
                        } else if (diag.contains("Timeout") || diag.contains("timed out") || diag.contains("context deadline exceeded")) {
                            diag = "连接主节点 " + masterIp + ":2377 超时。请确认主节点的防火墙、安全组已放通 2377(TCP)、7946(TCP/UDP)、4789(UDP) 端口。";
                        }
                        log.warn("[AdminClusterService] 目标节点 [{}] 未能成功加入 Swarm 集群: {}", ip, diag);
                        throw new BusinessException(HttpStatus.BAD_REQUEST, "目标节点未能成功加入 Docker Swarm 集群: " + diag + "。请确认主节点已放通 2377 等集群通信端口并确保网络互通。");
                    }

                    node.setInCluster(true);
                    node.setClusterNodeId(workerNodeId);
                    node.setStatus("READY");
                } else {
                    throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "无法获取 Docker Swarm 注册令牌 (Join Token)，请确认控制面 Swarm 已正常初始化");
                }
            } else {
                log.info("[AdminClusterService] Master 节点未配置公网/局域网通信 IP (当前为 127.0.0.1)，跳过 Swarm 跨节点自动加入");
            }
        } else if ("K3S".equalsIgnoreCase(request.getClusterType())) {
            // 若为 K3s 架构，自动将节点作为 K3s Agent 加入集群
            String masterIp = resolveMasterIpForWorker(ip, sshPort);
            if (StringUtils.hasText(masterIp) && !"127.0.0.1".equals(masterIp) && !"localhost".equalsIgnoreCase(masterIp)) {
                String k3sToken = resolveK3sNodeToken(masterIp);
                if (StringUtils.hasText(k3sToken)) {
                    String k3sServerUrl = "https://" + masterIp + ":6443";
                    log.info("[AdminClusterService] 正在将目标节点 [{}] 加入 K3s 集群 (server={})...", ip, k3sServerUrl);
                    String k3sJoinCmd = String.format(Locale.ROOT,
                            "if command -v k3s >/dev/null 2>&1; then " +
                            "  systemctl stop k3s-agent 2>/dev/null; " +
                            "  mkdir -p /etc/rancher/k3s && " +
                            "  printf 'server: %s\\ntoken: %s\\n' '%s' '%s' > /etc/rancher/k3s/config.yaml && " +
                            "  (systemctl restart k3s-agent 2>/dev/null || (k3s agent --server %s --token %s >/dev/null 2>&1 &)); " +
                            "else " +
                            "  curl -sfL https://get.k3s.io | K3S_URL=%s K3S_TOKEN=%s sh - ; " +
                            "fi",
                            k3sServerUrl, k3sToken, k3sServerUrl, k3sToken, k3sServerUrl, k3sToken, k3sServerUrl, k3sToken);

                    SshExecResult joinRes = executePrivilegedSshCommand(ip, sshPort, sshUser, sshPassword, sshKey, k3sJoinCmd, 40, privMode);
                    log.info("[AdminClusterService] 目标节点执行 K3s agent 加入结果: exit={}, out={}, err={}",
                            joinRes.exitCode(), joinRes.stdout(), joinRes.stderr());

                    boolean joined = false;
                    for (int i = 0; i < 8; i++) {
                        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                        KubernetesClient client = k3sStrategy.getClient();
                        if (client != null) {
                            try {
                                for (Node n : client.nodes().list().getItems()) {
                                    if (n.getStatus() != null && n.getStatus().getAddresses() != null) {
                                        for (NodeAddress a : n.getStatus().getAddresses()) {
                                            if (ip.equalsIgnoreCase(a.getAddress())) {
                                                node.setInCluster(true);
                                                if (n.getMetadata() != null) {
                                                    node.setClusterNodeId(n.getMetadata().getUid());
                                                }
                                                node.setStatus("READY");
                                                joined = true;
                                                break;
                                            }
                                        }
                                    }
                                    if (joined) break;
                                }
                            } catch (Exception ignored) {}
                        }
                        if (joined) break;
                    }

                    if (!joined) {
                        log.warn("[AdminClusterService] 目标节点 [{}] 已下发 K3s 加入指令，但未在 16 秒内于 API 观测到 Ready 状态，置为 JOINING 状态", ip);
                        node.setInCluster(false);
                        node.setStatus("JOINING");
                    }
                } else {
                    log.warn("[AdminClusterService] 未能获取到 K3s Node Token，置为 JOINING 状态");
                    node.setInCluster(false);
                    node.setStatus("JOINING");
                }
            } else {
                log.info("[AdminClusterService] Master 节点未配置可通信的外部 IP (当前为 127.0.0.1)，跳过 K3s 自动跨机入网");
            }
        }

        ClusterNode savedNode = clusterNodeRepository.save(node);
        log.info("[AdminClusterService] 成功扩展集群并添加新节点: name={}, ip={}, role={}, isResourceNode={}",
                savedNode.getNodeName(), savedNode.getIpAddress(), savedNode.getRole(), savedNode.getIsResourceNode());

        // 拓扑隔离维护：当集群存在 Worker 资源节点时，Master 节点仅作为纯 Control Node
        if (isWorker) {
            clusterNodeRepository.findByRole("MASTER").forEach(master -> {
                if (Boolean.TRUE.equals(master.getIsResourceNode())) {
                    master.setIsResourceNode(false);
                    clusterNodeRepository.save(master);
                    log.info("[AdminClusterService] 检测到集群已具备 Worker 资源节点，Master 节点自动切换为纯控制面: ip={}", master.getIpAddress());
                }
            });
        }

        return clusterNodeMapper.toVO(savedNode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminClusterNodeVO updateNode(Long id, AdminClusterNodeUpdateDTO request) {
        ClusterNode node = clusterNodeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "节点不存在"));

        if ("MASTER".equalsIgnoreCase(node.getRole())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "主控制节点为本机平台控制面，禁止修改");
        }

        String oldIp = node.getIpAddress() != null ? node.getIpAddress().trim() : "";
        String newIp = (request != null && StringUtils.hasText(request.getIpAddress())) ? request.getIpAddress().trim() : oldIp;
        boolean ipChanged = StringUtils.hasText(newIp) && !newIp.equalsIgnoreCase(oldIp);
        boolean wasResourceNode = Boolean.TRUE.equals(node.getIsResourceNode());

        // 如果 IP 发生变更，执行前置安全检查与唯一性校验
        if (ipChanged) {
            // 1. 运行中容器安全检查：检查数据库中属于该节点的运行中/启动中实例
            long activeInstances = ctfChallengeInstanceRepository.countByNodeIpAndStatusIn(oldIp,
                    List.of(InstanceStartupStatus.RUNNING, InstanceStartupStatus.STARTING, InstanceStartupStatus.DOWNLOADING_IMAGE, InstanceStartupStatus.QUEUED));
            Integer containerCount = node.getContainerCount();
            int reportedContainerCount = (containerCount != null) ? containerCount : 0;

            int liveRunningContainers = 0;
            try {
                int oldSshPort = node.getSshPort() != null ? node.getSshPort() : 22;
                String oldSshUser = StringUtils.hasText(node.getSshUser()) ? node.getSshUser().trim() : "root";
                String oldSshPassword = node.getSshPassword();
                String oldSshKey = node.getSshKey();
                SshExecResult psRes = executeSshCommand(oldIp, oldSshPort, oldSshUser, oldSshPassword, oldSshKey, "docker ps -q | wc -l", 5);
                if (psRes.isSuccess() && StringUtils.hasText(psRes.stdout())) {
                    liveRunningContainers = Integer.parseInt(psRes.stdout().trim());
                }
            } catch (Exception e) {
                log.debug("[AdminClusterService] 获取旧节点 live 容器数提示: {}", e.getMessage());
            }

            long totalActive = Math.max(Math.max(activeInstances, reportedContainerCount), liveRunningContainers);
            if (totalActive > 0) {
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        String.format("安全检查未通过：目标节点当前仍有 %d 个正在运行中的容器实例。为了避免运行中实例网络中断或失联，严禁在活跃状态下更改节点 IP！请先在容器管理中停止或销毁相关实例后再进行修改。", totalActive));
            }

            // 2. IP 唯一性校验
            if (clusterNodeRepository.existsByIpAddress(newIp)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "目标 IP 地址 [" + newIp + "] 已被其他集群节点占用");
            }
        }

        if (StringUtils.hasText(request.getNodeName())) {
            node.setNodeName(request.getNodeName().trim());
        }
        if (request.getSshPort() != null) {
            node.setSshPort(request.getSshPort());
        }
        if (StringUtils.hasText(request.getSshUser())) {
            node.setSshUser(request.getSshUser().trim());
        }

        boolean credentialsChanged = false;
        if (StringUtils.hasText(request.getSshPassword())) {
            node.setSshPassword(request.getSshPassword().trim());
            credentialsChanged = true;
        }
        if (StringUtils.hasText(request.getSshKey())) {
            node.setSshKey(request.getSshKey().trim());
            credentialsChanged = true;
        }

        String targetRole = StringUtils.hasText(request.getRole()) ? request.getRole().toUpperCase().trim() : node.getRole();
        if ("MASTER".equalsIgnoreCase(targetRole)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "主控制节点为本机平台控制面，禁止将工作节点变更为控制节点");
        }
        node.setRole(targetRole);

        boolean isWorker = "WORKER".equalsIgnoreCase(targetRole);
        boolean isResource = request.getIsResourceNode() != null
                ? request.getIsResourceNode()
                : (isWorker || !clusterNodeRepository.existsByRole("WORKER"));
        node.setIsResourceNode(isResource);

        String effectiveIp = ipChanged ? newIp : oldIp;
        int effectivePort = node.getSshPort() != null ? node.getSshPort() : 22;
        String effectiveUser = StringUtils.hasText(node.getSshUser()) ? node.getSshUser().trim() : "root";
        String effectivePassword = node.getSshPassword();
        String effectiveKey = node.getSshKey();

        // 如果修改了账号密码凭据或更改了 IP 地址，验证目标节点网络连通与 SSH 凭据
        if (ipChanged || credentialsChanged) {
            checkIpReachable(effectiveIp, effectivePort);
            verifySshConnectivity(effectiveIp, effectivePort, effectiveUser, effectivePassword, effectiveKey);
        }

        // 如果修改了 IP 地址，执行 TLS 重新签发、Swarm 重入与全平台数据级联同步
        if (ipChanged) {
            log.info("[AdminClusterService] 节点 IP 发生变更: {} -> {}，开始为新 IP 生成 Docker 2376 TLS 双向安全证书...", oldIp, newIp);
            setupDockerTlsAndValidate(newIp, effectivePort, effectiveUser, effectivePassword, effectiveKey, node);

            // 清理旧 IP 对应的平台本地证书缓存目录
            try {
                Path oldCertDir = Paths.get(System.getProperty("user.dir"), "data", "certs", "nodes", oldIp);
                if (Files.exists(oldCertDir)) {
                    try (var stream = Files.walk(oldCertDir)) {
                        stream.sorted(Comparator.reverseOrder()).forEach(p -> {
                            try { Files.deleteIfExists(p); } catch (Exception ignored) {}
                        });
                    }
                }
            } catch (Exception e) {
                log.warn("[AdminClusterService] 清理旧节点 TLS 证书目录异常: {}", e.getMessage());
            }

            // 更新节点的 IP 字段
            node.setIpAddress(newIp);

            // 若节点为 Worker 且已加入集群，重新下发 Swarm leave 与 join 指令以绑定新 IP
            if (isWorker) {
                try {
                    String masterIp = resolveMasterIpForWorker(newIp, effectivePort);
                    if (StringUtils.hasText(masterIp) && !"127.0.0.1".equals(masterIp) && !"localhost".equalsIgnoreCase(masterIp)) {
                        String workerToken = getSwarmWorkerJoinToken(masterIp);
                        if (StringUtils.hasText(workerToken)) {
                            log.info("[AdminClusterService] 正在为新 IP [{}] 重新下发 Swarm 集群入网指令 (master={}:2377)...", newIp, masterIp);
                            String swarmRejoinCmd = "docker swarm leave --force 2>/dev/null; docker swarm join --token " + workerToken + " " + masterIp + ":2377";
                            executeSshCommand(newIp, effectivePort, effectiveUser, effectivePassword, effectiveKey, swarmRejoinCmd, 20);
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException ignored) {}
                            String workerNodeId = checkWorkerSwarmNodeId(newIp, effectivePort, effectiveUser, effectivePassword, effectiveKey);
                            if (StringUtils.hasText(workerNodeId)) {
                                node.setClusterNodeId(workerNodeId);
                                node.setInCluster(true);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("[AdminClusterService] 节点 IP 变更后重新加入 Swarm 异常: {}", e.getMessage());
                }
            }

            // 全平台功能同步 1: 节点 IP 变更，从镜像管理中安全剔除旧节点 (oldIp) 的镜像拓扑关联
            adminImageService.removeNodeFromImages(oldIp, false);
            log.info("[AdminClusterService] 节点 IP 变更: 已从镜像管理中成功剔除旧节点 [{}] 的镜像拓扑关联", oldIp);

            // 全平台功能同步 2: 同步 CtfChallengeInstance.nodeIp 与 accessUrl
            List<CtfChallengeInstance> instances = ctfChallengeInstanceRepository.findByNodeIp(oldIp);
            for (CtfChallengeInstance inst : instances) {
                inst.setNodeIp(newIp);
                if (StringUtils.hasText(inst.getAccessUrl()) && inst.getAccessUrl().contains(oldIp)) {
                    inst.setAccessUrl(inst.getAccessUrl().replace(oldIp, newIp));
                }
                ctfChallengeInstanceRepository.save(inst);
            }
            log.info("[AdminClusterService] 已完成历史容器实例节点 IP 与访问 URL 级联同步 (共 {} 个实例)", instances.size());
        }

        performHealthCheck(node);

        // 业务解耦：若工作节点尚未成功加入编排集群 (inCluster=false)，则自动降级禁止作为题目资源调度节点，允许正常保存 IP 与凭据
        if (isWorker && !Boolean.TRUE.equals(node.getInCluster())) {
            node.setIsResourceNode(false);
            log.warn("[AdminClusterService] 工作节点 [{}] 尚未加入编排集群 (inCluster=false)，已自动降级将 isResourceNode 置为 false", node.getIpAddress());
        }

        ClusterNode saved = clusterNodeRepository.save(node);

        // 拓扑隔离维护与 Master 自动兜底：
        // 当集群中存在活跃就绪且在集群内的 Worker 资源节点时，Master 切换为纯控制面 (isResourceNode=false)；
        // 否则 Master 节点必须自动兜底承担题目运行角色 (isResourceNode=true)
        List<ClusterNode> currentNodes = clusterNodeRepository.findAll();
        boolean hasReadyWorkerResource = currentNodes.stream()
                .anyMatch(n -> "WORKER".equalsIgnoreCase(n.getRole())
                        && Boolean.TRUE.equals(n.getInCluster())
                        && Boolean.TRUE.equals(n.getIsResourceNode()));

        clusterNodeRepository.findByRole("MASTER").forEach(master -> {
            boolean shouldMasterBeResource = !hasReadyWorkerResource;
            if (!Objects.equals(master.getIsResourceNode(), shouldMasterBeResource)) {
                master.setIsResourceNode(shouldMasterBeResource);
                clusterNodeRepository.save(master);
                log.info("[AdminClusterService] Master 节点题目资源调度状态更新为 isResourceNode={}, ip={}",
                        shouldMasterBeResource, master.getIpAddress());
            }
        });

        if (ipChanged) {
            try {
                adminImageService.loadAndSyncClusterImages();
            } catch (Exception e) {
                log.warn("[AdminClusterService] IP 变更后触发全平台镜像对账提示: {}", e.getMessage());
            }
        } else if (wasResourceNode && !Boolean.TRUE.equals(saved.getIsResourceNode())) {
            // 若未变更 IP 但节点被取消资源节点资格 (isResourceNode: true -> false)，从赛题镜像中同步剔除该节点拓扑
            try {
                adminImageService.removeNodeFromImages(saved.getIpAddress(), true);
                log.info("[AdminClusterService] 节点 [{}] 已降级为非资源节点，已从赛题镜像中剔除该节点拓扑", saved.getIpAddress());
            } catch (Exception e) {
                log.warn("[AdminClusterService] 节点降级后剔除赛题镜像拓扑异常: {}", e.getMessage());
            }
        }

        log.info("[AdminClusterService] 成功更新集群节点配置: id={}, name={}, ip={}, role={}, isResourceNode={}",
                id, saved.getNodeName(), saved.getIpAddress(), saved.getRole(), saved.getIsResourceNode());

        return clusterNodeMapper.toVO(saved);
    }

    @Override
    public AdminContainerTestConnectionVO testNodeConnection(Long id, AdminClusterNodeUpdateDTO request) {
        ClusterNode node = clusterNodeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "节点不存在"));

        if ("MASTER".equalsIgnoreCase(node.getRole())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "主控制节点为本机平台控制面，无需且不支持独立存活性测试");
        }

        long startTime = System.currentTimeMillis();
        String host = (request != null && StringUtils.hasText(request.getIpAddress())) ? request.getIpAddress().trim() : node.getIpAddress().trim();
        int port = (request != null && request.getSshPort() != null) ? request.getSshPort() : (node.getSshPort() != null ? node.getSshPort() : 22);
        String user = (request != null && StringUtils.hasText(request.getSshUser())) ? request.getSshUser().trim() : node.getSshUser();
        String password = (request != null && StringUtils.hasText(request.getSshPassword())) ? request.getSshPassword().trim() : node.getSshPassword();
        String privateKey = (request != null && StringUtils.hasText(request.getSshKey())) ? request.getSshKey().trim() : node.getSshKey();

        // 1. 测试网络 IP 连通性
        try {
            checkIpReachable(host, port);
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            return AdminContainerTestConnectionVO.builder()
                    .connected(false)
                    .latencyMs(latency)
                    .message("目标 IP 连通测试失败: " + e.getMessage())
                    .build();
        }

        // 2. 测试 SSH 身份认证连接
        try {
            verifySshConnectivity(host, port, user, password, privateKey);
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            return AdminContainerTestConnectionVO.builder()
                    .connected(false)
                    .latencyMs(latency)
                    .message(e.getMessage())
                    .build();
        }

        // 3. 探测节点容器引擎活跃度与版本
        long latency = System.currentTimeMillis() - startTime;
        String runtimeVer = "容器运行时就绪";
        if ("K3S".equalsIgnoreCase(node.getClusterType())) {
            SshExecResult k3sRes = executeSshCommand(host, port, user, password, privateKey, "k3s --version 2>/dev/null || containerd --version 2>/dev/null || docker --version 2>/dev/null", 5);
            if (k3sRes.isSuccess() && StringUtils.hasText(k3sRes.stdout())) {
                runtimeVer = k3sRes.stdout().trim().split("\n")[0];
            } else {
                runtimeVer = "K3s Agent 运行中";
            }
        } else {
            SshExecResult verRes = executeSshCommand(host, port, user, password, privateKey, "docker --version", 5);
            if (verRes.isSuccess() && StringUtils.hasText(verRes.stdout())) {
                runtimeVer = verRes.stdout().trim();
            }
        }

        return AdminContainerTestConnectionVO.builder()
                .connected(true)
                .latencyMs(latency)
                .clusterVersion(runtimeVer)
                .nodeCount(1)
                .message("节点存活测试成功，响应延迟 " + latency + "ms (" + runtimeVer + ")")
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNodeRole(Long id, AdminClusterNodeRoleUpdateDTO request) {
        ClusterNode node = clusterNodeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "节点不存在"));

        String targetRole = request.getRole().toUpperCase().trim();
        if ("MASTER".equalsIgnoreCase(node.getRole()) && !"MASTER".equalsIgnoreCase(targetRole)) {
            long masterCount = clusterNodeRepository.countByRole("MASTER");
            if (masterCount <= 1) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "集群必须保留至少一个主控制节点");
            }
        }

        boolean wasResource = Boolean.TRUE.equals(node.getIsResourceNode());
        boolean targetIsResource = Boolean.TRUE.equals(request.getIsResourceNode());
        node.setRole(targetRole);
        node.setIsResourceNode(targetIsResource);
        clusterNodeRepository.save(node);
        log.info("[AdminClusterService] 已更新节点角色: id={}, role={}, isResourceNode={}",
                id, targetRole, node.getIsResourceNode());

        if (wasResource && !targetIsResource) {
            try {
                adminImageService.removeNodeFromImages(node.getIpAddress(), true);
                log.info("[AdminClusterService] 节点 [{}] 角色更新为非资源节点，已从赛题镜像中剔除该节点拓扑", node.getIpAddress());
            } catch (Exception e) {
                log.warn("[AdminClusterService] 角色更新后剔除赛题镜像拓扑异常: {}", e.getMessage());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeNode(Long id) {
        ClusterNode node = clusterNodeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "节点不存在"));

        if ("MASTER".equalsIgnoreCase(node.getRole())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "主控制节点为本机平台控制面，禁止移除");
        }

        String nodeIp = node.getIpAddress();
        clusterNodeRepository.delete(node);
        log.info("[AdminClusterService] 成功移除集群节点: id={}, ip={}", id, nodeIp);

        // 同步从镜像管理中清理被移除节点的镜像拓扑
        if (StringUtils.hasText(nodeIp)) {
            try {
                adminImageService.removeNodeFromImages(nodeIp, false);
                log.info("[AdminClusterService] 移除节点后已同步从镜像管理中清理节点 [{}] 镜像拓扑", nodeIp);
            } catch (Exception e) {
                log.warn("[AdminClusterService] 移除节点后清理镜像拓扑异常: {}", e.getMessage());
            }
        }

        // 如果移除后仅剩主节点，且没有任何资源节点，将主节点自动还原为资源节点以保证平台仍可单机运行
        List<ClusterNode> remainingNodes = clusterNodeRepository.findAll();
        boolean hasWorker = remainingNodes.stream().anyMatch(n -> "WORKER".equalsIgnoreCase(n.getRole()));
        if (!hasWorker) {
            remainingNodes.stream().filter(n -> "MASTER".equalsIgnoreCase(n.getRole())).forEach(m -> {
                m.setIsResourceNode(true);
                clusterNodeRepository.save(m);
                log.info("[AdminClusterService] 集群已无 Worker 节点，主节点已自动降级承担单机资源运行角色: ip={}", m.getIpAddress());
            });
        }
    }

    @Override
    public void syncImagesToNode(Long id) {
        ClusterNode node = clusterNodeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "目标节点不存在"));
        adminImageService.migrateMasterImagesToWorker(node);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminClusterNodeVO checkNodeHealth(Long id) {
        ClusterNode node = clusterNodeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "目标节点不存在"));

        performHealthCheck(node);
        ClusterNode saved = clusterNodeRepository.save(node);
        return clusterNodeMapper.toVO(saved);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<AdminClusterNodeVO> checkAllNodesHealth() {
        String containerType = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_TYPE, "DOCKER_SWARM");
        if ("K3S".equalsIgnoreCase(containerType)) {
            try {
                syncK3sClusterNodes();
            } catch (Exception e) {
                log.warn("[AdminClusterService] 全量健康检测同步 K3s 节点提示: {}", e.getMessage());
            }
        }

        List<ClusterNode> nodes = clusterNodeRepository.findAll();
        for (ClusterNode node : nodes) {
            performHealthCheck(node);
            clusterNodeRepository.save(node);
        }

        // 重新核验与对账平台镜像拓扑分布
        try {
            adminImageService.loadAndSyncClusterImages();
        } catch (Exception e) {
            log.warn("[AdminClusterService] 全量健康检测后触发镜像对账异常: {}", e.getMessage());
        }

        nodes.sort(Comparator.comparing((ClusterNode n) -> !"MASTER".equalsIgnoreCase(n.getRole()))
                .thenComparing(ClusterNode::getJoinTime, Comparator.nullsLast(Comparator.naturalOrder())));

        return clusterNodeMapper.toVOList(nodes);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminClusterNodeVO joinCluster(Long id) {
        ClusterNode node = clusterNodeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "节点不存在"));

        if ("MASTER".equalsIgnoreCase(node.getRole())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "主控制节点为集群管理中枢，无需重新加入");
        }

        String ip = node.getIpAddress().trim();
        int sshPort = node.getSshPort() != null ? node.getSshPort() : 22;
        String sshUser = StringUtils.hasText(node.getSshUser()) ? node.getSshUser().trim() : "root";
        String sshPassword = node.getSshPassword();
        String sshKey = node.getSshKey();

        boolean hasAuth = StringUtils.hasText(sshPassword) || StringUtils.hasText(sshKey);
        if (!hasAuth) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "节点未配置 SSH 登录凭据，无法自动化下发集群入网指令");
        }

        if ("K3S".equalsIgnoreCase(node.getClusterType())) {
            String masterIp = resolveMasterIpForWorker(ip, sshPort);
            if (!StringUtils.hasText(masterIp) || "127.0.0.1".equals(masterIp) || "localhost".equalsIgnoreCase(masterIp)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "平台主节点未配置可供子节点访问的外部 IP (当前为 127.0.0.1)，请在集群设置中配置靶机公网 Host");
            }

            String k3sToken = resolveK3sNodeToken(masterIp);
            if (!StringUtils.hasText(k3sToken)) {
                throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "无法获取 K3s 注册令牌 (Node Token)，请确认控制面 K3s 已正常安装且可访问 /var/lib/rancher/k3s/server/node-token");
            }

            String k3sServerUrl = "https://" + masterIp + ":6443";
            log.info("[AdminClusterService] 正在为工作节点 [{}] 重新执行 K3s agent 入网指令: server={}", ip, k3sServerUrl);
            String k3sJoinCmd = String.format(Locale.ROOT,
                    "if command -v k3s >/dev/null 2>&1; then " +
                    "  systemctl stop k3s-agent 2>/dev/null; " +
                    "  mkdir -p /etc/rancher/k3s && " +
                    "  printf 'server: %s\\ntoken: %s\\n' '%s' '%s' > /etc/rancher/k3s/config.yaml && " +
                    "  (systemctl restart k3s-agent 2>/dev/null || (k3s agent --server %s --token %s >/dev/null 2>&1 &)); " +
                    "else " +
                    "  curl -sfL https://get.k3s.io | K3S_URL=%s K3S_TOKEN=%s sh - ; " +
                    "fi",
                    k3sServerUrl, k3sToken, k3sServerUrl, k3sToken, k3sServerUrl, k3sToken, k3sServerUrl, k3sToken);

            SshExecResult joinRes = executeSshCommand(ip, sshPort, sshUser, sshPassword, sshKey, k3sJoinCmd, 45);
            log.info("[AdminClusterService] 目标节点重新执行 K3s agent 加入结果: exit={}, out={}, err={}",
                    joinRes.exitCode(), joinRes.stdout(), joinRes.stderr());

            boolean joined = false;
            String clusterNodeId = null;
            for (int i = 0; i < 8; i++) {
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                KubernetesClient client = k3sStrategy.getClient();
                if (client != null) {
                    try {
                        for (Node n : client.nodes().list().getItems()) {
                            if (n.getStatus() != null && n.getStatus().getAddresses() != null) {
                                for (NodeAddress a : n.getStatus().getAddresses()) {
                                    if (ip.equalsIgnoreCase(a.getAddress())) {
                                        joined = true;
                                        if (n.getMetadata() != null) {
                                            clusterNodeId = n.getMetadata().getUid();
                                        }
                                        break;
                                    }
                                }
                            }
                            if (joined) break;
                        }
                    } catch (Exception ignored) {}
                }
                if (joined) break;
            }

            if (!joined) {
                node.setInCluster(false);
                node.setStatus("ERROR");
                clusterNodeRepository.save(node);
                throw new BusinessException(HttpStatus.BAD_REQUEST, "工作节点加入 K3s 集群超时或失败，请检查网络 6443 端口连通性及 K3s Token");
            }

            node.setInCluster(true);
            node.setClusterNodeId(clusterNodeId);
            node.setStatus("READY");
            node.setLastHeartbeat(Instant.now());
            ClusterNode saved = clusterNodeRepository.save(node);
            return clusterNodeMapper.toVO(saved);
        }

        String masterIp = resolveMasterIpForWorker(ip, sshPort);
        if (!StringUtils.hasText(masterIp) || "127.0.0.1".equals(masterIp) || "localhost".equalsIgnoreCase(masterIp)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "平台主节点未配置可供子节点访问的外部 IP (当前为 127.0.0.1)，请在集群设置中配置靶机公网 Host");
        }

        String workerToken = getSwarmWorkerJoinToken(masterIp);
        if (!StringUtils.hasText(workerToken)) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "无法获取 Docker Swarm 注册令牌 (Join Token)，请确认控制面 Swarm 已正常初始化");
        }

        log.info("[AdminClusterService] 正在为节点 [{}] 重新执行 Swarm 集群入网指令 (master={}:2377)...", ip, masterIp);
        String swarmJoinCmd = "docker swarm leave --force 2>/dev/null; docker swarm join --token " + workerToken + " " + masterIp + ":2377";
        SshExecResult joinRes = executeSshCommand(ip, sshPort, sshUser, sshPassword, sshKey, swarmJoinCmd, 20);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException ignored) {}

        String workerNodeId = checkWorkerSwarmNodeId(ip, sshPort, sshUser, sshPassword, sshKey);
        Map<String, SwarmNodeInfo> swarmNodes = querySwarmNodesFromManager();
        boolean joinedAndReady = (workerNodeId != null) && swarmNodes.containsKey(workerNodeId)
                && "Ready".equalsIgnoreCase(swarmNodes.get(workerNodeId).status());

        if (!joinedAndReady) {
            String diag = "未知原因";
            if (StringUtils.hasText(joinRes.stderr())) {
                diag = joinRes.stderr().trim();
            } else if (StringUtils.hasText(joinRes.stdout())) {
                diag = joinRes.stdout().trim();
            }
            if (diag.contains("Connection refused") || diag.contains("connection refused")) {
                diag = "连接主节点 " + masterIp + ":2377 被拒绝。主节点可能处于 macOS Hypervisor NAT 内部或 2377 端口未对外监听放通。";
            } else if (diag.contains("Timeout") || diag.contains("timed out") || diag.contains("context deadline exceeded")) {
                diag = "连接主节点 " + masterIp + ":2377 超时。请确认主节点的防火墙、安全组已放通 2377(TCP)、7946(TCP/UDP)、4789(UDP) 端口。";
            }
            node.setInCluster(false);
            node.setStatus("ERROR");
            clusterNodeRepository.save(node);
            throw new BusinessException(HttpStatus.BAD_REQUEST, "工作节点加入 Docker Swarm 失败: " + diag);
        }

        node.setInCluster(true);
        node.setClusterNodeId(workerNodeId);
        node.setStatus("READY");
        node.setLastHeartbeat(Instant.now());
        ClusterNode saved = clusterNodeRepository.save(node);
        return clusterNodeMapper.toVO(saved);
    }

    private void performHealthCheck(ClusterNode node) {
        String containerType = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_TYPE, "DOCKER_SWARM");
        if ("K3S".equalsIgnoreCase(containerType) || "K3S".equalsIgnoreCase(node.getClusterType())) {
            performK3sHealthCheck(node);
            return;
        }

        String ip = node.getIpAddress().trim();
        String masterHost = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_PUBLIC_HOST, "127.0.0.1");
        boolean isMaster = "MASTER".equalsIgnoreCase(node.getRole()) || ip.equalsIgnoreCase(masterHost) || "127.0.0.1".equalsIgnoreCase(ip);

        Map<String, SwarmNodeInfo> swarmNodes = querySwarmNodesFromManager();

        if (isMaster) {
            AdminContainerTestConnectionVO testVo = swarmStrategy.testConnection(null);
            if (testVo.isConnected()) {
                node.setStatus("READY");
                node.setInCluster(true);
                for (SwarmNodeInfo sn : swarmNodes.values()) {
                    if (StringUtils.hasText(sn.managerStatus())) {
                        node.setClusterNodeId(sn.id());
                        break;
                    }
                }
                node.setLastHeartbeat(Instant.now());
                updateLocalNodeMetrics(node);
            } else {
                node.setStatus("OFFLINE");
                node.setInCluster(false);
            }
            return;
        }

        // Worker 节点检测：优先尝试 2376+TLS 双向证书探针，次级兼容 2375 HTTP 探针
        boolean pingOk = false;
        HttpClient client = buildHttpClientForNode(node);
        String baseUrl = getDockerEndpointUrl(node);
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/_ping"))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            pingOk = (resp.statusCode() == 200);
        } catch (Exception ignored) {}

        boolean sshOk = false;
        boolean hasAuth = StringUtils.hasText(node.getSshPassword()) || StringUtils.hasText(node.getSshKey());
        if (!pingOk && hasAuth) {
            SshExecResult sshRes = executeSshCommand(ip, node.getSshPort() != null ? node.getSshPort() : 22,
                    node.getSshUser(), node.getSshPassword(), node.getSshKey(), "docker info --format '{{.ServerVersion}}'", 5);
            sshOk = sshRes.isSuccess();
        }

        boolean daemonAlive = pingOk || sshOk;

        if (daemonAlive) {
            node.setLastHeartbeat(Instant.now());
            if (Boolean.TRUE.equals(node.getTlsEnabled()) && !pingOk && sshOk) {
                String ensure2376 = "if ! systemctl is-active docker >/dev/null 2>&1 || ! ss -tlpn 2>/dev/null | grep -q ':2376 '; then " +
                        "DOCKERD_BIN=$(command -v dockerd 2>/dev/null || which dockerd 2>/dev/null); " +
                        "if [ -z \"$DOCKERD_BIN\" ]; then " +
                        "for p in /usr/sbin/dockerd /usr/bin/dockerd /usr/local/bin/dockerd; do " +
                        "[ -x \"$p\" ] && DOCKERD_BIN=\"$p\" && break; " +
                        "done; " +
                        "fi; " +
                        "[ -z \"$DOCKERD_BIN\" ] && DOCKERD_BIN=/usr/bin/dockerd; " +
                        "mkdir -p /etc/systemd/system/docker.service.d && " +
                        "printf '[Service]\\nExecStart=\\nExecStart=%s -H fd:// -H tcp://0.0.0.0:2376 --tlsverify --tlscacert=/etc/docker/certs/ca.pem --tlscert=/etc/docker/certs/server-cert.pem --tlskey=/etc/docker/certs/server-key.pem\\n' \"$DOCKERD_BIN\" > /etc/systemd/system/docker.service.d/override.conf && " +
                        "systemctl daemon-reload && systemctl restart docker; " +
                        "fi";
                executeSshCommand(ip, node.getSshPort() != null ? node.getSshPort() : 22,
                        node.getSshUser(), node.getSshPassword(), node.getSshKey(), ensure2376, 15);
            }
            updateRemoteNodeMetrics(node);

            // 严格核验 Worker 是否真正在 Swarm 集群中就绪活跃
            String workerNodeId = checkWorkerSwarmNodeId(ip, node.getSshPort() != null ? node.getSshPort() : 22,
                    node.getSshUser(), node.getSshPassword(), node.getSshKey());
            boolean inSwarm = false;
            if (StringUtils.hasText(workerNodeId) && swarmNodes.containsKey(workerNodeId)) {
                SwarmNodeInfo sInfo = swarmNodes.get(workerNodeId);
                inSwarm = "Ready".equalsIgnoreCase(sInfo.status());
            } else {
                for (SwarmNodeInfo sn : swarmNodes.values()) {
                    if (ip.equalsIgnoreCase(sn.ip()) || ip.equalsIgnoreCase(sn.hostname())) {
                        workerNodeId = sn.id();
                        inSwarm = "Ready".equalsIgnoreCase(sn.status());
                        break;
                    }
                }
            }

            node.setInCluster(inSwarm);
            node.setClusterNodeId(workerNodeId);

            if (inSwarm) {
                node.setStatus("READY");
            } else {
                node.setStatus("ERROR");
                log.warn("[AdminClusterService] Worker 节点 [{}] 宿主机在线，但尚未加入 Docker Swarm 编排集群 (或状态非就绪)", ip);
            }
        } else {
            node.setStatus("OFFLINE");
            node.setInCluster(false);
            node.setContainerCount(0);
            log.warn("[AdminClusterService] 子节点 [{}] 存活探测失败，已置为 OFFLINE 状态", ip);
        }
    }

    private void updateRemoteNodeMetrics(ClusterNode node) {
        String ip = node.getIpAddress().trim();
        HttpClient client = buildHttpClientForNode(node);
        String baseUrl = getDockerEndpointUrl(node);
        try {
            HttpRequest imgReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/images/json"))
                    .timeout(Duration.ofSeconds(4))
                    .GET()
                    .build();
            HttpResponse<String> imgResp = client.send(imgReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (imgResp.statusCode() == 200 && StringUtils.hasText(imgResp.body())) {
                JSONArray arr = JSONUtil.parseArray(imgResp.body());
                node.setImageCount(arr.size());
            }

            HttpRequest cntReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/containers/json?all=false"))
                    .timeout(Duration.ofSeconds(4))
                    .GET()
                    .build();
            HttpResponse<String> cntResp = client.send(cntReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (cntResp.statusCode() == 200 && StringUtils.hasText(cntResp.body())) {
                JSONArray arr = JSONUtil.parseArray(cntResp.body());
                node.setContainerCount(arr.size());
            }
        } catch (Exception e) {
            log.debug("[AdminClusterService] 采集远程节点 [{}] 资源失败: {}", ip, e.getMessage());
        }
    }

    private void updateLocalNodeMetrics(ClusterNode node) {
        try {
            long totalImages = clusterImageRepository.count();
            node.setImageCount((int) totalImages);
            long runningInstances = ctfChallengeInstanceRepository.countByStatus(InstanceStartupStatus.RUNNING);
            node.setContainerCount((int) runningInstances);
        } catch (Exception ignored) {}
    }

    private void checkIpReachable(String ip, int candidatePort) {
        if (!StringUtils.hasText(ip)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "目标节点 IP 地址不能为空");
        }
        String trimmedIp = ip.trim();
        try {
            InetAddress address = InetAddress.getByName(trimmedIp);
            if (address.isLoopbackAddress() || address.isAnyLocalAddress() || "127.0.0.1".equals(trimmedIp) || "localhost".equalsIgnoreCase(trimmedIp)) {
                return;
            }
            if (address.isReachable(2000)) {
                return;
            }
        } catch (Exception ignored) {}

        try {
            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
            ProcessBuilder pb = isWindows
                    ? new ProcessBuilder("ping", "-n", "1", "-w", "2000", trimmedIp)
                    : new ProcessBuilder("ping", "-c", "1", "-W", "2", trimmedIp);
            Process process = pb.start();
            if (process.waitFor(3, TimeUnit.SECONDS) && process.exitValue() == 0) {
                return;
            }
        } catch (Exception ignored) {}

        int probePort = candidatePort > 0 ? candidatePort : 22;
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(trimmedIp, probePort), 2500);
            return;
        } catch (ConnectException ce) {
            return;
        } catch (Exception ignored) {}

        throw new BusinessException(HttpStatus.BAD_REQUEST, "平台无法连接到目标节点 IP (" + trimmedIp + ")，请检查网络连通性或防火墙规则");
    }

    private void verifySshConnectivity(String host, int port, String user, String password, String privateKey) {
        JSch jsch = new JSch();
        Session session = null;
        try {
            if (StringUtils.hasText(privateKey)) {
                jsch.addIdentity("key", privateKey.trim().getBytes(StandardCharsets.UTF_8), null, null);
            }
            session = jsch.getSession(StringUtils.hasText(user) ? user.trim() : "root", host.trim(), port > 0 ? port : 22);
            if (StringUtils.hasText(password)) {
                session.setPassword(password);
            }
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            config.put("PreferredAuthentications", "publickey,password,keyboard-interactive");
            session.setConfig(config);
            session.setTimeout(8000);
            session.connect(8000);
        } catch (Exception e) {
            String rawMsg = e.getMessage() != null ? e.getMessage() : "连接超时或拒绝连接";
            String friendlyMsg = rawMsg;
            if (rawMsg.contains("Auth fail")) {
                friendlyMsg = "SSH 身份凭据认证失败，请检查用户名与密码/私钥是否匹配";
            } else if (rawMsg.contains("Connection refused")) {
                friendlyMsg = "SSH 端口 " + port + " 连接被拒绝，请确认目标节点 SSH 服务已开启";
            } else if (rawMsg.contains("timeout") || rawMsg.contains("timed out")) {
                friendlyMsg = "SSH 连接超时，请确认目标主机 SSH 端口未被安全组或防火墙拦截";
            } else if (rawMsg.contains("invalid privatekey")) {
                friendlyMsg = "SSH 私钥格式无效，请确认私钥为标准 OpenSSH 或 PEM 格式";
            } else if (rawMsg.contains("Network is down") || rawMsg.contains("Network is unreachable") || rawMsg.contains("No route to host")) {
                friendlyMsg = "目标网络不可达或网络接口未启用 (" + rawMsg + ")";
            }
            log.warn("[AdminClusterService] SSH 连接与凭据校验失败: host={}:{}, error={}", host, port, rawMsg);
            throw new BusinessException(HttpStatus.BAD_REQUEST, "SSH 连接失败: " + friendlyMsg);
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    private void setupDockerTlsAndValidate(String ip, int sshPort, String sshUser, String sshPassword, String sshKey, ClusterNode node) {
        setupDockerTlsAndValidate(ip, sshPort, sshUser, sshPassword, sshKey, node, null);
    }

    private void setupDockerTlsAndValidate(String ip, int sshPort, String sshUser, String sshPassword, String sshKey, ClusterNode node, SshPrivilegeMode privMode) {
        log.info("[AdminClusterService] 开始为节点 [{}] 生成 2376+TLS 双向安全证书...", ip);

        // 1. 确保远程节点具备 openssl 命令行工具
        String checkOpenssl = "which openssl || (command -v openssl) || (apt-get update && apt-get install -y openssl) || (yum install -y openssl)";
        SshExecResult opensslRes = executePrivilegedSshCommand(ip, sshPort, sshUser, sshPassword, sshKey, checkOpenssl, 30, privMode);
        if (!opensslRes.isSuccess() && !StringUtils.hasText(opensslRes.stdout())) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "目标节点缺少 openssl 工具且自动化安装失败，无法生成 TLS 证书");
        }

        // 2. 自动化生成 CA、Server 端与平台 Client 端证书（有效期 10 年），SAN 绑定目标节点 IP 与 127.0.0.1
        String script = String.format(Locale.ROOT,
                "mkdir -p /etc/docker/certs && cd /etc/docker/certs && " +
                "openssl genrsa -out ca-key.pem 2048 2>/dev/null && " +
                "openssl req -new -x509 -days 3650 -key ca-key.pem -sha256 -out ca.pem -subj \"/CN=VolurCA\" 2>/dev/null && " +
                "openssl genrsa -out server-key.pem 2048 2>/dev/null && " +
                "openssl req -subj \"/CN=%s\" -sha256 -new -key server-key.pem -out server.csr 2>/dev/null && " +
                "printf \"subjectAltName = IP:%s,IP:127.0.0.1\\nextendedKeyUsage = serverAuth\\n\" > extfile.cnf && " +
                "openssl x509 -req -days 3650 -sha256 -in server.csr -CA ca.pem -CAkey ca-key.pem -CAcreateserial -out server-cert.pem -extfile extfile.cnf 2>/dev/null && " +
                "openssl genrsa -out key.pem 2048 2>/dev/null && " +
                "openssl req -subj \"/CN=volur-platform-client\" -new -key key.pem -out client.csr 2>/dev/null && " +
                "printf \"extendedKeyUsage = clientAuth\\n\" > extfile-client.cnf && " +
                "openssl x509 -req -days 3650 -sha256 -in client.csr -CA ca.pem -CAkey ca-key.pem -CAcreateserial -out cert.pem -extfile extfile-client.cnf 2>/dev/null && " +
                "openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in key.pem -out key-pkcs8.pem 2>/dev/null && " +
                "chmod 0400 ca-key.pem server-key.pem key.pem key-pkcs8.pem && " +
                "chmod 0444 ca.pem server-cert.pem cert.pem && " +
                "rm -f server.csr client.csr extfile.cnf extfile-client.cnf",
                ip, ip);

        SshExecResult genRes = executePrivilegedSshCommand(ip, sshPort, sshUser, sshPassword, sshKey, script, 35, privMode);
        if (!genRes.isSuccess()) {
            log.error("[AdminClusterService] 目标节点 [{}] 生成 TLS 证书脚本执行失败: exit={}, err={}", ip, genRes.exitCode(), genRes.stderr());
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "在目标节点生成 Docker TLS 证书失败: " + genRes.stderr());
        }

        // 3. 读取证书内容到平台
        SshExecResult caRes = executePrivilegedSshCommand(ip, sshPort, sshUser, sshPassword, sshKey, "cat /etc/docker/certs/ca.pem", 10, privMode);
        SshExecResult certRes = executePrivilegedSshCommand(ip, sshPort, sshUser, sshPassword, sshKey, "cat /etc/docker/certs/cert.pem", 10, privMode);
        SshExecResult keyRes = executePrivilegedSshCommand(ip, sshPort, sshUser, sshPassword, sshKey, "cat /etc/docker/certs/key.pem", 10, privMode);
        SshExecResult pkcs8Res = executePrivilegedSshCommand(ip, sshPort, sshUser, sshPassword, sshKey, "cat /etc/docker/certs/key-pkcs8.pem", 10, privMode);

        if (!caRes.isSuccess() || !certRes.isSuccess() || !keyRes.isSuccess() ||
                !StringUtils.hasText(caRes.stdout()) || !StringUtils.hasText(certRes.stdout()) || !StringUtils.hasText(keyRes.stdout())) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "无法从目标节点回读生成的 TLS 证书文件，请检查远程路径权限");
        }

        String caPem = caRes.stdout().trim();
        String certPem = certRes.stdout().trim();
        String keyPem = keyRes.stdout().trim();
        String pkcs8Pem = pkcs8Res.isSuccess() && StringUtils.hasText(pkcs8Res.stdout()) ? pkcs8Res.stdout().trim() : keyPem;

        // 4. 保存证书到平台本地磁盘目录与数据库实体
        Path localCertDir = Paths.get(System.getProperty("user.dir"), "data", "certs", "nodes", ip);
        try {
            Files.createDirectories(localCertDir);
            Files.writeString(localCertDir.resolve("ca.pem"), caPem);
            Files.writeString(localCertDir.resolve("cert.pem"), certPem);
            Files.writeString(localCertDir.resolve("key.pem"), keyPem);
            Files.writeString(localCertDir.resolve("key-pkcs8.pem"), pkcs8Pem);
        } catch (Exception e) {
            log.warn("[AdminClusterService] 保存节点 TLS 证书至平台本地磁盘异常: {}", e.getMessage());
        }

        node.setDockerPort(2376);
        node.setTlsEnabled(true);
        node.setTlsCertPath(localCertDir.toAbsolutePath().toString());
        node.setTlsCaCert(caPem);
        node.setTlsClientCert(certPem);
        node.setTlsClientKey(keyPem);

        // 5. 编排配置远程 Docker Daemon 监听 2376 + TLS
        String config2376Cmd = "mkdir -p /etc/systemd/system/docker.service.d && " +
                "DOCKERD_BIN=$(command -v dockerd 2>/dev/null || which dockerd 2>/dev/null); " +
                "if [ -z \"$DOCKERD_BIN\" ]; then " +
                "for p in /usr/sbin/dockerd /usr/bin/dockerd /usr/local/bin/dockerd; do " +
                "[ -x \"$p\" ] && DOCKERD_BIN=\"$p\" && break; " +
                "done; " +
                "fi; " +
                "[ -z \"$DOCKERD_BIN\" ] && DOCKERD_BIN=/usr/bin/dockerd; " +
                "printf '[Service]\\nExecStart=\\nExecStart=%s -H fd:// -H tcp://0.0.0.0:2376 --tlsverify --tlscacert=/etc/docker/certs/ca.pem --tlscert=/etc/docker/certs/server-cert.pem --tlskey=/etc/docker/certs/server-key.pem\\n' \"$DOCKERD_BIN\" > /etc/systemd/system/docker.service.d/override.conf && " +
                "systemctl daemon-reload && systemctl restart docker";

        SshExecResult configRes = executePrivilegedSshCommand(ip, sshPort, sshUser, sshPassword, sshKey, config2376Cmd, 30, privMode);
        if (!configRes.isSuccess()) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "在目标节点配置 2376+TLS override.conf 失败: " + configRes.stderr());
        }

        // 6. 验证 Docker 是否正常运行
        boolean active = false;
        for (int i = 0; i < 8; i++) {
            try {
                Thread.sleep(800);
            } catch (InterruptedException ignored) {}
            SshExecResult actRes = executePrivilegedSshCommand(ip, sshPort, sshUser, sshPassword, sshKey, "systemctl is-active docker", 5, privMode);
            if (actRes.isSuccess() && "active".equalsIgnoreCase(actRes.stdout().trim())) {
                active = true;
                break;
            }
        }

        if (!active) {
            SshExecResult logRes = executePrivilegedSshCommand(ip, sshPort, sshUser, sshPassword, sshKey, "journalctl -u docker -n 25 --no-pager", 6, privMode);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "Docker 守护进程配置 2376 TLS 后未能正常运行: " + logRes.stdout());
        }

        // 本地 TLS 握手测试
        String testCmd = "docker --tlsverify --tlscacert=/etc/docker/certs/ca.pem --tlscert=/etc/docker/certs/cert.pem --tlskey=/etc/docker/certs/key.pem -H=127.0.0.1:2376 info --format '{{.ServerVersion}}'";
        SshExecResult testRes = executePrivilegedSshCommand(ip, sshPort, sshUser, sshPassword, sshKey, testCmd, 10, privMode);
        if (!testRes.isSuccess()) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "Docker 2376 TLS 双向证书本地握手验证失败: " + testRes.stderr());
        }

        // 平台直接向目标节点发起 HTTPS 连通探针测试
        boolean remoteTlsReachable = DockerTlsUtil.isDockerTlsReachable(ip, 2376, caPem, certPem, pkcs8Pem);
        if (remoteTlsReachable) {
            log.info("[AdminClusterService] 平台与目标节点 [{}:2376] 双向 TLS 握手测试成功", ip);
        } else {
            log.warn("[AdminClusterService] 节点 [{}] Docker 2376+TLS 本地已正常运行，但平台直连超时，请确认云网络安全组已放行 2376 端口", ip);
        }
    }

    private HttpClient buildHttpClientForNode(ClusterNode node) {
        return DockerTlsUtil.buildHttpClientForNode(node, Duration.ofSeconds(3));
    }

    private String getDockerEndpointUrl(ClusterNode node) {
        return DockerTlsUtil.getDockerEndpointUrl(node);
    }

    private boolean isDockerHttpReachable(String host, int port) {
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + host + ":" + port + "/_ping"))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            return resp.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    public record SshExecResult(int exitCode, String stdout, String stderr) {
        public boolean isSuccess() {
            return exitCode == 0;
        }
    }

    private enum SshPrivilegeMode {
        ROOT,
        SUDO_NOPASSWD,
        SUDO_PASSWORD,
        UNPRIVILEGED
    }

    private SshPrivilegeMode detectPrivilegeMode(String host, int port, String user, String password, String privateKey) {
        if (!StringUtils.hasText(user) || "root".equalsIgnoreCase(user.trim())) {
            return SshPrivilegeMode.ROOT;
        }

        // 1. 尝试免密 sudo 探测: sudo -n true
        SshExecResult noPassRes = executeSshCommandInternal(host, port, user, password, privateKey, "sudo -n true", 5, null);
        if (noPassRes.isSuccess()) {
            log.info("[AdminClusterService] 目标节点 [{}] 用户 [{}] 具备免密 sudo 特权 (NOPASSWD)", host, user);
            return SshPrivilegeMode.SUDO_NOPASSWD;
        }

        // 2. 若未通过免密，但提供了密码（ssh 密码即为 sudo 密码），尝试密码提权: sudo -S -p '' true
        if (StringUtils.hasText(password)) {
            SshExecResult passRes = executeSshCommandInternal(host, port, user, password, privateKey, "sudo -S -p '' true", 5, password);
            if (passRes.isSuccess()) {
                log.info("[AdminClusterService] 目标节点 [{}] 用户 [{}] 验证 sudo 密码提权成功", host, user);
                return SshPrivilegeMode.SUDO_PASSWORD;
            }
            log.warn("[AdminClusterService] 目标节点 [{}] 用户 [{}] sudo 密码提权失败: exit={}, err={}",
                    host, user, passRes.exitCode(), passRes.stderr());
        }

        log.warn("[AdminClusterService] 目标节点 [{}] 用户 [{}] 既非免密 sudo 亦无法通过密码提权: err={}",
                host, user, noPassRes.stderr());
        return SshPrivilegeMode.UNPRIVILEGED;
    }

    private SshExecResult executeSshCommand(String host, int port, String user, String password, String privateKey, String command, int timeoutSeconds) {
        return executePrivilegedSshCommand(host, port, user, password, privateKey, command, timeoutSeconds, null);
    }

    private SshExecResult executePrivilegedSshCommand(String host, int port, String user, String password, String privateKey, String command, int timeoutSeconds) {
        return executePrivilegedSshCommand(host, port, user, password, privateKey, command, timeoutSeconds, null);
    }

    private SshExecResult executePrivilegedSshCommand(String host, int port, String user, String password, String privateKey, String command, int timeoutSeconds, SshPrivilegeMode privMode) {
        if (!StringUtils.hasText(user) || "root".equalsIgnoreCase(user.trim())) {
            return executeSshCommandInternal(host, port, user, password, privateKey, command, timeoutSeconds, null);
        }

        if (privMode == null) {
            privMode = StringUtils.hasText(password) ? SshPrivilegeMode.SUDO_PASSWORD : SshPrivilegeMode.SUDO_NOPASSWD;
        }

        String escaped = command.replace("'", "'\\''");
        if (privMode == SshPrivilegeMode.SUDO_PASSWORD && StringUtils.hasText(password)) {
            String wrapped = "sudo -S -p '' -- sh -c '" + escaped + "'";
            return executeSshCommandInternal(host, port, user, password, privateKey, wrapped, timeoutSeconds, password);
        } else {
            String wrapped = "sudo -n -- sh -c '" + escaped + "'";
            return executeSshCommandInternal(host, port, user, password, privateKey, wrapped, timeoutSeconds, null);
        }
    }

    private SshExecResult executeSshCommandInternal(String host, int port, String user, String password, String privateKey, String command, int timeoutSeconds, String stdinInput) {
        JSch jsch = new JSch();
        Session session = null;
        ChannelExec channel = null;
        try {
            if (StringUtils.hasText(privateKey)) {
                jsch.addIdentity("key", privateKey.trim().getBytes(StandardCharsets.UTF_8), null, null);
            }
            session = jsch.getSession(StringUtils.hasText(user) ? user.trim() : "root", host.trim(), port > 0 ? port : 22);
            if (StringUtils.hasText(password)) {
                session.setPassword(password);
            }
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            config.put("PreferredAuthentications", "publickey,password,keyboard-interactive");
            session.setConfig(config);
            session.setTimeout(timeoutSeconds * 1000);
            session.connect(10000);

            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);

            if (StringUtils.hasText(stdinInput)) {
                ByteArrayInputStream bin = new ByteArrayInputStream((stdinInput.stripTrailing() + "\n").getBytes(StandardCharsets.UTF_8));
                channel.setInputStream(bin);
            } else {
                channel.setInputStream(null);
            }

            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            ByteArrayOutputStream stderr = new ByteArrayOutputStream();
            channel.setOutputStream(stdout);
            channel.setErrStream(stderr);
            channel.connect(10000);

            long deadline = System.currentTimeMillis() + (timeoutSeconds * 1000L);
            while (!channel.isClosed() && System.currentTimeMillis() < deadline) {
                try {
                    Thread.sleep(150);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            int exitStatus = channel.getExitStatus();
            return new SshExecResult(exitStatus, stdout.toString(StandardCharsets.UTF_8), stderr.toString(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.warn("[AdminClusterService] SSH 执行异常: host={}, cmd={}, error={}", host, command, e.getMessage());
            return new SshExecResult(-1, "", e.getMessage());
        } finally {
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    private String resolveMasterIpForWorker(String workerIp, int workerPort) {
        String configuredHost = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_PUBLIC_HOST, "");
        if (StringUtils.hasText(configuredHost) && !"127.0.0.1".equalsIgnoreCase(configuredHost.trim()) && !"localhost".equalsIgnoreCase(configuredHost.trim())) {
            return configuredHost.trim();
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(workerIp, workerPort), 2000);
            InetAddress localAddress = socket.getLocalAddress();
            if (localAddress != null && StringUtils.hasText(localAddress.getHostAddress()) && !"127.0.0.1".equals(localAddress.getHostAddress())) {
                return localAddress.getHostAddress();
            }
        } catch (Exception e) {
            log.debug("[AdminClusterService] 探测发往目标 Worker 节点的本机出口 IP 异常: {}", e.getMessage());
        }
        return "127.0.0.1";
    }

    private String getSwarmWorkerJoinToken(String masterIp) {
        String swarmApiUrl = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_DOCKER_SWARM_API_URL, "unix:///var/run/docker.sock");
        List<String> cmd = new ArrayList<>();
        cmd.add("docker");
        if (StringUtils.hasText(swarmApiUrl) && !swarmApiUrl.startsWith("unix://")) {
            cmd.add("-H");
            cmd.add(swarmApiUrl.trim());
        }
        cmd.add("swarm");
        cmd.add("join-token");
        cmd.add("-q");
        cmd.add("worker");

        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            Process p = pb.start();
            boolean finished = p.waitFor(6, TimeUnit.SECONDS);
            if (finished && p.exitValue() == 0) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                    String token = reader.readLine();
                    if (StringUtils.hasText(token)) {
                        return token.trim();
                    }
                }
            }

            // 如果主节点尚未初始化为 Swarm Manager，自动初始化
            List<String> initCmd = new ArrayList<>();
            initCmd.add("docker");
            if (StringUtils.hasText(swarmApiUrl) && !swarmApiUrl.startsWith("unix://")) {
                initCmd.add("-H");
                initCmd.add(swarmApiUrl.trim());
            }
            initCmd.add("swarm");
            initCmd.add("init");
            if (StringUtils.hasText(masterIp)) {
                initCmd.add("--advertise-addr");
                initCmd.add(masterIp.trim());
            }
            Process initProc = new ProcessBuilder(initCmd).start();
            initProc.waitFor(10, TimeUnit.SECONDS);

            Process p2 = new ProcessBuilder(cmd).start();
            if (p2.waitFor(6, TimeUnit.SECONDS) && p2.exitValue() == 0) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(p2.getInputStream(), StandardCharsets.UTF_8))) {
                    String token = reader.readLine();
                    if (StringUtils.hasText(token)) {
                        return token.trim();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[AdminClusterService] 获取 Swarm Join Token 异常: {}", e.getMessage());
        }
        return null;
    }

    private void applyResourceIsolation(boolean enabled) {
        List<ClusterNode> nodes = clusterNodeRepository.findAll();
        boolean hasWorker = nodes.stream().anyMatch(n -> "WORKER".equalsIgnoreCase(n.getRole()));

        if (enabled && hasWorker) {
            nodes.stream().filter(n -> "MASTER".equalsIgnoreCase(n.getRole())).forEach(m -> {
                m.setIsResourceNode(false);
                clusterNodeRepository.save(m);
            });
            nodes.stream().filter(n -> "WORKER".equalsIgnoreCase(n.getRole())).forEach(w -> {
                w.setIsResourceNode(true);
                clusterNodeRepository.save(w);
            });
        } else if (!enabled || !hasWorker) {
            nodes.stream().filter(n -> "MASTER".equalsIgnoreCase(n.getRole())).forEach(m -> {
                m.setIsResourceNode(true);
                clusterNodeRepository.save(m);
            });
        }
    }

    public record SwarmNodeInfo(String id, String hostname, String status, String availability, String managerStatus, String ip) {}

    private Map<String, SwarmNodeInfo> querySwarmNodesFromManager() {
        Map<String, SwarmNodeInfo> map = new LinkedHashMap<>();
        String swarmApiUrl = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_DOCKER_SWARM_API_URL, "unix:///var/run/docker.sock");
        List<String> cmd = new ArrayList<>();
        cmd.add("docker");
        if (StringUtils.hasText(swarmApiUrl) && !swarmApiUrl.startsWith("unix://")) {
            cmd.add("-H");
            cmd.add(swarmApiUrl.trim());
        }
        cmd.add("node");
        cmd.add("ls");
        cmd.add("--format");
        cmd.add("{{.ID}}\t{{.Hostname}}\t{{.Status}}\t{{.Availability}}\t{{.ManagerStatus}}");

        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
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
                        String availability = parts.length > 3 ? parts[3].trim() : "";
                        String managerStatus = parts.length > 4 ? parts[4].trim() : "";
                        if (StringUtils.hasText(nodeId)) {
                            String addr = querySwarmNodeAddr(nodeId, swarmApiUrl);
                            SwarmNodeInfo info = new SwarmNodeInfo(nodeId, hostname, status, availability, managerStatus, addr);
                            map.put(nodeId, info);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("[AdminClusterService] 获取 Swarm 节点列表提示: {}", e.getMessage());
        }
        return map;
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

    private String checkWorkerSwarmNodeId(String host, int port, String user, String password, String privateKey) {
        return checkWorkerSwarmNodeId(host, port, user, password, privateKey, null);
    }

    private String checkWorkerSwarmNodeId(String host, int port, String user, String password, String privateKey, SshPrivilegeMode privMode) {
        boolean hasAuth = StringUtils.hasText(password) || StringUtils.hasText(privateKey);
        if (!hasAuth) {
            return null;
        }
        SshExecResult res = executePrivilegedSshCommand(host, port, user, password, privateKey,
                "docker info --format '{{.Swarm.LocalNodeState}}\t{{.Swarm.NodeID}}'", 6, privMode);
        if (res.isSuccess() && StringUtils.hasText(res.stdout())) {
            String out = res.stdout().trim();
            String[] parts = out.split("\t");
            String state = parts.length > 0 ? parts[0].trim() : "";
            String nodeId = parts.length > 1 ? parts[1].trim() : "";
            if ("active".equalsIgnoreCase(state) && StringUtils.hasText(nodeId)) {
                return nodeId;
            }
        }
        return null;
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
                .inCluster(true)
                .status("READY")
                .osInfo("Linux 6.6.0 (Ubuntu 24.04 LTS)")
                .dockerVersion("DOCKER_SWARM".equalsIgnoreCase(containerType) ? "Docker Engine 27.1" : "containerd 1.7.15")
                .cpuCores(8)
                .memoryBytes(17179869184L) // 16GB
                .containerCount(0)
                .imageCount(0)
                .joinTime(Instant.now())
                .lastHeartbeat(Instant.now())
                .build();

        clusterNodeRepository.save(masterNode);
        log.info("[AdminClusterService] 集群首个主节点已自动初始化注册: ip={}", publicHost);
    }

    private void setSetting(String key, String value) {
        systemSettingService.updateSetting(new com.humolix.volur.system.domain.dto.SystemSettingUpdateDTO(key, value));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<AdminClusterNodeVO> syncK3sClusterNodes() {
        List<K3sRuntimeStrategy.K3sDiscoveredNode> discovered = k3sStrategy.scanK3sNodes();
        if (discovered.isEmpty()) {
            log.warn("[AdminClusterService] K3s 集群未扫描到任何在线节点，跳过同步");
            return listNodes();
        }

        String configuredMasterHost = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_PUBLIC_HOST, "127.0.0.1");
        boolean hasDiscoveredWorker = discovered.stream().anyMatch(d -> "WORKER".equalsIgnoreCase(d.role()));
        boolean hasDiscoveredMaster = discovered.stream().anyMatch(d -> "MASTER".equalsIgnoreCase(d.role()));
        if (hasDiscoveredMaster) {
            clusterNodeRepository.findByNodeName("volur-master-01").ifPresent(placeholder -> {
                if ("127.0.0.1".equals(placeholder.getIpAddress())) {
                    clusterNodeRepository.delete(placeholder);
                }
            });
        }

        for (K3sRuntimeStrategy.K3sDiscoveredNode d : discovered) {
            Optional<ClusterNode> existingOpt = clusterNodeRepository.findByNodeName(d.nodeName());
            if (existingOpt.isEmpty()) {
                existingOpt = clusterNodeRepository.findByIpAddress(d.ipAddress());
            }

            ClusterNode node;
            if (existingOpt.isPresent()) {
                node = existingOpt.get();
                node.setNodeName(d.nodeName());
                node.setIpAddress(d.ipAddress());
                node.setClusterType("K3S");
                node.setRole(d.role());
                node.setStatus(d.status());
                node.setInCluster(true);
                node.setClusterNodeId(d.uid());
                node.setOsInfo(d.osInfo());
                node.setDockerVersion(d.containerRuntimeVersion());
                node.setCpuCores(d.cpuCores());
                node.setMemoryBytes(d.memoryBytes());
                node.setLastHeartbeat(Instant.now());
                if ("WORKER".equalsIgnoreCase(d.role())) {
                    node.setIsResourceNode(true);
                } else if ("MASTER".equalsIgnoreCase(d.role())) {
                    node.setIsResourceNode(!hasDiscoveredWorker);
                }
            } else {
                boolean isResource = "WORKER".equalsIgnoreCase(d.role()) || !hasDiscoveredWorker;
                node = ClusterNode.builder()
                        .nodeName(d.nodeName())
                        .ipAddress(d.ipAddress())
                        .sshPort(22)
                        .sshUser("root")
                        .clusterType("K3S")
                        .role(d.role())
                        .isResourceNode(isResource)
                        .status(d.status())
                        .osInfo(d.osInfo())
                        .dockerVersion(d.containerRuntimeVersion())
                        .cpuCores(d.cpuCores())
                        .memoryBytes(d.memoryBytes())
                        .containerCount(0)
                        .imageCount(0)
                        .inCluster(true)
                        .clusterNodeId(d.uid())
                        .joinTime(Instant.now())
                        .lastHeartbeat(Instant.now())
                        .build();
            }

            clusterNodeRepository.save(node);
            log.info("[AdminClusterService] 已同步 K3s 集群节点: name={}, ip={}, role={}, status={}",
                    node.getNodeName(), node.getIpAddress(), node.getRole(), node.getStatus());
        }

        // 若 Master 节点拥有具体物理 IP 且当前系统 publicHost 仍为 127.0.0.1，自动对齐
        discovered.stream().filter(d -> "MASTER".equalsIgnoreCase(d.role())).findFirst().ifPresent(master -> {
            if (!"127.0.0.1".equals(master.ipAddress()) && "127.0.0.1".equals(configuredMasterHost)) {
                setSetting(SystemSettingKeys.CONTAINER_PUBLIC_HOST, master.ipAddress());
                log.info("[AdminClusterService] 根据 K3s Master 节点自动校正 CONTAINER_PUBLIC_HOST: {}", master.ipAddress());
            }
        });

        return listNodes();
    }

    private void performK3sHealthCheck(ClusterNode node) {
        KubernetesClient client = k3sStrategy.getClient();
        if (client == null) {
            node.setStatus("OFFLINE");
            node.setInCluster(false);
            return;
        }

        try {
            Node k8sNode = null;
            if (StringUtils.hasText(node.getNodeName())) {
                try {
                    k8sNode = client.nodes().withName(node.getNodeName().trim()).get();
                } catch (Exception ignored) {}
            }
            if (k8sNode == null && StringUtils.hasText(node.getIpAddress())) {
                for (Node n : client.nodes().list().getItems()) {
                    if (n.getStatus() != null && n.getStatus().getAddresses() != null) {
                        for (NodeAddress a : n.getStatus().getAddresses()) {
                            if (node.getIpAddress().trim().equalsIgnoreCase(a.getAddress())) {
                                k8sNode = n;
                                break;
                            }
                        }
                    }
                    if (k8sNode != null) break;
                }
            }

            if (k8sNode != null) {
                boolean ready = false;
                if (k8sNode.getStatus() != null && k8sNode.getStatus().getConditions() != null) {
                    for (NodeCondition cond : k8sNode.getStatus().getConditions()) {
                        if ("Ready".equalsIgnoreCase(cond.getType())) {
                            ready = "True".equalsIgnoreCase(cond.getStatus());
                            break;
                        }
                    }
                }
                node.setStatus(ready ? "READY" : "OFFLINE");
                node.setInCluster(true);
                node.setLastHeartbeat(Instant.now());
                if (k8sNode.getMetadata() != null && StringUtils.hasText(k8sNode.getMetadata().getUid())) {
                    node.setClusterNodeId(k8sNode.getMetadata().getUid());
                }
            } else {
                node.setStatus("OFFLINE");
                node.setInCluster(false);
            }
        } catch (Exception e) {
            log.warn("[AdminClusterService] K3s 节点 [{}] 存活检测异常: {}", node.getNodeName(), e.getMessage());
            node.setStatus("OFFLINE");
        }
    }

    private String resolveK3sNodeToken(String masterIp) {
        String token = k3sStrategy.resolveK3sToken();
        if (StringUtils.hasText(token)) {
            return token.trim();
        }
        List<ClusterNode> masters = clusterNodeRepository.findByRole("MASTER");
        for (ClusterNode master : masters) {
            if (StringUtils.hasText(master.getSshPassword()) || StringUtils.hasText(master.getSshKey())) {
                SshExecResult res = executeSshCommand(master.getIpAddress(),
                        master.getSshPort() != null ? master.getSshPort() : 22,
                        master.getSshUser(), master.getSshPassword(), master.getSshKey(),
                        "cat /var/lib/rancher/k3s/server/node-token 2>/dev/null", 5);
                if (res.isSuccess() && StringUtils.hasText(res.stdout().trim())) {
                    return res.stdout().trim();
                }
            }
        }
        return null;
    }

}
