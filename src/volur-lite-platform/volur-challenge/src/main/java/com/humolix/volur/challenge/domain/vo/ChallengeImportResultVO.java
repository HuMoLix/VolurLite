package com.humolix.volur.challenge.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeImportResultVO implements Serializable {

    @Builder.Default
    private int totalCount = 0;

    @Builder.Default
    private int successCount = 0;

    @Builder.Default
    private int failedCount = 0;

    @Builder.Default
    private List<String> errors = new ArrayList<>();

    @Builder.Default
    private List<Long> importedChallengeIds = new ArrayList<>();

    @Builder.Default
    private List<String> triggeredImagePullTasks = new ArrayList<>();

}
