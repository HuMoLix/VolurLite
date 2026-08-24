package com.humolix.volur.challenge.object.vo.stage.ctf;

import com.humolix.volur.challenge.object.vo.pub.AttachmentVO;
import com.humolix.volur.challenge.object.vo.pub.HintVO;
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
public class CtfTrainingChallengeDetailResponseVO implements Serializable {

    private Long id;
    private String name;
    private String description;
    private String category;
    private Integer points;
    private Integer solvedCount;
    private Integer difficulty;
    private boolean solved;
    private boolean dynamicContainer;
    private String containerUrl;
    private Integer remainingTimeSeconds;

    @Builder.Default
    private List<AttachmentVO> attachments = new ArrayList<>();

    @Builder.Default
    private List<HintVO> hints = new ArrayList<>();

}
