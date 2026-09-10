package com.humolix.volur.container.mapper;

import com.humolix.volur.container.domain.entity.ClusterNode;
import com.humolix.volur.container.domain.vo.AdminClusterNodeVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ClusterNodeMapper {

    @Mapping(target = "memoryFormatted", expression = "java(formatBytes(node.getMemoryBytes()))")
    @Mapping(target = "hasSshAuth", expression = "java(org.springframework.util.StringUtils.hasText(node.getSshPassword()) || org.springframework.util.StringUtils.hasText(node.getSshKey()))")
    @Mapping(target = "authType", expression = "java(org.springframework.util.StringUtils.hasText(node.getSshKey()) ? \"key\" : \"password\")")
    AdminClusterNodeVO toVO(ClusterNode node);

    List<AdminClusterNodeVO> toVOList(List<ClusterNode> nodes);

    default String formatBytes(Long bytes) {
        if (bytes == null || bytes <= 0) {
            return "0 B";
        }
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        digitGroups = Math.min(digitGroups, units.length - 1);
        return String.format(java.util.Locale.ROOT, "%.1f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }

}
