package com.humolix.volur.container.mapper;

import com.humolix.volur.container.domain.entity.ClusterImage;
import com.humolix.volur.container.domain.vo.AdminClusterImageVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ClusterImageMapper {

    @Mapping(target = "fullImageName", expression = "java(buildFullName(image.getRepository(), image.getTag()))")
    @Mapping(target = "sizeFormatted", expression = "java(formatBytes(image.getSizeBytes()))")
    @Mapping(target = "nodeIps", expression = "java(parseNodeIps(image.getNodeIps()))")
    AdminClusterImageVO toVO(ClusterImage image);

    List<AdminClusterImageVO> toVOList(List<ClusterImage> images);

    default String buildFullName(String repository, String tag) {
        if (!StringUtils.hasText(repository)) return "";
        return StringUtils.hasText(tag) ? repository + ":" + tag : repository + ":latest";
    }

    default String formatBytes(Long bytes) {
        if (bytes == null || bytes <= 0) {
            return "0 B";
        }
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        digitGroups = Math.min(digitGroups, units.length - 1);
        return String.format(java.util.Locale.ROOT, "%.1f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    default List<String> parseNodeIps(String nodeIps) {
        if (!StringUtils.hasText(nodeIps)) {
            return Collections.emptyList();
        }
        return Arrays.stream(nodeIps.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

}
