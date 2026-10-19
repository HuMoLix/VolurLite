package com.humolix.volur.competition.mapper;

import com.humolix.volur.competition.domain.entity.CompetitionOperationLog;
import com.humolix.volur.competition.domain.vo.AdminCompetitionLogVO;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CompetitionOperationLogMapper {

    AdminCompetitionLogVO toVO(CompetitionOperationLog entity);

    List<AdminCompetitionLogVO> toVOList(List<CompetitionOperationLog> entities);

}
