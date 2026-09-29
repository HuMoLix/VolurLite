package com.humolix.volur.container.service.admin;

import com.humolix.volur.container.domain.dto.*;
import com.humolix.volur.container.domain.vo.AdminClusterNodeVO;
import com.humolix.volur.container.domain.vo.AdminClusterSettingsVO;
import com.humolix.volur.container.domain.vo.AdminContainerTestConnectionVO;

import java.util.List;

public interface AdminClusterService {

    AdminClusterSettingsVO getSettings();

    void updateSettings(AdminClusterSettingsDTO request);

    AdminContainerTestConnectionVO testConnection(AdminContainerTestConnectionDTO request);

    List<AdminClusterNodeVO> listNodes();

    AdminClusterNodeVO addNode(AdminClusterNodeCreateDTO request);

    AdminClusterNodeVO updateNode(Long id, AdminClusterNodeUpdateDTO request);

    AdminContainerTestConnectionVO testNodeConnection(Long id, AdminClusterNodeUpdateDTO request);

    void updateNodeRole(Long id, AdminClusterNodeRoleUpdateDTO request);

    void removeNode(Long id);

    void syncImagesToNode(Long id);

    AdminClusterNodeVO checkNodeHealth(Long id);

    List<AdminClusterNodeVO> checkAllNodesHealth();

    AdminClusterNodeVO joinCluster(Long id);
 
    List<AdminClusterNodeVO> syncK3sClusterNodes();
}
