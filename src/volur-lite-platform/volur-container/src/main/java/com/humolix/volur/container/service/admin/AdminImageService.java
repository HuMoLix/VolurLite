package com.humolix.volur.container.service.admin;

import com.humolix.volur.container.domain.dto.AdminImageBackupDTO;
import com.humolix.volur.container.domain.dto.AdminImagePageRequestDTO;
import com.humolix.volur.container.domain.dto.AdminImagePullRequestDTO;
import com.humolix.volur.container.domain.dto.AdminImageTransferDTO;
import com.humolix.volur.container.domain.entity.ClusterNode;
import com.humolix.volur.container.domain.vo.AdminClusterImageDetailVO;
import com.humolix.volur.container.domain.vo.AdminClusterImageStatisticsVO;
import com.humolix.volur.container.domain.vo.AdminClusterImageVO;
import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.system.domain.vo.AdminAsyncTaskSubmitVO;
import jakarta.servlet.http.HttpServletResponse;

public interface AdminImageService {

    AdminClusterImageStatisticsVO getStatistics();

    PageResponse<AdminClusterImageVO> listImages(AdminImagePageRequestDTO request);

    AdminClusterImageDetailVO getImageDetail(Long id);

    AdminAsyncTaskSubmitVO pullImage(AdminImagePullRequestDTO request);

    void deleteImage(Long id, String targetNodeIp);

    void syncAllImagesToResourceNodes();

    void migrateMasterImagesToWorker(ClusterNode workerNode);

    void loadAndSyncClusterImages();

    AdminAsyncTaskSubmitVO transferImage(AdminImageTransferDTO request);

    AdminAsyncTaskSubmitVO backupImage(AdminImageBackupDTO request);

    void exportImage(Long id, HttpServletResponse response);

    AdminAsyncTaskSubmitVO importImage(org.springframework.web.multipart.MultipartFile file, String repository, String tag);

    void pushImageToRegistry(Long id);

    void ensureImageOnNode(String targetNodeIp, String fullImageName);

    void replicateImageToNode(String sourceNodeIp, String targetNodeIp, String fullImageName);

    void removeNodeFromImages(String nodeIp, boolean onlyChallenge);

    void updateNodeImageCounts();

}
