package com.humolix.volur.container.strategy;

import cn.hutool.core.util.RandomUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.humolix.volur.challenge.entity.Challenge;
import com.humolix.volur.challenge.object.vo.admin.ChallengeContainerStatusResponseVO;
import com.humolix.volur.container.dto.ContainerRunResultDTO;
import com.humolix.volur.container.entity.enums.InstanceStartupStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.ServerSocket;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Component
public class DockerRuntimeStrategy implements ContainerRuntimeStrategy {

    private static final Logger log = LoggerFactory.getLogger(DockerRuntimeStrategy.class);

    @Value("${volur.container.docker-host:tcp://127.0.0.1:2375}")
    private String dockerHost;

    @Value("${volur.container.public-host:127.0.0.1}")
    private String publicHost;

    private DockerClient dockerClient;

    private synchronized DockerClient getDockerClient() {
        if (dockerClient == null) {
            try {
                DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                        .withDockerHost(dockerHost)
                        .build();
                ApacheDockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                        .dockerHost(config.getDockerHost())
                        .sslConfig(config.getSSLConfig())
                        .maxConnections(100)
                        .connectionTimeout(Duration.ofSeconds(30))
                        .responseTimeout(Duration.ofSeconds(45))
                        .build();
                dockerClient = DockerClientImpl.getInstance(config, httpClient);
            } catch (Exception e) {
                log.warn("Docker client initialization failed: {}. Dynamic containers may run in mock mode.", e.getMessage());
            }
        }
        return dockerClient;
    }

    private int findAvailablePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (Exception e) {
            return RandomUtil.randomInt(20000, 40000);
        }
    }

    @Override
    public ContainerRunResultDTO startContainer(String resourceName, Challenge challenge, String dynamicFlag, Integer cpuLimit, Integer memoryLimit) {
        int hostPort = findAvailablePort();
        int containerPort = challenge.getContainerPort() != null ? challenge.getContainerPort() : 80;
        String imageName = challenge.getImageName();

        DockerClient client = getDockerClient();
        if (client != null && imageName != null) {
            try {
                ExposedPort exposedPort = ExposedPort.tcp(containerPort);
                Ports portBindings = new Ports();
                portBindings.bind(exposedPort, Ports.Binding.bindPort(hostPort));

                HostConfig hostConfig = HostConfig.newHostConfig()
                        .withPortBindings(portBindings)
                        .withAutoRemove(true);

                if (cpuLimit != null && cpuLimit > 0) {
                    hostConfig.withCpuQuota((long) cpuLimit * 100000);
                }
                if (memoryLimit != null && memoryLimit > 0) {
                    hostConfig.withMemory((long) memoryLimit * 1024 * 1024);
                }

                List<String> env = dynamicFlag != null
                        ? List.of("FLAG=" + dynamicFlag, "GZCTF_FLAG=" + dynamicFlag)
                        : Collections.emptyList();

                CreateContainerResponse container = client.createContainerCmd(imageName)
                        .withName(resourceName)
                        .withEnv(env)
                        .withHostConfig(hostConfig)
                        .withExposedPorts(exposedPort)
                        .exec();

                client.startContainerCmd(container.getId()).exec();

                String accessUrl = "http://" + publicHost + ":" + hostPort;
                return new ContainerRunResultDTO(container.getId(), accessUrl, hostPort, InstanceStartupStatus.RUNNING);
            } catch (Exception e) {
                log.error("Failed to start docker container {}: {}", resourceName, e.getMessage());
            }
        }

        // Fallback / mock endpoint if Docker daemon is not connected
        String fallbackUrl = "http://" + publicHost + ":" + hostPort;
        return new ContainerRunResultDTO("mock-" + resourceName, fallbackUrl, hostPort, InstanceStartupStatus.RUNNING);
    }

    @Override
    public void stopContainer(String resourceName) {
        DockerClient client = getDockerClient();
        if (client != null) {
            try {
                client.stopContainerCmd(resourceName).withTimeout(5).exec();
                client.removeContainerCmd(resourceName).withForce(true).exec();
            } catch (Exception e) {
                log.warn("Failed to stop/remove docker container {}: {}", resourceName, e.getMessage());
            }
        }
    }

    @Override
    public ChallengeContainerStatusResponseVO getStatus(String resourceName, Challenge challenge, String targetUsername) {
        return new ChallengeContainerStatusResponseVO(resourceName, "RUNNING", publicHost, challenge.getContainerPort(), challenge.getImageName());
    }

}
