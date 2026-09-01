package com.humolix.volur.system.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageResponseVO implements Serializable {

    private String fileName;

    private String filePath;

    private Long fileSize;

    private String sha256;

}
