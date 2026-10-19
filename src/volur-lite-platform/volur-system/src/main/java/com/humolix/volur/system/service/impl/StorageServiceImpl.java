package com.humolix.volur.system.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.system.constant.SystemSettingKeys;
import com.humolix.volur.system.domain.vo.StorageResponseVO;
import com.humolix.volur.system.service.StorageService;
import com.humolix.volur.system.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageServiceImpl implements StorageService {

    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "webp", "svg");
    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024; // 5MB
    private static final long MAX_RESOURCE_SIZE = 1024L * 1024 * 1024; // 1GB (1024MB)

    private final SystemSettingService systemSettingService;

    @Override
    public StorageResponseVO uploadImage(MultipartFile file, String folder) {
        validateFile(file, ALLOWED_IMAGE_EXTENSIONS, MAX_IMAGE_SIZE);
        validateImageContent(file);
        String safeFolder = sanitizeFolder(folder, "images");
        return executeUpload(file, safeFolder);
    }

    @Override
    public StorageResponseVO uploadResource(MultipartFile file, String folder) {
        validateFile(file, null, MAX_RESOURCE_SIZE);
        String safeFolder = sanitizeFolder(folder, "resources");
        return executeUpload(file, safeFolder);
    }

    private void validateFile(MultipartFile file, List<String> allowedExtensions, long maxSize) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "上传文件不能为空");
        }
        if (file.getSize() > maxSize) {
            String limitDesc = maxSize >= 1024L * 1024 * 1024
                    ? (maxSize / (1024L * 1024 * 1024)) + "GB"
                    : (maxSize / (1024L * 1024)) + "MB";
            throw new BusinessException(HttpStatus.BAD_REQUEST, "文件大小超过限制: " + limitDesc);
        }
        if (allowedExtensions != null) {
            String extension = FilenameUtils.getExtension(file.getOriginalFilename());
            if (extension == null || !allowedExtensions.contains(extension.toLowerCase())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "不支持的文件格式: " + extension);
            }
        }
    }

    private void validateImageContent(MultipartFile file) {
        String extension = FilenameUtils.getExtension(file.getOriginalFilename());
        if (StringUtils.hasText(extension) && "svg".equalsIgnoreCase(extension)) {
            return;
        }
        try (InputStream is = file.getInputStream()) {
            BufferedImage image = ImageIO.read(is);
            if (image == null) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "非法的图片内容");
            }
        } catch (IOException e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "图片校验解析失败");
        }
    }

    private String sanitizeFolder(String folder, String defaultFolder) {
        if (!StringUtils.hasText(folder)) {
            return defaultFolder;
        }
        String sanitized = folder.trim().replace("\\", "/").replaceAll("/+", "/");
        if (sanitized.startsWith("/")) sanitized = sanitized.substring(1);
        if (sanitized.endsWith("/")) sanitized = sanitized.substring(0, sanitized.length() - 1);
        if (sanitized.contains("..")) {
            return defaultFolder;
        }
        return sanitized.isEmpty() ? defaultFolder : sanitized;
    }

    private StorageResponseVO executeUpload(MultipartFile file, String folder) {
        String storageType = systemSettingService.getSettingValue(SystemSettingKeys.STORAGE_TYPE, "LOCAL");
        if ("OSS".equalsIgnoreCase(storageType)) {
            return uploadToOSS(file, folder);
        } else {
            return uploadToLocal(file, folder);
        }
    }

    private StorageResponseVO uploadToLocal(MultipartFile file, String folder) {
        try {
            String originalFilename = file.getOriginalFilename();
            String extension = FilenameUtils.getExtension(originalFilename);
            String safeExt = StringUtils.hasText(extension) ? "." + extension.toLowerCase() : "";
            String newFileName = IdUtil.fastSimpleUUID() + safeExt;

            String baseDir = Optional.ofNullable(systemSettingService.getSettingValue(SystemSettingKeys.STORAGE_LOCAL_UPLOAD_DIR))
                    .filter(StringUtils::hasText)
                    .orElse("uploads/");
            if (!baseDir.endsWith("/")) {
                baseDir += "/";
            }

            Path folderPath = Paths.get(baseDir, folder).toAbsolutePath().normalize();
            if (!Files.exists(folderPath)) {
                Files.createDirectories(folderPath);
            }

            Path targetPath = folderPath.resolve(newFileName).normalize();
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            String sha256;
            try (InputStream in = file.getInputStream()) {
                sha256 = DigestUtil.sha256Hex(in);
            }

            String webPath = "/api/v1/" + baseDir + folder + "/" + newFileName;
            webPath = webPath.replaceAll("(?<!http:)(?<!https:)/{2,}", "/");

            return StorageResponseVO.builder()
                    .fileName(newFileName)
                    .filePath(webPath)
                    .fileSize(file.getSize())
                    .sha256(sha256)
                    .build();
        } catch (IOException e) {
            log.error("[StorageService] Local upload failed", e);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "文件本地存储失败");
        }
    }

    private StorageResponseVO uploadToOSS(MultipartFile file, String folder) {
        String endpoint = systemSettingService.getSettingValue(SystemSettingKeys.STORAGE_OSS_ENDPOINT);
        String accessKeyId = systemSettingService.getSettingValue(SystemSettingKeys.STORAGE_OSS_ACCESS_KEY);
        String accessKeySecret = systemSettingService.getSettingValue(SystemSettingKeys.STORAGE_OSS_SECRET_KEY);
        String bucketName = systemSettingService.getSettingValue(SystemSettingKeys.STORAGE_OSS_BUCKET);

        if (!StringUtils.hasText(endpoint) || !StringUtils.hasText(accessKeyId) || !StringUtils.hasText(accessKeySecret) || !StringUtils.hasText(bucketName)) {
            log.warn("[StorageService] OSS configuration incomplete, falling back to Local storage");
            return uploadToLocal(file, folder);
        }

        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try {
            String originalFilename = file.getOriginalFilename();
            String extension = FilenameUtils.getExtension(originalFilename);
            String safeExt = StringUtils.hasText(extension) ? "." + extension.toLowerCase() : "";
            String newFileName = IdUtil.fastSimpleUUID() + safeExt;
            String objectName = folder + "/" + newFileName;

            try (InputStream is = file.getInputStream()) {
                ossClient.putObject(bucketName, objectName, is);
            }

            String sha256;
            try (InputStream is = file.getInputStream()) {
                sha256 = DigestUtil.sha256Hex(is);
            }

            String cleanEndpoint = endpoint.replace("https://", "").replace("http://", "");
            String url = "https://" + bucketName + "." + cleanEndpoint + "/" + objectName;

            return StorageResponseVO.builder()
                    .fileName(newFileName)
                    .filePath(url)
                    .fileSize(file.getSize())
                    .sha256(sha256)
                    .build();
        } catch (IOException e) {
            log.error("[StorageService] OSS upload failed", e);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "OSS文件存储失败");
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    @Override
    public void deleteFile(String filePath) {
        if (!StringUtils.hasText(filePath)) {
            return;
        }

        String storageType = systemSettingService.getSettingValue(SystemSettingKeys.STORAGE_TYPE, "LOCAL");
        if ("OSS".equalsIgnoreCase(storageType) || filePath.startsWith("http://") || filePath.startsWith("https://")) {
            deleteFromOSS(filePath);
        } else {
            deleteFromLocal(filePath);
        }
    }

    private void deleteFromLocal(String filePath) {
        try {
            String relativePath = filePath.startsWith("/") ? filePath.substring(1) : filePath;
            if (relativePath.startsWith("api/v1/uploads/")) {
                relativePath = relativePath.substring(7); // "uploads/..."
            }
            Path path = Paths.get(relativePath).toAbsolutePath().normalize();
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.error("[StorageService] Failed to delete local file: {}", filePath, e);
        }
    }

    private void deleteFromOSS(String filePath) {
        String endpoint = systemSettingService.getSettingValue(SystemSettingKeys.STORAGE_OSS_ENDPOINT);
        String accessKeyId = systemSettingService.getSettingValue(SystemSettingKeys.STORAGE_OSS_ACCESS_KEY);
        String accessKeySecret = systemSettingService.getSettingValue(SystemSettingKeys.STORAGE_OSS_SECRET_KEY);
        String bucketName = systemSettingService.getSettingValue(SystemSettingKeys.STORAGE_OSS_BUCKET);

        if (!StringUtils.hasText(endpoint) || !StringUtils.hasText(accessKeyId) || !StringUtils.hasText(accessKeySecret) || !StringUtils.hasText(bucketName)) {
            return;
        }

        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try {
            String cleanEndpoint = endpoint.replace("https://", "").replace("http://", "");
            String host = bucketName + "." + cleanEndpoint;
            int index = filePath.indexOf(host);
            if (index != -1) {
                String objectName = filePath.substring(index + host.length() + 1);
                ossClient.deleteObject(bucketName, objectName);
            }
        } catch (Exception e) {
            log.error("[StorageService] Failed to delete OSS object: {}", filePath, e);
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    @Override
    public String calculateMd5(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        try (InputStream in = file.getInputStream()) {
            return DigestUtil.md5Hex(in);
        } catch (IOException e) {
            log.error("[StorageService] Failed to calculate MultipartFile MD5", e);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "计算文件MD5失败");
        }
    }

    @Override
    public String getFileMd5(String filePath) {
        if (!StringUtils.hasText(filePath)) {
            return null;
        }
        String storageType = systemSettingService.getSettingValue(SystemSettingKeys.STORAGE_TYPE, "LOCAL");
        if ("OSS".equalsIgnoreCase(storageType) || filePath.startsWith("http://") || filePath.startsWith("https://")) {
            return getOssFileMd5(filePath);
        } else {
            return getLocalFileMd5(filePath);
        }
    }

    private String getLocalFileMd5(String filePath) {
        try {
            String relativePath = filePath.startsWith("/") ? filePath.substring(1) : filePath;
            if (relativePath.startsWith("api/v1/uploads/")) {
                relativePath = relativePath.substring(7); // "uploads/..."
            }
            Path path = Paths.get(relativePath).toAbsolutePath().normalize();
            if (Files.exists(path) && Files.isRegularFile(path)) {
                try (InputStream in = Files.newInputStream(path)) {
                    return DigestUtil.md5Hex(in);
                }
            }
            return null;
        } catch (IOException e) {
            log.warn("[StorageService] Failed to calculate local file MD5 for: {}", filePath, e);
            return null;
        }
    }

    private String getOssFileMd5(String filePath) {
        String endpoint = systemSettingService.getSettingValue(SystemSettingKeys.STORAGE_OSS_ENDPOINT);
        String accessKeyId = systemSettingService.getSettingValue(SystemSettingKeys.STORAGE_OSS_ACCESS_KEY);
        String accessKeySecret = systemSettingService.getSettingValue(SystemSettingKeys.STORAGE_OSS_SECRET_KEY);
        String bucketName = systemSettingService.getSettingValue(SystemSettingKeys.STORAGE_OSS_BUCKET);

        if (!StringUtils.hasText(endpoint) || !StringUtils.hasText(accessKeyId) || !StringUtils.hasText(accessKeySecret) || !StringUtils.hasText(bucketName)) {
            return null;
        }

        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try {
            String cleanEndpoint = endpoint.replace("https://", "").replace("http://", "");
            String host = bucketName + "." + cleanEndpoint;
            int index = filePath.indexOf(host);
            if (index != -1) {
                String objectName = filePath.substring(index + host.length() + 1);
                com.aliyun.oss.model.OSSObject ossObject = ossClient.getObject(bucketName, objectName);
                if (ossObject != null) {
                    try (InputStream is = ossObject.getObjectContent()) {
                        return DigestUtil.md5Hex(is);
                    }
                }
            }
            return null;
        } catch (Exception e) {
            log.warn("[StorageService] Failed to calculate OSS file MD5 for: {}", filePath, e);
            return null;
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

}
