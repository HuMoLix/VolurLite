package com.humolix.volur.container.util;

import cn.hutool.core.util.RandomUtil;
import com.humolix.volur.container.domain.entity.CtfChallengeInstance;
import com.humolix.volur.container.domain.enums.InstanceStartupStatus;
import com.humolix.volur.container.repository.CtfChallengeInstanceRepository;
import com.humolix.volur.framework.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 容器动态端口分配与冲突避免组件
 * <p>
 * 综合多层级防冲突机制：
 * 1. 数据库活跃容器实例已占端口（RUNNING / STARTING / RESTARTING）；
 * 2. 外部集群（如 K3s 现有 Service NodePort / Swarm 服务）已占端口；
 * 3. 并发启动内存临时保留表（防毫秒级并发冲突）；
 * 4. 实时网络探活探测（本机 ServerSocket bind 测试，远程节点 TCP connect 测试）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContainerPortAllocator {

    private static final Pattern PORT_PATTERN = Pattern.compile(":(\\d{2,5})");
    private static final long RESERVATION_TTL_MS = 60_000L; // 预占保留 60 秒

    private final CtfChallengeInstanceRepository instanceRepository;

    /**
     * 临时保留端口表（防止高并发启动时分配到相同端口）
     * Key: host:port 或 *:port
     * Value: 过期时间戳 (System.currentTimeMillis() + TTL)
     */
    private final ConcurrentHashMap<String, Long> reservedPorts = new ConcurrentHashMap<>();

    /**
     * 为指定目标主机分配自定义端口范围 [portStart, portEnd] 内的可用空闲端口（避开已占用端口）
     *
     * @param targetHost             目标主机地址（IP 或域名）
     * @param portStart              起始端口
     * @param portEnd                结束端口
     * @param additionalExcludedPorts 外部额外排除的端口集合（如 K3s 现有服务的 NodePort）
     * @return 可用且未占用的端口
     */
    public synchronized int allocateAvailablePort(String targetHost, int portStart, int portEnd, Set<Integer> additionalExcludedPorts) {
        cleanExpiredReservations();

        String host = StringUtils.hasText(targetHost) ? targetHost.trim() : "127.0.0.1";
        int min = Math.min(portStart, portEnd);
        int max = Math.max(portStart, portEnd);
        int rangeMin = Math.max(1024, min);
        int rangeMax = Math.min(65535, max);
        if (rangeMin >= rangeMax) {
            rangeMax = rangeMin + 1;
        }
        int total = rangeMax - rangeMin + 1;

        // 收集已知被占用的端口集合
        Set<Integer> knownOccupied = new HashSet<>();
        if (additionalExcludedPorts != null && !additionalExcludedPorts.isEmpty()) {
            knownOccupied.addAll(additionalExcludedPorts);
        }
        knownOccupied.addAll(getDbAllocatedPorts(host));
        knownOccupied.addAll(getReservedPorts(host));

        // 随机起点遍历，保证均匀分散且避免冲突
        int startOffset = RandomUtil.randomInt(0, total);
        for (int i = 0; i < total; i++) {
            int candidate = rangeMin + ((startOffset + i) % total);
            if (knownOccupied.contains(candidate)) {
                continue;
            }

            // 执行实时网络级探活检验（本地 bind 检测或远端 TCP connect 检测）
            if (isPortOccupied(host, candidate)) {
                log.debug("[PortAllocator] 端口 {}:{} 在网络层已被占用，跳过", host, candidate);
                knownOccupied.add(candidate);
                continue;
            }

            // 锁定预占
            reservePort(host, candidate);
            log.info("[PortAllocator] 为主机 {} 成功分配可用端口: {}", host, candidate);
            return candidate;
        }

        log.error("[PortAllocator] 在主机 {} 的端口范围 [{}-{}] 内未找到任何可用空闲端口", host, rangeMin, rangeMax);
        throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,
                "在主机 " + host + " 的端口范围 [" + rangeMin + ", " + rangeMax + "] 内无可用的空闲端口，请扩容端口区间或释放无用容器");
    }

    /**
     * 校验指定的主机端口是否处于可用状态（未被占用）
     */
    public void verifyPortAvailable(String targetHost, int port, Set<Integer> additionalExcludedPorts) {
        if (port < 1024 || port > 65535) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "主机端口必须在 1024 - 65535 之间，非法端口: " + port);
        }
        String host = StringUtils.hasText(targetHost) ? targetHost.trim() : "127.0.0.1";
        if (additionalExcludedPorts != null && additionalExcludedPorts.contains(port)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "主机端口 " + port + " 已被集群现有服务占用，请更换端口");
        }
        if (getDbAllocatedPorts(host).contains(port)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "主机端口 " + port + " 已被平台运行中的容器占用，请更换端口");
        }
        if (isPortOccupied(host, port)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "主机 " + host + " 的端口 " + port + " 已被系统其他进程或网络服务占用，请更换端口");
        }
        reservePort(host, port);
    }

    /**
     * 释放锁定的临时预占端口
     */
    public void releasePort(String targetHost, int port) {
        String host = StringUtils.hasText(targetHost) ? targetHost.trim() : "127.0.0.1";
        reservedPorts.remove(host + ":" + port);
        reservedPorts.remove("*:" + port);
    }

    /**
     * 检验端口是否在网络层已被占用（本地尝试 bind，远端尝试 connect）
     */
    public boolean isPortOccupied(String host, int port) {
        if (isLocalAddress(host)) {
            return isPortBoundLocally(port);
        } else {
            return isPortListeningRemotely(host, port);
        }
    }

    /**
     * 从数据库中提取当前运行中/启动中容器已占用的端口集合
     */
    public Set<Integer> getDbAllocatedPorts(String targetHost) {
        Set<Integer> ports = new HashSet<>();
        try {
            List<CtfChallengeInstance> activeList = instanceRepository.findByStatusIn(
                    List.of(InstanceStartupStatus.RUNNING, InstanceStartupStatus.STARTING, InstanceStartupStatus.DOWNLOADING_IMAGE)
            );
            for (CtfChallengeInstance inst : activeList) {
                // 如果 nodeIp 匹配或实例未指定具体节点，均视为潜在占用
                boolean nodeMatch = !StringUtils.hasText(targetHost)
                        || !StringUtils.hasText(inst.getNodeIp())
                        || targetHost.equalsIgnoreCase(inst.getNodeIp().trim())
                        || (isLocalAddress(targetHost) && isLocalAddress(inst.getNodeIp()));
                if (!nodeMatch) {
                    continue;
                }

                // 1. 从 accessUrl 中提取端口
                if (StringUtils.hasText(inst.getAccessUrl())) {
                    try {
                        URI uri = URI.create(inst.getAccessUrl().trim());
                        if (uri.getPort() > 0) {
                            ports.add(uri.getPort());
                        }
                    } catch (Exception ignored) {
                    }
                }

                // 2. 从 ports 字段正则匹配提取端口
                if (StringUtils.hasText(inst.getPorts())) {
                    Matcher matcher = PORT_PATTERN.matcher(inst.getPorts());
                    while (matcher.find()) {
                        try {
                            int p = Integer.parseInt(matcher.group(1));
                            if (p >= 1024 && p <= 65535) {
                                ports.add(p);
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[PortAllocator] 获取数据库活跃容器已分配端口失败: {}", e.getMessage());
        }
        return ports;
    }

    private void reservePort(String host, int port) {
        long expireAt = System.currentTimeMillis() + RESERVATION_TTL_MS;
        reservedPorts.put(host + ":" + port, expireAt);
        reservedPorts.put("*:" + port, expireAt);
    }

    private Set<Integer> getReservedPorts(String host) {
        Set<Integer> ports = new HashSet<>();
        cleanExpiredReservations();
        reservedPorts.forEach((k, v) -> {
            String[] parts = k.split(":");
            if (parts.length == 2) {
                String h = parts[0];
                if ("*".equals(h) || h.equalsIgnoreCase(host)) {
                    try {
                        ports.add(Integer.parseInt(parts[1]));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        });
        return ports;
    }

    private void cleanExpiredReservations() {
        long now = System.currentTimeMillis();
        reservedPorts.entrySet().removeIf(entry -> entry.getValue() < now);
    }

    private boolean isPortBoundLocally(int port) {
        try (ServerSocket ss = new ServerSocket()) {
            ss.setReuseAddress(true);
            ss.bind(new InetSocketAddress(port));
            return false; // 绑定成功说明端口未被占用
        } catch (Exception e) {
            return true; // 绑定异常说明已被其他进程监听
        }
    }

    private boolean isPortListeningRemotely(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 200);
            return true; // 连接成功说明端口处于监听状态（已被占用）
        } catch (Exception e) {
            return false; // 拒绝连接或超时说明没有服务在该端口监听
        }
    }

    public static boolean isLocalAddress(String host) {
        if (!StringUtils.hasText(host)) {
            return true;
        }
        String trimmed = host.trim();
        if ("localhost".equalsIgnoreCase(trimmed) || "127.0.0.1".equals(trimmed) || "::1".equals(trimmed) || "0.0.0.0".equals(trimmed)) {
            return true;
        }
        try {
            InetAddress addr = InetAddress.getByName(trimmed);
            if (addr.isAnyLocalAddress() || addr.isLoopbackAddress()) {
                return true;
            }
            return NetworkInterface.getByInetAddress(addr) != null;
        } catch (Exception ignored) {
            return false;
        }
    }
}
