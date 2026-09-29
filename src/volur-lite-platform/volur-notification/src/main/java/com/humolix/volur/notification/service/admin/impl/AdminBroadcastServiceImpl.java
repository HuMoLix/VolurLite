package com.humolix.volur.notification.service.admin.impl;

import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.framework.common.util.SortUtils;
import com.humolix.volur.notification.domain.dto.AdminBroadcastPageRequestDTO;
import com.humolix.volur.notification.domain.dto.BroadcastMessageDTO;
import com.humolix.volur.notification.domain.entity.BroadcastMessage;
import com.humolix.volur.notification.domain.enums.RealtimeEventType;
import com.humolix.volur.notification.domain.vo.AdminBroadcastStatisticsVO;
import com.humolix.volur.notification.domain.vo.BroadcastMessageVO;
import com.humolix.volur.notification.mapper.NotificationMapper;
import com.humolix.volur.notification.repository.BroadcastMessageRepository;
import com.humolix.volur.notification.service.RealtimePushService;
import com.humolix.volur.notification.service.admin.AdminBroadcastService;
import com.humolix.volur.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminBroadcastServiceImpl implements AdminBroadcastService {

    private final BroadcastMessageRepository broadcastMessageRepository;
    private final UserRepository userRepository;
    private final NotificationMapper notificationMapper;
    private final RealtimePushService realtimePushService;

    @Override
    public PageResponse<BroadcastMessageVO> listBroadcasts(AdminBroadcastPageRequestDTO pageRequest) {
        Pageable pageable = SortUtils.createPageable(
                pageRequest.getPage(),
                pageRequest.getSize(),
                BroadcastMessage.class,
                pageRequest.getSortBy(),
                pageRequest.getSortOrder(),
                "createdAt",
                Sort.Direction.DESC
        );

        Specification<BroadcastMessage> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(pageRequest.getKeyword())) {
                String keywordLike = "%" + pageRequest.getKeyword().trim() + "%";
                Predicate titleLike = cb.like(root.get("title"), keywordLike);
                Predicate contentLike = cb.like(root.get("content"), keywordLike);
                predicates.add(cb.or(titleLike, contentLike));
            }

            if (pageRequest.getDisplayType() != null) {
                predicates.add(cb.equal(root.get("displayType"), pageRequest.getDisplayType()));
            }

            if (StringUtils.hasText(pageRequest.getCategory())) {
                predicates.add(cb.equal(root.get("category"), pageRequest.getCategory().trim()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<BroadcastMessage> entityPage = broadcastMessageRepository.findAll(spec, pageable);
        return PageResponse.of(entityPage.map(notificationMapper::toBroadcastVO));
    }

    @Override
    public BroadcastMessageVO getBroadcastDetail(Long id) {
        if (id == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "广播ID不能为空");
        }

        BroadcastMessage entity = broadcastMessageRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "公告通知不存在或已删除"));

        return notificationMapper.toBroadcastVO(entity);
    }

    @Override
    public AdminBroadcastStatisticsVO getStatistics() {
        long totalAnnouncements = broadcastMessageRepository.countByCategory("ANNOUNCEMENT");
        long totalPromotions = broadcastMessageRepository.countByCategory("PROMOTION");
        long urgentModals = broadcastMessageRepository.countByForcePopupTrue();

        return AdminBroadcastStatisticsVO.builder()
                .totalAnnouncements(totalAnnouncements)
                .totalPromotions(totalPromotions)
                .urgentModals(urgentModals)
                .sseOnlineStatus(true)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishAnnouncement(BroadcastMessageDTO broadcastDTO, Long publisherId) {
        if (broadcastDTO == null) {
            return;
        }

        BroadcastMessage entity = notificationMapper.toBroadcastEntity(broadcastDTO);
        entity.setCategory("ANNOUNCEMENT");
        fillPublisherInfo(entity, publisherId);

        BroadcastMessage saved = broadcastMessageRepository.save(entity);
        BroadcastMessageVO vo = notificationMapper.toBroadcastVO(saved);

        realtimePushService.broadcastAll(RealtimeEventType.ANNOUNCEMENT.getValue(), vo);
        log.info("[AdminBroadcast] 发布全站系统公告成功 -> id: {}, title: {}, publisher: {}",
                saved.getId(), saved.getTitle(), saved.getPublisherName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishPromotion(BroadcastMessageDTO promotionDTO, Long publisherId) {
        if (promotionDTO == null) {
            return;
        }

        BroadcastMessage entity = notificationMapper.toBroadcastEntity(promotionDTO);
        entity.setCategory("PROMOTION");
        fillPublisherInfo(entity, publisherId);

        BroadcastMessage saved = broadcastMessageRepository.save(entity);
        BroadcastMessageVO vo = notificationMapper.toBroadcastVO(saved);

        realtimePushService.broadcastAll(RealtimeEventType.PROMOTION.getValue(), vo);
        log.info("[AdminBroadcast] 发布运营活动/广告弹窗成功 -> id: {}, title: {}, publisher: {}",
                saved.getId(), saved.getTitle(), saved.getPublisherName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBroadcast(Long id) {
        if (id == null) {
            return;
        }

        BroadcastMessage entity = broadcastMessageRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "广播通知不存在或已删除"));

        broadcastMessageRepository.delete(entity);
        log.info("[AdminBroadcast] 删除广播通知成功 -> id: {}, title: {}", id, entity.getTitle());
    }

    private void fillPublisherInfo(BroadcastMessage entity, Long publisherId) {
        if (publisherId != null) {
            entity.setPublisherId(publisherId);
            userRepository.findById(publisherId)
                    .ifPresentOrElse(
                            user -> entity.setPublisherName(user.getUsername()),
                            () -> entity.setPublisherName("系统管理员")
                    );
        } else {
            entity.setPublisherName("系统管理员");
        }
    }

}
