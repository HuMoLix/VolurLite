package com.humolix.volur.notification.service.admin;

import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.notification.domain.dto.AdminBroadcastPageRequestDTO;
import com.humolix.volur.notification.domain.dto.BroadcastMessageDTO;
import com.humolix.volur.notification.domain.vo.AdminBroadcastStatisticsVO;
import com.humolix.volur.notification.domain.vo.BroadcastMessageVO;

public interface AdminBroadcastService {

    PageResponse<BroadcastMessageVO> listBroadcasts(AdminBroadcastPageRequestDTO pageRequest);

    BroadcastMessageVO getBroadcastDetail(Long id);

    AdminBroadcastStatisticsVO getStatistics();

    void publishAnnouncement(BroadcastMessageDTO broadcastDTO, Long publisherId);

    void publishPromotion(BroadcastMessageDTO promotionDTO, Long publisherId);

    void deleteBroadcast(Long id);

}
