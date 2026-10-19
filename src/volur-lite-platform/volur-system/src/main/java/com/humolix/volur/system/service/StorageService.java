package com.humolix.volur.system.service;

import com.humolix.volur.system.domain.vo.StorageResponseVO;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    StorageResponseVO uploadImage(MultipartFile file, String folder);

    StorageResponseVO uploadResource(MultipartFile file, String folder);

    void deleteFile(String filePath);

    String calculateMd5(MultipartFile file);

    String getFileMd5(String filePath);

}
