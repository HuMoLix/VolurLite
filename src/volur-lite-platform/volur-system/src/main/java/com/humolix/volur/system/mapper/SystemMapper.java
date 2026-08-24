package com.humolix.volur.system.mapper;

import com.humolix.volur.system.domain.entity.SystemSettingEntity;
import com.humolix.volur.system.domain.vo.SystemSettingVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SystemMapper {

    @Mapping(source = "settingGroup", target = "group")
    @Mapping(source = "settingKey", target = "key")
    @Mapping(source = "settingValue", target = "value")
    SystemSettingVO toSettingVO(SystemSettingEntity entity);

    List<SystemSettingVO> toSettingVOList(List<SystemSettingEntity> entities);

}
