package com.humolix.volur.system.mapper;

import com.humolix.volur.system.domain.entity.AsyncTaskEntity;
import com.humolix.volur.system.domain.vo.AdminAsyncTaskVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AsyncTaskMapper {

    @Mapping(target = "taskTypeDesc", expression = "java(entity.getTaskType() != null ? entity.getTaskType().getDescription() : null)")
    @Mapping(target = "statusDesc", expression = "java(entity.getStatus() != null ? entity.getStatus().getDescription() : null)")
    AdminAsyncTaskVO toVO(AsyncTaskEntity entity);

    List<AdminAsyncTaskVO> toVOList(List<AsyncTaskEntity> entities);

}
