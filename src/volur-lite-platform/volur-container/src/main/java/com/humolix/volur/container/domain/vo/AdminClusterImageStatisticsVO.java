package com.humolix.volur.container.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminClusterImageStatisticsVO {

    @Builder.Default
    private Long totalImages = 0L;

    @Builder.Default
    private Long totalSizeBytes = 0L;

    @Builder.Default
    private Boolean registryOnline = false;

    @Builder.Default
    private Long registryImagesCount = 0L;

}
