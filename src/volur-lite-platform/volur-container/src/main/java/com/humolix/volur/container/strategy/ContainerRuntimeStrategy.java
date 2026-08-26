package com.humolix.volur.container.strategy;

import com.humolix.volur.challenge.domain.entity.Challenge;
import com.humolix.volur.container.dto.ContainerRunResultDTO;

public interface ContainerRuntimeStrategy {

    ContainerRunResultDTO startContainer(String resourceName, Challenge challenge, String dynamicFlag, Integer cpuLimit, Integer memoryLimit);

    void stopContainer(String resourceName);

}
