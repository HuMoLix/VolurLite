package com.humolix.volur.verification.mapper;

import com.humolix.volur.verification.domain.entity.EmailLog;
import com.humolix.volur.verification.domain.vo.EmailLogVO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EmailLogMapper {

    EmailLogVO toVO(EmailLog entity);

    List<EmailLogVO> toVOList(List<EmailLog> entityList);

}
