package com.humolix.volur.container.domain.entity;

import com.humolix.volur.framework.id.annotation.SnowflakeGenerated;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.Instant;

/**
 * 集群节点持久化实体
 * 支持 Docker Swarm 节点与 K3s Worker / Master 节点
 */
@Entity
@Table(name = "cluster_node", indexes = {
        @Index(name = "idx_node_ip", columnList = "ip_address"),
        @Index(name = "idx_node_role", columnList = "role")
})
@DynamicUpdate
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ClusterNode implements Serializable {

    @Id
    @SnowflakeGenerated
    @EqualsAndHashCode.Include
    @Column(name = "id", nullable = false, updatable = false, unique = true, columnDefinition = "BIGINT")
    private Long id;

    @Column(name = "node_name", nullable = false, length = 128)
    private String nodeName;

    @Column(name = "ip_address", nullable = false, length = 64)
    private String ipAddress;

    @Builder.Default
    @Column(name = "ssh_port", columnDefinition = "INT DEFAULT 22")
    private Integer sshPort = 22;

    @Builder.Default
    @Column(name = "ssh_user", length = 64)
    private String sshUser = "root";

    @Column(name = "ssh_password", length = 255)
    private String sshPassword;

    @Column(name = "ssh_key", columnDefinition = "TEXT")
    private String sshKey;

    @Column(name = "cluster_type", nullable = false, length = 32)
    private String clusterType; // DOCKER_SWARM / K3S

    @Column(name = "role", nullable = false, length = 32)
    private String role; // MASTER / WORKER

    @Builder.Default
    @Column(name = "is_resource_node", nullable = false)
    private Boolean isResourceNode = true; // 是否作为题目录入与运行的资源节点

    @Builder.Default
    @Column(name = "status", nullable = false, length = 32)
    private String status = "READY"; // READY / JOINING / OFFLINE / ERROR

    @Column(name = "os_info", length = 128)
    private String osInfo;

    @Column(name = "docker_version", length = 64)
    private String dockerVersion;

    @Builder.Default
    @Column(name = "cpu_cores", columnDefinition = "INT DEFAULT 1")
    private Integer cpuCores = 1;

    @Builder.Default
    @Column(name = "memory_bytes", columnDefinition = "BIGINT DEFAULT 1073741824")
    private Long memoryBytes = 1073741824L;

    @Builder.Default
    @Column(name = "docker_port", columnDefinition = "INT DEFAULT 2376")
    private Integer dockerPort = 2376;

    @Builder.Default
    @Column(name = "tls_enabled", nullable = false)
    private Boolean tlsEnabled = true;

    @Column(name = "tls_cert_path", length = 255)
    private String tlsCertPath;

    @Column(name = "tls_ca_cert", columnDefinition = "TEXT")
    private String tlsCaCert;

    @Column(name = "tls_client_cert", columnDefinition = "TEXT")
    private String tlsClientCert;

    @Column(name = "tls_client_key", columnDefinition = "TEXT")
    private String tlsClientKey;

    @Builder.Default
    @Column(name = "container_count", columnDefinition = "INT DEFAULT 0")
    private Integer containerCount = 0;

    @Builder.Default
    @Column(name = "image_count", columnDefinition = "INT DEFAULT 0")
    private Integer imageCount = 0;

    @Builder.Default
    @Column(name = "in_cluster", nullable = false)
    private Boolean inCluster = false; // 是否已成功加入并处于集群状态

    @Column(name = "cluster_node_id", length = 64)
    private String clusterNodeId; // 底层编排集群分配的唯一节点 ID (如 Swarm Node ID)

    @CreationTimestamp
    @Column(name = "join_time", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Instant joinTime;

    @UpdateTimestamp
    @Column(name = "last_heartbeat", columnDefinition = "TIMESTAMP")
    private Instant lastHeartbeat;

}
