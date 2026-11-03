package com.humolix.volur.container.service.admin;

import com.humolix.volur.container.domain.dto.*;
import com.humolix.volur.container.domain.vo.AdminContainerExecResultVO;
import com.humolix.volur.container.domain.vo.AdminContainerInstanceVO;
import com.humolix.volur.container.domain.vo.AdminContainerStatisticsVO;
import com.humolix.volur.container.domain.vo.AdminContainerTerminalInfoVO;
import com.humolix.volur.framework.common.api.PageResponse;

public interface AdminContainerService {

    PageResponse<AdminContainerInstanceVO> listInstances(AdminContainerPageRequestDTO request);

    AdminContainerStatisticsVO getStatistics();

    void stopInstance(Long instanceId);

    void batchStop(AdminContainerBatchStopRequestDTO request);

    void renewInstance(Long instanceId, AdminContainerRenewRequestDTO request);

    AdminContainerInstanceVO manualStartInstance(AdminContainerManualStartDTO request);

    AdminContainerExecResultVO execContainer(Long instanceId, AdminContainerExecDTO request);

    AdminContainerTerminalInfoVO getTerminalInfo(Long instanceId);

    java.util.List<com.humolix.volur.container.domain.vo.AdminClusterNodeVO> getCandidateNodes();

}
