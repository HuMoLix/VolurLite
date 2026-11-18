package com.humolix.volur.training.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 附件信息（来自题库原题，前端拿 url 直接下载）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingAttachmentVO {

    private String name;

    private String url;

}
