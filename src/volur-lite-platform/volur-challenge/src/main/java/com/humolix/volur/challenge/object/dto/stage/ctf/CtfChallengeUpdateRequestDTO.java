package com.humolix.volur.challenge.object.dto.stage.ctf;

import com.humolix.volur.framework.common.dto.AttachmentDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CtfChallengeUpdateRequestDTO {

    private String name;
    private String description;
    private Long categoryId;
    private Integer difficulty;
    private String staticFlag;
    private Boolean dynamicContainer;
    private List<AttachmentDTO> attachments;
    private String imageName;
    private Integer containerPort;

}
