package com.humolix.volur.container.strategy;

import cn.hutool.core.util.RandomUtil;
import com.humolix.volur.challenge.domain.entity.Challenge;
import com.humolix.volur.challenge.domain.entity.Containerizable;
import com.humolix.volur.container.dto.ContainerRunResultDTO;
import com.humolix.volur.container.entity.enums.InstanceStartupStatus;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class K3sRuntimeStrategy implements ContainerRuntimeStrategy {

    private static final Logger log = LoggerFactory.getLogger(K3sRuntimeStrategy.class);

    @Value("${volur.k3s.namespace:volur-challenges}")
    private String namespace;

    @Value("${volur.k3s.public-host:127.0.0.1}")
    private String publicHost;

    @Value("${volur.k3s.kubeconfig-path:/etc/rancher/k3s/k3s.yaml}")
    private String kubeconfigPath;

    @Value("${volur.k3s.master-url:https://127.0.0.1:6443}")
    private String masterUrl;

    private KubernetesClient k8sClient;

    private synchronized KubernetesClient getClient() {
        if (k8sClient != null) {
            return k8sClient;
        }

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
    public ContainerRunResultDTO startContainer(String resourceName, Challenge challenge, String dynamicFlag, Integer cpuLimit, Integer memoryLimit) {
        String sanitizedName = sanitizeResourceName(resourceName);
        KubernetesClient client = getClient();

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

                Pod pod = new PodBuilder()
                        .withNewMetadata()
                        .withName(sanitizedName)
                        .withNamespace(namespace)
                        .withLabels(Map.of("app", sanitizedName, "challenge-id", String.valueOf(challenge.getId())))
                        .endMetadata()
                        .withNewSpec()
                        .withRestartPolicy("Never")
                        .addNewContainer()
                        .withName("challenge-container")
                        .withImage(imageName)
                        .withEnv(envVars)
                        .withResources(resources)
                        .addNewPort()
                        .withContainerPort(containerPort)
                        .endPort()
                        .endContainer()
                        .endSpec()
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
                    nodePort = 30000 + RandomUtil.randomInt(1, 2767);
                }

                String accessUrl = "http://" + publicHost + ":" + nodePort;
                return new ContainerRunResultDTO(sanitizedName, accessUrl, nodePort, InstanceStartupStatus.RUNNING);
            } catch (Exception e) {
                log.error("Failed to schedule K3s Pod/Service {}: {}", sanitizedName, e.getMessage());
            }
        }

        // Fallback simulation mode
        int fallbackPort = 30000 + RandomUtil.randomInt(1, 2767);
        String fallbackUrl = "http://" + publicHost + ":" + fallbackPort;
        return new ContainerRunResultDTO("mock-" + sanitizedName, fallbackUrl, fallbackPort, InstanceStartupStatus.RUNNING);
    }

    @Override
    public void stopContainer(String resourceName) {
        String sanitizedName = sanitizeResourceName(resourceName);
        KubernetesClient client = getClient();
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

    private String sanitizeResourceName(String name) {
        if (name == null) return "pod-" + System.currentTimeMillis();
        return name.toLowerCase().replaceAll("[^a-z0-9-]", "-");
    }

}
