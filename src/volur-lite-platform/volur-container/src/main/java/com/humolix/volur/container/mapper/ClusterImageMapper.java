package com.humolix.volur.container.mapper;

import com.humolix.volur.container.domain.entity.ClusterImage;
import com.humolix.volur.container.domain.record.DiscoveredDockerImage;
import com.humolix.volur.container.domain.vo.AdminClusterImageDetailVO;
import com.humolix.volur.container.domain.vo.AdminClusterImageStatisticsVO;
import com.humolix.volur.container.domain.vo.AdminClusterImageVO;
import com.humolix.volur.container.domain.vo.DockerImageLayerVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ClusterImageMapper {

    @Mapping(target = "fullImageName", expression = "java(buildFullName(image.getRepository(), image.getTag()))")
    AdminClusterImageVO toVO(ClusterImage image);

    List<AdminClusterImageVO> toVOList(List<ClusterImage> images);

    @Mapping(target = "totalImages", source = "totalImages")
    @Mapping(target = "totalSizeBytes", source = "totalSizeBytes")
    @Mapping(target = "registryOnline", source = "registryOnline")
    @Mapping(target = "registryImagesCount", source = "registryImagesCount")
    AdminClusterImageStatisticsVO toStatisticsVO(
            Long totalImages,
            Long totalSizeBytes,
            Boolean registryOnline,
            Long registryImagesCount
    );

    @Mapping(target = "id", source = "image.id")
    @Mapping(target = "repository", source = "image.repository")
    @Mapping(target = "tag", source = "image.tag")
    @Mapping(target = "imageId", source = "image.imageId")
    @Mapping(target = "sizeBytes", source = "image.sizeBytes")
    @Mapping(target = "inRegistry", source = "image.inRegistry")
    @Mapping(target = "createdAt", source = "image.createdAt")
    @Mapping(target = "updatedAt", source = "image.updatedAt")
    @Mapping(target = "fullImageName", source = "fullImageName")
    @Mapping(target = "os", source = "os")
    @Mapping(target = "architecture", source = "architecture")
    @Mapping(target = "dockerVersion", source = "dockerVersion")
    @Mapping(target = "author", source = "author")
    @Mapping(target = "workingDir", source = "workingDir")
    @Mapping(target = "user", source = "user")
    @Mapping(target = "entrypoint", source = "entrypoint")
    @Mapping(target = "cmd", source = "cmd")
    @Mapping(target = "env", source = "env")
    @Mapping(target = "exposedPorts", source = "exposedPorts")
    @Mapping(target = "labels", source = "labels")
    @Mapping(target = "layers", source = "layers")
    AdminClusterImageDetailVO toDetailVO(
            ClusterImage image,
            String fullImageName,
            String os,
            String architecture,
            String dockerVersion,
            String author,
            String workingDir,
            String user,
            List<String> entrypoint,
            List<String> cmd,
            List<String> env,
            List<String> exposedPorts,
            Map<String, String> labels,
            List<DockerImageLayerVO> layers
    );

    @Mapping(target = "layerIndex", source = "layerIndex")
    @Mapping(target = "layerId", source = "layerId")
    @Mapping(target = "instruction", source = "instruction")
    @Mapping(target = "createdBy", source = "createdBy")
    @Mapping(target = "sizeBytes", source = "sizeBytes")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "comment", source = "comment")
    DockerImageLayerVO toLayerVO(
            Integer layerIndex,
            String layerId,
            String instruction,
            String createdBy,
            Long sizeBytes,
            String createdAt,
            String comment
    );

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "repository", source = "discovered.repository")
    @Mapping(target = "tag", source = "discovered.tag")
    @Mapping(target = "imageId", source = "discovered.imageId")
    @Mapping(target = "sizeBytes", source = "discovered.sizeBytes")
    @Mapping(target = "status", constant = "PULLED")
    @Mapping(target = "inRegistry", constant = "false")
    @Mapping(target = "createdAt", expression = "java(java.time.Instant.now())")
    @Mapping(target = "updatedAt", expression = "java(java.time.Instant.now())")
    ClusterImage toEntity(DiscoveredDockerImage discovered);

    default String buildFullName(String repository, String tag) {
        if (!StringUtils.hasText(repository)) return "";
        return StringUtils.hasText(tag) ? repository + ":" + tag : repository + ":latest";
    }

}
