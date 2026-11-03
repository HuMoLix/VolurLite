package com.humolix.volur.container.repository;

import com.humolix.volur.container.domain.entity.ClusterImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClusterImageRepository extends JpaRepository<ClusterImage, Long>, JpaSpecificationExecutor<ClusterImage> {

    Optional<ClusterImage> findByRepositoryAndTag(String repository, String tag);

    long countByInRegistry(Boolean inRegistry);

    long countByInRegistryTrue();

    List<ClusterImage> findByInRegistryTrue();

}
