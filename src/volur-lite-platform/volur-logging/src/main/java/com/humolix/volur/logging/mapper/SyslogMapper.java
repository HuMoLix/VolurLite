package com.humolix.volur.logging.mapper;

import com.humolix.volur.logging.domain.entity.Syslog;
import com.humolix.volur.logging.domain.vo.AdminLogVO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SyslogMapper {

    AdminLogVO toVO(Syslog syslog);

}
