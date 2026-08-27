package com.humolix.volur.notification.mapper;

import com.humolix.volur.notification.domain.bo.SendNotificationBO;
import com.humolix.volur.notification.domain.dto.BroadcastMessageDTO;
import com.humolix.volur.notification.domain.entity.BroadcastMessage;
import com.humolix.volur.notification.domain.entity.Notification;
import com.humolix.volur.notification.domain.vo.BroadcastMessageVO;
import com.humolix.volur.notification.domain.vo.NotificationVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface NotificationMapper {

    NotificationVO toVO(Notification entity);

    List<NotificationVO> toVOList(List<Notification> entities);

    Notification toEntity(SendNotificationBO bo);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publishTime", expression = "java(java.time.Instant.now())")
    @Mapping(source = "displayType", target = "displayType", defaultValue = "MODAL")
    @Mapping(source = "forcePopup", target = "forcePopup", defaultValue = "false")
    @Mapping(source = "priority", target = "priority", defaultValue = "0")
    BroadcastMessageVO toBroadcastVO(BroadcastMessageDTO dto);

    @Mapping(source = "publisherName", target = "publisher")
    @Mapping(source = "createdAt", target = "publishTime")
    BroadcastMessageVO toBroadcastVO(BroadcastMessage entity);

    List<BroadcastMessageVO> toBroadcastVOList(List<BroadcastMessage> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    BroadcastMessage toBroadcastEntity(BroadcastMessageDTO dto);

}
