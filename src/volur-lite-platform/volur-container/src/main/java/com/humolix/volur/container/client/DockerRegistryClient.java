package com.humolix.volur.container.client;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.humolix.volur.system.constant.SystemSettingKeys;
import com.humolix.volur.system.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DockerRegistryClient {

    private final SystemSettingService systemSettingService;

    @org.springframework.beans.factory.annotation.Value("${volur.container.registry.internal-url:${VOLUR_CONTAINER_REGISTRY_INTERNAL_URL:http://volur-registry:5000}}")
    private String registryInternalUrl;

    @org.springframework.beans.factory.annotation.Value("${volur.container.registry.public-port:${VOLUR_CONTAINER_REGISTRY_PUBLIC_PORT:5000}}")
    private int registryPublicPort;

    @org.springframework.beans.factory.annotation.Value("${volur.container.registry.enabled:${VOLUR_CONTAINER_REGISTRY_ENABLED:true}}")
    private boolean registryEnabled;

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(6);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(DEFAULT_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public record RegistryManifestInfo(
            String digest,
            String mediaType,
            long sizeBytes,
            int schemaVersion
    ) {}

    private volatile String cachedActiveRegistryUrl = null;

    private boolean checkRegistryUrl(String url) {
        if (!StringUtils.hasText(url)) return false;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url.trim() + "/v2/"))
                    .timeout(DEFAULT_TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 || response.statusCode() == 401;
        } catch (Exception e) {
            return false;
        }
    }

    public String resolveInternalRegistryUrl() {
        if (StringUtils.hasText(cachedActiveRegistryUrl) && checkRegistryUrl(cachedActiveRegistryUrl)) {
            return cachedActiveRegistryUrl;
        }

        List<String> candidates = new ArrayList<>();
        String configured = StringUtils.hasText(registryInternalUrl) ? registryInternalUrl.trim() : "http://volur-registry:5000";
        if (configured.contains("volur-registry")) {
            try {
                InetAddress.getByName("volur-registry");
                candidates.add(configured);
            } catch (Exception ignored) {}
        } else {
            candidates.add(configured);
        }

        String publicHost = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_PUBLIC_HOST, "127.0.0.1");
        if (StringUtils.hasText(publicHost) && !"127.0.0.1".equalsIgnoreCase(publicHost)) {
            candidates.add("http://" + publicHost.trim() + ":" + registryPublicPort);
        }

        candidates.add("http://127.0.0.1:" + registryPublicPort);

        for (String c : candidates) {
            if (checkRegistryUrl(c)) {
                cachedActiveRegistryUrl = c;
                return c;
            }
        }

        return candidates.get(0);
    }

    public String getPublicRegistryHost() {
        String publicHost = systemSettingService.getSettingValue(SystemSettingKeys.CONTAINER_PUBLIC_HOST, "127.0.0.1");
        return publicHost + ":" + registryPublicPort;
    }

    public boolean isRegistryEnabled() {
        return registryEnabled;
    }

    public boolean isAvailable() {
        if (!isRegistryEnabled()) {
            return false;
        }
        String resolved = resolveInternalRegistryUrl();
        return checkRegistryUrl(resolved);
    }

    /**
     * 列出 Registry 中所有的镜像仓库 (Catalog)
     */
    public List<String> listRepositories() {
        if (!isAvailable()) {
            return Collections.emptyList();
        }

        String baseUrl = resolveInternalRegistryUrl();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/v2/_catalog?n=1000"))
                    .timeout(DEFAULT_TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && StringUtils.hasText(response.body())) {
                JSONObject json = JSONUtil.parseObj(response.body());
                JSONArray repos = json.getJSONArray("repositories");
                if (repos != null) {
                    return repos.toList(String.class);
                }
            }
        } catch (Exception e) {
            log.warn("[DockerRegistryClient] 获取镜像 Catalog 列表异常: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    /**
     * 列出指定镜像仓库的所有 Tag 标签
     */
    public List<String> listTags(String repository) {
        if (!StringUtils.hasText(repository) || !isAvailable()) {
            return Collections.emptyList();
        }

        String baseUrl = resolveInternalRegistryUrl();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/v2/" + repository.trim() + "/tags/list"))
                    .timeout(DEFAULT_TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && StringUtils.hasText(response.body())) {
                JSONObject json = JSONUtil.parseObj(response.body());
                JSONArray tags = json.getJSONArray("tags");
                if (tags != null) {
                    return tags.toList(String.class);
                }
            }
        } catch (Exception e) {
            log.warn("[DockerRegistryClient] 获取镜像 [{}] Tags 异常: {}", repository, e.getMessage());
        }
        return Collections.emptyList();
    }

    /**
     * 获取指定镜像的 Manifest 元数据与体积
     */
    public RegistryManifestInfo getManifest(String repository, String tag) {
        if (!StringUtils.hasText(repository) || !isAvailable()) {
            return null;
        }
        String actualTag = StringUtils.hasText(tag) ? tag.trim() : "latest";
        String baseUrl = resolveInternalRegistryUrl();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/v2/" + repository.trim() + "/manifests/" + actualTag))
                    .header("Accept", "application/vnd.docker.distribution.manifest.v2+json, application/vnd.oci.image.manifest.v1+json")
                    .timeout(DEFAULT_TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String digest = response.headers().firstValue("Docker-Content-Digest").orElse("");
                String mediaType = response.headers().firstValue("Content-Type").orElse("");
                long totalSize = 0L;
                int schemaVersion = 2;

                if (StringUtils.hasText(response.body())) {
                    JSONObject bodyJson = JSONUtil.parseObj(response.body());
                    schemaVersion = bodyJson.getInt("schemaVersion", 2);

                    JSONObject configObj = bodyJson.getJSONObject("config");
                    if (configObj != null && configObj.containsKey("size")) {
                        totalSize += configObj.getLong("size", 0L);
                    }

                    JSONArray layers = bodyJson.getJSONArray("layers");
                    if (layers != null) {
                        for (int i = 0; i < layers.size(); i++) {
                            JSONObject layer = layers.getJSONObject(i);
                            if (layer != null && layer.containsKey("size")) {
                                totalSize += layer.getLong("size", 0L);
                            }
                        }
                    }
                }

                return new RegistryManifestInfo(digest, mediaType, totalSize, schemaVersion);
            }
        } catch (Exception e) {
            log.warn("[DockerRegistryClient] 获取镜像 [{}:{}] Manifest 失败: {}", repository, actualTag, e.getMessage());
        }
        return null;
    }

    /**
     * 从 Registry 中物理删除指定镜像 Manifest
     */
    public boolean deleteManifest(String repository, String digest) {
        if (!StringUtils.hasText(repository) || !StringUtils.hasText(digest) || !isAvailable()) {
            return false;
        }

        String baseUrl = resolveInternalRegistryUrl();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/v2/" + repository.trim() + "/manifests/" + digest.trim()))
                    .header("Accept", "application/vnd.docker.distribution.manifest.v2+json, application/vnd.oci.image.manifest.v1+json")
                    .timeout(DEFAULT_TIMEOUT)
                    .DELETE()
                    .build();

            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            boolean success = response.statusCode() == 202 || response.statusCode() == 200;
            if (success) {
                log.info("[DockerRegistryClient] 成功从 Registry 中删除镜像 Manifest: {} ({})", repository, digest);
            }
            return success;
        } catch (Exception e) {
            log.warn("[DockerRegistryClient] 删除镜像 Manifest [{}] 失败: {}", digest, e.getMessage());
            return false;
        }
    }

    /**
     * 根据 Tag 标签查询 Digest 并从 Registry 中物理删除指定镜像 Manifest
     */
    public boolean deleteManifestByTag(String repository, String tag) {
        if (!StringUtils.hasText(repository) || !isAvailable()) {
            return false;
        }
        RegistryManifestInfo info = getManifest(repository, tag);
        if (info != null && StringUtils.hasText(info.digest())) {
            return deleteManifest(repository, info.digest());
        }
        return false;
    }

}
