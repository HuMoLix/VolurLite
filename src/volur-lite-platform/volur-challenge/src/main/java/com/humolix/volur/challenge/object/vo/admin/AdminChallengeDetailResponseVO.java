package com.humolix.volur.challenge.object.vo.admin;

import com.humolix.volur.framework.common.dto.AttachmentDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminChallengeDetailResponseVO implements Serializable {

    private Long id;
    private String name;
    private String description;
    private Long categoryId;
    private String categoryName;
    private Long authorId;
    private String authorName;
    private Integer difficulty;
    private String staticFlag;
    private boolean dynamicContainer;

    @Builder.Default
    private List<AttachmentDTO> attachments = new ArrayList<>();

    private String imageName;
    private Integer containerPort;
    private boolean deleted;
    private Instant createdAt;
    private Instant updatedAt;

}
