package com.humolix.volur.container.strategy;

import com.humolix.volur.challenge.domain.entity.Challenge;
import com.humolix.volur.container.domain.dto.AdminContainerExecDTO;
import com.humolix.volur.container.domain.dto.AdminContainerManualStartDTO;
import com.humolix.volur.container.domain.dto.AdminContainerTestConnectionDTO;
import com.humolix.volur.container.domain.dto.ContainerRunResultDTO;
import com.humolix.volur.container.domain.enums.InstanceStartupStatus;
import com.humolix.volur.container.domain.vo.AdminContainerExecResultVO;
import com.humolix.volur.container.domain.vo.AdminContainerTerminalInfoVO;
import com.humolix.volur.container.domain.vo.AdminContainerTestConnectionVO;

public interface ContainerRuntimeStrategy {

    default ContainerRunResultDTO startContainer(String resourceName, Challenge challenge, String dynamicFlag, Integer cpuLimit, Integer memoryLimit) {
        return startContainer(resourceName, challenge, dynamicFlag, cpuLimit, memoryLimit, null);
    }

    ContainerRunResultDTO startContainer(String resourceName, Challenge challenge, String dynamicFlag, Integer cpuLimit, Integer memoryLimit, String targetNodeIp);

    ContainerRunResultDTO startManualContainer(AdminContainerManualStartDTO request, String resourceName, Challenge challenge, String dynamicFlag);

    void stopContainer(String resourceName);

    AdminContainerTestConnectionVO testConnection(AdminContainerTestConnectionDTO request);

    AdminContainerExecResultVO execCommand(String containerId, AdminContainerExecDTO request, String fallbackFlag);

    InstanceStartupStatus checkInstanceStatus(String containerId);

    AdminContainerTerminalInfoVO getTerminalInfo(String containerId, String fallbackFlag);

}
