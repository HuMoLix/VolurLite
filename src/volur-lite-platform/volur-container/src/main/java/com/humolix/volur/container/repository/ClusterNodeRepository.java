package com.humolix.volur.container.repository;

import com.humolix.volur.container.domain.entity.ClusterNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClusterNodeRepository extends JpaRepository<ClusterNode, Long>, JpaSpecificationExecutor<ClusterNode> {

    Optional<ClusterNode> findByIpAddress(String ipAddress);

    Optional<ClusterNode> findByNodeName(String nodeName);

    List<ClusterNode> findByRole(String role);

    long countByRole(String role);

    List<ClusterNode> findByIsResourceNodeTrue();

    List<ClusterNode> findByStatus(String status);

    long countByStatus(String status);

    boolean existsByIpAddress(String ipAddress);

    boolean existsByRole(String role);

}
