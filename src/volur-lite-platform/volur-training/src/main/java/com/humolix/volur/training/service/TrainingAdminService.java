package com.humolix.volur.training.service;

import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.training.domain.dto.TrainingChallengeUpdateDTO;
import com.humolix.volur.training.domain.dto.TrainingImportRequestDTO;
import com.humolix.volur.training.domain.vo.TrainingChallengeListVO;
import com.humolix.volur.training.domain.vo.TrainingImportResultVO;

/**
 * 训练池管理端服务：批量导入、列表、改分值/上下架、移除。
 */
public interface TrainingAdminService {

    PageResponse<TrainingChallengeListVO> listChallenges(int page, int size, String keyword, String status);

    TrainingImportResultVO importChallenges(TrainingImportRequestDTO request, Long adminId);

    void updateChallenge(Long id, TrainingChallengeUpdateDTO request);

    void deleteChallenge(Long id);

}
