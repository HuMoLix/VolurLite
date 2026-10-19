package com.humolix.volur.container.mapper;

import com.humolix.volur.container.domain.entity.CtfChallengeInstance;
import com.humolix.volur.container.domain.vo.AdminContainerInstanceVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ContainerMapper {

    @Mapping(source = "challenge.id", target = "challengeId")
    @Mapping(target = "challengeTitle", expression = "java(resolveChallengeTitle(instance))")
    @Mapping(target = "challengeCategory", expression = "java(resolveChallengeCategory(instance))")
    @Mapping(source = "targetId", target = "userId")
    @Mapping(target = "remainingSeconds", expression = "java(calculateRemainingSeconds(instance.getExpiresAt()))")
    @Mapping(target = "containerType", expression = "java(determineContainerType(instance))")
    @Mapping(target = "username", ignore = true)
    AdminContainerInstanceVO toAdminInstanceVO(CtfChallengeInstance instance);

    List<AdminContainerInstanceVO> toAdminInstanceVOList(List<CtfChallengeInstance> instances);

    default String resolveChallengeTitle(CtfChallengeInstance instance) {
        if (instance == null) {
            return null;
        }
        if (instance.getChallenge() != null && instance.getChallenge().getName() != null) {
            return instance.getChallenge().getName();
        }
        if (instance.getContainerName() != null && !instance.getContainerName().isBlank()) {
            return instance.getContainerName();
        }
        if (instance.getImageName() != null && !instance.getImageName().isBlank()) {
            return instance.getImageName();
        }
        return "独立容器";
    }

    default String resolveChallengeCategory(CtfChallengeInstance instance) {
        if (instance == null) {
            return null;
        }
        if (instance.getChallenge() != null && instance.getChallenge().getCategory() != null) {
            return instance.getChallenge().getCategory().getName();
        }
        return "独立镜像";
    }

    default Integer calculateRemainingSeconds(Instant expiresAt) {
        if (expiresAt == null) {
            return null;
        }
        long secs = Duration.between(Instant.now(), expiresAt).getSeconds();
        return (int) Math.max(0, secs);
    }

    default String determineContainerType(CtfChallengeInstance instance) {
        if (instance == null) {
            return "DOCKER_SWARM";
        }
        if (instance.getContainerType() != null && !instance.getContainerType().isBlank()) {
            return instance.getContainerType();
        }
        if (instance.getContainerId() != null && instance.getContainerId().startsWith("swarm-")) {
            return "DOCKER_SWARM";
        }
        if (instance.getContainerId() != null && (instance.getContainerId().startsWith("pod-") || instance.getContainerId().startsWith("mock-"))) {
            return "K3S";
        }
        return "DOCKER_SWARM";
    }

}
