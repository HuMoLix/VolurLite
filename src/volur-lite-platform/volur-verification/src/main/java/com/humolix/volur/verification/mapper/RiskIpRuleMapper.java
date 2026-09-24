package com.humolix.volur.verification.mapper;

import com.humolix.volur.verification.domain.entity.RiskIpRule;
import com.humolix.volur.verification.domain.vo.AdminIpRuleVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RiskIpRuleMapper {

    @Mapping(target = "isEffective", expression = "java(entity.isEffective())")
    AdminIpRuleVO toVO(RiskIpRule entity);

    List<AdminIpRuleVO> toVOList(List<RiskIpRule> entityList);

}
