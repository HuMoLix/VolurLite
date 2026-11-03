package com.humolix.volur.container.util;

import com.humolix.volur.container.domain.entity.ClusterNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Slf4j
public final class DockerTlsUtil {

    private DockerTlsUtil() {
    }

    public static SSLContext createDockerSslContext(String caPem, String certPem, String keyPkcs8Pem) {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");

            // 1. TrustStore: 信任节点自签 CA
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);
            if (StringUtils.hasText(caPem)) {
                X509Certificate caCert = (X509Certificate) cf.generateCertificate(
                        new ByteArrayInputStream(caPem.getBytes(StandardCharsets.UTF_8)));
                trustStore.setCertificateEntry("docker-ca", caCert);
            }
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);

            // 2. KeyStore: 客户端双向认证公私钥
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            if (StringUtils.hasText(certPem) && StringUtils.hasText(keyPkcs8Pem)) {
                X509Certificate clientCert = (X509Certificate) cf.generateCertificate(
                        new ByteArrayInputStream(certPem.getBytes(StandardCharsets.UTF_8)));

                String cleanedKey = keyPkcs8Pem
                        .replace("-----BEGIN PRIVATE KEY-----", "")
                        .replace("-----END PRIVATE KEY-----", "")
                        .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                        .replace("-----END RSA PRIVATE KEY-----", "")
                        .replaceAll("\\s+", "");
                byte[] keyBytes = Base64.getDecoder().decode(cleanedKey);
                PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
                KeyFactory kf = KeyFactory.getInstance("RSA");
                PrivateKey clientKey = kf.generatePrivate(keySpec);

                KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                keyStore.load(null, null);
                keyStore.setKeyEntry("docker-client", clientKey, "".toCharArray(), new Certificate[]{clientCert});
                kmf.init(keyStore, "".toCharArray());
            }

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
            return sslContext;
        } catch (Exception e) {
            log.warn("[DockerTlsUtil] 构建 Docker TLS SSLContext 失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 验证节点 Docker TLS 端口 (2376) 连通性
     */
    public static boolean isDockerTlsReachable(String host, int port, String caPem, String certPem, String keyPkcs8Pem) {
        try {
            SSLContext sslContext = createDockerSslContext(caPem, certPem, keyPkcs8Pem);
            if (sslContext == null) {
                return false;
            }
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .sslContext(sslContext)
                    .build();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://" + host + ":" + port + "/_ping"))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            return resp.statusCode() == 200 && "OK".equalsIgnoreCase(resp.body().trim());
        } catch (Exception e) {
            log.debug("[DockerTlsUtil] 验证 Docker TLS 端口响应失败 ({}:{}): {}", host, port, e.getMessage());
            return false;
        }
    }

    /**
     * 为目标节点创建适配 TLS 或平坦 HTTP 的 HttpClient
     */
    public static HttpClient buildHttpClientForNode(ClusterNode node, Duration timeout) {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(timeout != null ? timeout : Duration.ofSeconds(3));

        if (node != null && Boolean.TRUE.equals(node.getTlsEnabled()) && StringUtils.hasText(node.getTlsCaCert())) {
            SSLContext sslContext = createDockerSslContext(
                    node.getTlsCaCert(),
                    node.getTlsClientCert(),
                    node.getTlsClientKey()
            );
            if (sslContext != null) {
                builder.sslContext(sslContext);
            }
        }
        return builder.build();
    }

    /**
     * 获取目标节点的 Docker HTTP / HTTPS 端点基础 URL
     */
    public static String getDockerEndpointUrl(ClusterNode node) {
        if (node == null) return "";
        boolean tls = Boolean.TRUE.equals(node.getTlsEnabled()) && StringUtils.hasText(node.getTlsCaCert());
        int port = node.getDockerPort() != null ? node.getDockerPort() : (tls ? 2376 : 2375);
        String schema = tls ? "https://" : "http://";
        return schema + node.getIpAddress().trim() + ":" + port;
    }

    /**
     * 构建包含 TLS 证书与主机端点的 Docker CLI 基础命令列表
     */
    public static List<String> buildDockerBaseCliCommand(String targetHost, ClusterNode node, String defaultPublicHost, String defaultSwarmApiUrl) {
        List<String> command = new ArrayList<>();
        command.add("docker");

        boolean isLocal = !StringUtils.hasText(targetHost) || "127.0.0.1".equalsIgnoreCase(targetHost)
                || (StringUtils.hasText(defaultPublicHost) && defaultPublicHost.equalsIgnoreCase(targetHost));

        if (StringUtils.hasText(targetHost) && (targetHost.startsWith("tcp://") || targetHost.startsWith("http://") || targetHost.startsWith("https://"))) {
            command.add("-H");
            command.add(targetHost.trim());
        } else if (!isLocal) {
            boolean tls = node != null && Boolean.TRUE.equals(node.getTlsEnabled());
            if (tls && StringUtils.hasText(node.getTlsCertPath())) {
                Path certsDir = Path.of(node.getTlsCertPath());
                Path caPath = certsDir.resolve("ca.pem");
                Path certPath = certsDir.resolve("cert.pem");
                Path keyPath = certsDir.resolve("key.pem");
                if (Files.exists(caPath) && Files.exists(certPath) && Files.exists(keyPath)) {
                    command.add("--tlsverify");
                    command.add("--tlscacert=" + caPath.toAbsolutePath());
                    command.add("--tlscert=" + certPath.toAbsolutePath());
                    command.add("--tlskey=" + keyPath.toAbsolutePath());
                }
            }
            int port = (node != null && node.getDockerPort() != null) ? node.getDockerPort() : (tls ? 2376 : 2375);
            command.add("-H");
            command.add("tcp://" + targetHost.trim() + ":" + port);
        } else {
            if (StringUtils.hasText(defaultSwarmApiUrl) && !defaultSwarmApiUrl.startsWith("unix://")) {
                command.add("-H");
                command.add(defaultSwarmApiUrl.trim());
            }
        }
        return command;
    }
}
