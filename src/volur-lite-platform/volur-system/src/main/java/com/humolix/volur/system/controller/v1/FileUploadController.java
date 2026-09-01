package com.humolix.volur.system.controller.v1;

import com.humolix.volur.framework.common.api.ApiResponse;
import com.humolix.volur.framework.common.api.ApiResponseEntity;
import com.humolix.volur.logging.annotation.Syslog;
import com.humolix.volur.system.domain.vo.StorageResponseVO;
import com.humolix.volur.system.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "文件上传服务", description = "提供图片与通用资源文件的动态存储上传接口")
@RestController
@RequestMapping("/api/v1/system/upload")
@RequiredArgsConstructor
public class FileUploadController {

    private final StorageService storageService;

    @Operation(summary = "上传图片文件")
    @Syslog(module = "SYSTEM", value = "上传图片文件", logType = "OPERATION", recordParams = false)
    @PostMapping("/image")
    public ApiResponse<StorageResponseVO> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "images") String folder) {
        StorageResponseVO response = storageService.uploadImage(file, folder);
        return ApiResponseEntity.ok("图片上传成功", response);
    }

    @Operation(summary = "上传通用资源文件")
    @Syslog(module = "SYSTEM", value = "上传通用资源文件", logType = "OPERATION", recordParams = false)
    @PostMapping("/resource")
    public ApiResponse<StorageResponseVO> uploadResource(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "resources") String folder) {
        StorageResponseVO response = storageService.uploadResource(file, folder);
        return ApiResponseEntity.ok("资源文件上传成功", response);
    }

}
