package com.humolix.volur.challenge.object.vo.pub;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentVO implements Serializable {

    private String attachmentId;
    private String fileName;
    private String fileUrl;
    private String fileExtension;

}
