package com.humolix.volur.challenge.service.admin.ctf.impl;

import com.humolix.volur.framework.common.util.JsonUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import com.humolix.volur.challenge.domain.dto.ctf.CtfAttachmentDTO;
import com.humolix.volur.challenge.domain.dto.ctf.CtfChallengeCreateRequestDTO;
import com.humolix.volur.challenge.domain.dto.ctf.CtfChallengeUpdateRequestDTO;
import com.humolix.volur.challenge.domain.entity.ctf.CtfChallenge;
import com.humolix.volur.challenge.domain.entity.ctf.CtfChallengeCategory;
import com.humolix.volur.challenge.domain.enums.ChallengeDifficulty;
import com.humolix.volur.challenge.domain.enums.ChallengeType;
import com.humolix.volur.challenge.domain.vo.ChallengeImportResultVO;
import com.humolix.volur.challenge.domain.vo.ctf.CtfAttachmentVO;
import com.humolix.volur.challenge.domain.vo.ctf.CtfChallengeDetailVO;
import com.humolix.volur.challenge.domain.vo.ctf.CtfChallengeListVO;
import com.humolix.volur.challenge.event.ChallengeImagePullEvent;
import com.humolix.volur.challenge.mapper.ChallengeMapper;
import com.humolix.volur.challenge.repository.ctf.CtfChallengeCategoryRepository;
import com.humolix.volur.challenge.repository.ctf.CtfChallengeRepository;
import com.humolix.volur.challenge.service.admin.ctf.AdminCtfChallengeService;
import com.humolix.volur.challenge.util.InMemoryMultipartFile;
import com.humolix.volur.framework.cache.CacheNames;
import com.humolix.volur.framework.common.api.PageResponse;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.framework.common.thread.VolurThreadManager;
import com.humolix.volur.framework.common.util.SortUtils;
import com.humolix.volur.system.constant.SystemSettingKeys;
import com.humolix.volur.system.domain.vo.StorageResponseVO;
import com.humolix.volur.system.service.StorageService;
import com.humolix.volur.system.service.SystemSettingService;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminCtfChallengeServiceImpl implements AdminCtfChallengeService {

    private final CtfChallengeRepository challengeRepository;
    private final CtfChallengeCategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ChallengeMapper challengeMapper;
    private final VolurThreadManager volurThreadManager;
    private final StorageService storageService;
    private final SystemSettingService systemSettingService;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Override
    public PageResponse<CtfChallengeListVO> listChallenges(Integer page, Integer size, Long categoryId, String name) {
        Pageable pageable = SortUtils.createPageable(page, size);

        Specification<CtfChallenge> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (StringUtils.hasText(name)) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + name.trim().toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return PageResponse.of(challengeRepository.findAll(spec, pageable).map(challengeMapper::toCtfListVO));
    }

    @Override
    @Cacheable(cacheNames = CacheNames.CTF_CHALLENGE_DETAIL, key = "#id", sync = true)
    public CtfChallengeDetailVO getChallenge(Long id) {
        CtfChallenge challenge = challengeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "题目不存在"));
        return challengeMapper.toCtfDetailVO(challenge);
    }

    @Override
    @Transactional
    public Long createChallenge(CtfChallengeCreateRequestDTO request, Long authorId) {
        if (challengeRepository.existsByName(request.getName().trim())) {
            throw new BusinessException(HttpStatus.CONFLICT, "题目名称已存在");
        }

        CtfChallengeCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "指定的分类不存在"));

        User author = null;
        if (authorId != null) {
            author = userRepository.findById(authorId).orElse(null);
        }

        String attachmentsJson = null;
        String firstAttachmentUrl = request.getAttachmentUrl();
        String firstAttachmentName = request.getAttachmentName();

        if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
            attachmentsJson = JsonUtils.toJsonString(request.getAttachments());
            if (!StringUtils.hasText(firstAttachmentUrl)) {
                firstAttachmentUrl = request.getAttachments().get(0).getUrl();
            }
            if (!StringUtils.hasText(firstAttachmentName)) {
                firstAttachmentName = request.getAttachments().get(0).getName();
            }
        } else if (StringUtils.hasText(firstAttachmentUrl)) {
            List<CtfAttachmentDTO> singleList = List.of(new CtfAttachmentDTO(
                    StringUtils.hasText(firstAttachmentName) ? firstAttachmentName : "附件文件",
                    firstAttachmentUrl,
                    null
            ));
            attachmentsJson = JsonUtils.toJsonString(singleList);
        }

        CtfChallenge challenge = CtfChallenge.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .category(category)
                .author(author)
                .type(request.getType())
                .difficulty(request.getDifficulty() != null ? request.getDifficulty() : ChallengeDifficulty.EASY)
                .flag(request.getFlag())
                .attachmentUrl(firstAttachmentUrl)
                .attachmentName(firstAttachmentName)
                .attachments(attachmentsJson)
                .imageName(request.getImageName())
                .containerPort(request.getContainerPort())
                .cpuLimit(request.getCpuLimit() != null ? request.getCpuLimit() : 1)
                .memoryLimit(request.getMemoryLimit() != null ? request.getMemoryLimit() : 128)
                .build();

        CtfChallenge saved = challengeRepository.save(challenge);

        // 如果配置了镜像，自动触发镜像异步拉取任务
        if (StringUtils.hasText(saved.getImageName())) {
            eventPublisher.publishEvent(new ChallengeImagePullEvent(
                    this,
                    saved.getImageName().trim(),
                    saved.getId(),
                    saved.getName(),
                    authorId,
                    author != null ? author.getUsername() : "ADMIN"
            ));
        }

        return saved.getId();
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.CTF_CHALLENGE_DETAIL, key = "#id")
    public void updateChallenge(Long id, CtfChallengeUpdateRequestDTO request) {
        CtfChallenge challenge = challengeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "题目不存在"));

        if (request.getName() != null && !request.getName().isEmpty() && !challenge.getName().equals(request.getName())) {
            if (challengeRepository.existsByNameAndIdNot(request.getName(), id)) {
                throw new BusinessException(HttpStatus.CONFLICT, "题目名称已存在");
            }
            challenge.setName(request.getName());
        }

        if (request.getCategoryId() != null) {
            CtfChallengeCategory category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "指定的分类不存在"));
            challenge.setCategory(category);
        }

        if (request.getDescription() != null) challenge.setDescription(request.getDescription());
        if (request.getType() != null) challenge.setType(request.getType());
        if (request.getDifficulty() != null) challenge.setDifficulty(request.getDifficulty());
        if (request.getFlag() != null) challenge.setFlag(request.getFlag());

        if (request.getAttachments() != null) {
            if (request.getAttachments().isEmpty()) {
                challenge.setAttachments(null);
                challenge.setAttachmentUrl(null);
                challenge.setAttachmentName(null);
            } else {
                challenge.setAttachments(JsonUtils.toJsonString(request.getAttachments()));
                challenge.setAttachmentUrl(request.getAttachments().get(0).getUrl());
                challenge.setAttachmentName(request.getAttachments().get(0).getName());
            }
        } else {
            if (request.getAttachmentUrl() != null) challenge.setAttachmentUrl(request.getAttachmentUrl());
            if (request.getAttachmentName() != null) challenge.setAttachmentName(request.getAttachmentName());
            if (StringUtils.hasText(challenge.getAttachmentUrl()) && !StringUtils.hasText(challenge.getAttachments())) {
                challenge.setAttachments(JsonUtils.toJsonString(List.of(new CtfAttachmentDTO(
                        StringUtils.hasText(challenge.getAttachmentName()) ? challenge.getAttachmentName() : "附件文件",
                        challenge.getAttachmentUrl(),
                        null
                ))));
            }
        }

        String oldImage = challenge.getImageName();
        if (request.getImageName() != null) challenge.setImageName(request.getImageName());
        if (request.getContainerPort() != null) challenge.setContainerPort(request.getContainerPort());
        if (request.getCpuLimit() != null) challenge.setCpuLimit(request.getCpuLimit());
        if (request.getMemoryLimit() != null) challenge.setMemoryLimit(request.getMemoryLimit());

        CtfChallenge saved = challengeRepository.save(challenge);

        // 若镜像名称发生变更且非空，触发镜像异步拉取任务
        if (StringUtils.hasText(saved.getImageName()) && !saved.getImageName().equals(oldImage)) {
            eventPublisher.publishEvent(new ChallengeImagePullEvent(
                    this,
                    saved.getImageName().trim(),
                    saved.getId(),
                    saved.getName(),
                    null,
                    "ADMIN"
            ));
        }
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.CTF_CHALLENGE_DETAIL, key = "#id")
    public void deleteChallenge(Long id) {
        challengeRepository.deleteById(id);
    }

    @Override
    public ChallengeImportResultVO importChallenge(MultipartFile file, Long authorId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "请选择需要上传的题目 ZIP 压缩包");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".zip")) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "仅支持导入 .zip 格式的题目压缩包");
        }

        // 使用虚拟/计算线程池优化高并发导入性能
        return volurThreadManager.supplyCompute(() -> doProcessCtfZip(file, authorId)).join();
    }

    @Override
    public void downloadTemplate(HttpServletResponse response) {
        try {
            response.setContentType("application/zip");
            String fileName = "volur-ctf-challenge-template.zip";
            response.setHeader("Content-Disposition", "attachment; filename=\"" + URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20") + "\"");

            try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream(), StandardCharsets.UTF_8)) {
                ObjectNode chalObj = objectMapper.createObjectNode();
                chalObj.put("Name", "示例题目-Web基础");
                chalObj.put("Description", "### 题目描述\n\n这是一个用于演示 Volur 平台标准赛题包结构的示例题目。\n\n- 支持 Markdown 语法\n- 支持配置静态/动态靶机\n- 支持附带静态附件");
                chalObj.put("Category", "Web");
                chalObj.put("Points", 100);
                chalObj.put("Author", "Admin");
                chalObj.put("Difficulty", "Easy");

                ObjectNode dynamicType = objectMapper.createObjectNode();
                dynamicType.put("Mode", "Static");
                dynamicType.put("Flag", "flag{volur_standard_ctf_template_sample}");
                chalObj.set("DynamicType", dynamicType);

                ObjectNode container = objectMapper.createObjectNode();
                container.put("ContainerImage", "ctftraining/helloworld:latest");
                container.put("ContainerExposePort", 80);
                container.put("CPULimit", 1);
                container.put("MemoryLimit", 128);
                chalObj.set("Container", container);

                ArrayNode atts = objectMapper.createArrayNode();
                ObjectNode att = objectMapper.createObjectNode();
                att.put("Path", "attachment.txt");
                atts.add(att);
                chalObj.set("Attachments", atts);

                ObjectNode root = objectMapper.createObjectNode();
                root.set("Challenge", chalObj);
                byte[] jsonBytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(root);

                zos.putNextEntry(new ZipEntry("challenge.json"));
                zos.write(jsonBytes);
                zos.closeEntry();

                // 2. 示例附件 attachment.txt
                byte[] sampleAttachment = ("这是 Volur CTF 标准题目包模版的示例附件内容。\n" +
                        "如果赛题包含附件，可与 challenge.json 置于同一目录下，并在 challenge.json 的 Attachments 中指明相对路径。\n").getBytes(StandardCharsets.UTF_8);
                zos.putNextEntry(new ZipEntry("attachment.txt"));
                zos.write(sampleAttachment);
                zos.closeEntry();

                zos.finish();
            }
        } catch (IOException e) {
            log.error("[AdminCtfChallengeService] 下载标准 CTF 题目包模版失败", e);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "下载标准题目包模版失败: " + e.getMessage());
        }
    }

    private ChallengeImportResultVO doProcessCtfZip(MultipartFile file, Long authorId) {
        List<String> errors = new ArrayList<>();
        List<Long> importedIds = new ArrayList<>();
        List<String> triggeredImages = new ArrayList<>();
        int successCount = 0;
        int failedCount = 0;

        User author = null;
        if (authorId != null) {
            author = userRepository.findById(authorId).orElse(null);
        }

        // 1. 读取整个 Zip 到内存 Map
        Map<String, byte[]> rawEntries = new LinkedHashMap<>();
        try (ZipInputStream zis = new ZipInputStream(file.getInputStream(), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName().replace('\\', '/');
                if (shouldIgnoreEntry(name)) {
                    continue;
                }
                byte[] bytes = zis.readAllBytes();
                rawEntries.put(name, bytes);
            }
        } catch (Exception e) {
            log.error("[AdminCtfChallengeService] 解压题目 ZIP 包失败", e);
            throw new BusinessException(HttpStatus.BAD_REQUEST, "解析 ZIP 压缩包失败: " + e.getMessage());
        }

        if (rawEntries.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "上传的 ZIP 压缩包为空或无有效文件");
        }

        // 2. 检测是否存在嵌套题目包压缩包或多题目定义（取消批量导入，仅支持单题）
        Map<String, byte[]> allFiles = new LinkedHashMap<>(rawEntries);
        boolean containsNestedZips = rawEntries.keySet().stream().anyMatch(k -> k.toLowerCase().endsWith(".zip") && !k.contains("/"));
        boolean hasDirectJson = rawEntries.keySet().stream().anyMatch(this::isMetadataJson);
        if (containsNestedZips && !hasDirectJson) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "已取消批量导入功能，每次仅支持导入单个赛题包 ZIP");
        }

        // 3. 扫描 metadata JSON（challenge.json / build.json / chall.json / metadata.json）
        List<String> jsonEntryKeys = allFiles.keySet().stream()
                .filter(this::isMetadataJson)
                .toList();

        if (jsonEntryKeys.isEmpty()) {
            return ChallengeImportResultVO.builder()
                    .totalCount(1)
                    .failedCount(1)
                    .errors(List.of("压缩包内未找到 challenge.json 或 build.json 元数据文件"))
                    .build();
        }

        if (jsonEntryKeys.size() > 1) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "已取消批量导入功能，单次仅支持导入单个题目包。检测到压缩包包含 " + jsonEntryKeys.size() + " 个题目定义，请单独上传。");
        }

        String jsonKey = jsonEntryKeys.get(0);
        String dirPrefix = "";
        int lastSlash = jsonKey.lastIndexOf('/');
        if (lastSlash >= 0) {
            dirPrefix = jsonKey.substring(0, lastSlash + 1);
        }

        try {
            byte[] jsonBytes = allFiles.get(jsonKey);
            JsonNode root = objectMapper.readTree(jsonBytes);
            JsonNode chal = root.hasNonNull("Challenge") ? root.get("Challenge") : root;

            String chalName = chal.hasNonNull("Name") ? chal.get("Name").asText() : (chal.hasNonNull("name") ? chal.get("name").asText() : null);
            if (!StringUtils.hasText(chalName) && lastSlash > 0) {
                chalName = jsonKey.substring(0, lastSlash);
                if (chalName.contains("/")) {
                    chalName = chalName.substring(chalName.lastIndexOf('/') + 1);
                }
            }
            if (!StringUtils.hasText(chalName)) {
                errors.add(jsonKey + ": 题目名称 (Name) 不能为空");
                failedCount++;
            } else {
                chalName = chalName.trim();
                if (challengeRepository.existsByName(chalName)) {
                    errors.add("题目【" + chalName + "】导入失败: 数据库中已存在同名题目");
                    failedCount++;
                } else {
                    String desc = chal.hasNonNull("Description") ? chal.get("Description").asText() : (chal.hasNonNull("description") ? chal.get("description").asText() : null);

                    // 分类解析与自动创建
                    String catName = chal.hasNonNull("Category") ? chal.get("Category").asText() : (chal.hasNonNull("category") ? chal.get("category").asText() : null);
                    if (!StringUtils.hasText(catName)) catName = "Misc";
                    final String finalCatName = catName.trim();
                    CtfChallengeCategory category = categoryRepository.findByNameIgnoreCase(finalCatName)
                            .orElseGet(() -> categoryRepository.save(
                                    CtfChallengeCategory.builder().name(finalCatName).sortOrder(0).build()
                            ));

                    // 难度解析
                    ChallengeDifficulty difficulty = parseDifficulty(chal);

                    // Flag & 模式解析
                    JsonNode dynamicType = chal.hasNonNull("DynamicType") ? chal.get("DynamicType") : chal.get("dynamicType");
                    String mode = dynamicType != null && dynamicType.hasNonNull("Mode") ? dynamicType.get("Mode").asText() : (chal.hasNonNull("Mode") ? chal.get("Mode").asText() : null);
                    String flag = dynamicType != null && dynamicType.hasNonNull("Flag") ? dynamicType.get("Flag").asText() : (chal.hasNonNull("Flag") ? chal.get("Flag").asText() : null);
                    if (!StringUtils.hasText(flag) && dynamicType != null && dynamicType.hasNonNull("FlagTemplate")) {
                        flag = dynamicType.get("FlagTemplate").asText();
                    }
                    if (!StringUtils.hasText(flag) && chal.hasNonNull("flag")) {
                        flag = chal.get("flag").asText();
                    }

                    // 容器解析
                    JsonNode container = chal.hasNonNull("Container") ? chal.get("Container") : chal.get("container");
                    String imageName = container != null && container.hasNonNull("ContainerImage") ? container.get("ContainerImage").asText() : (chal.hasNonNull("imageName") ? chal.get("imageName").asText() : null);
                    Integer containerPort = container != null && container.hasNonNull("ContainerExposePort") ? container.get("ContainerExposePort").asInt() : (chal.hasNonNull("containerPort") ? chal.get("containerPort").asInt() : null);
                    Integer cpuLimit = 1;
                    Integer memoryLimit = 128;
                    if (container != null) {
                        if (container.hasNonNull("CPULimit")) {
                            cpuLimit = (int) Math.ceil(container.get("CPULimit").asDouble());
                        }
                        if (container.hasNonNull("MemoryLimit")) {
                            memoryLimit = container.get("MemoryLimit").asInt();
                        }
                    }

                    // 题型判定
                    ChallengeType type = ChallengeType.STATIC;
                    if (StringUtils.hasText(imageName) || "Dynamic".equalsIgnoreCase(mode)) {
                        type = ChallengeType.CONTAINER;
                    }

                    // 附件解析与存储落盘（支持多附件导入）
                    List<CtfAttachmentVO> importedAttachments = new ArrayList<>();

                    // 1. 先看 Attachments 配置中的文件路径
                    JsonNode atts = chal.get("Attachments");
                    if (atts != null && atts.isArray() && !atts.isEmpty()) {
                        for (int i = 0; i < atts.size(); i++) {
                            JsonNode attItem = atts.get(i);
                            String pathInJson = attItem.hasNonNull("Path") ? attItem.get("Path").asText() : (attItem.hasNonNull("Name") ? attItem.get("Name").asText() : null);
                            if (StringUtils.hasText(pathInJson)) {
                                String attName = Paths.get(pathInJson).getFileName().toString();
                                byte[] attBytes = null;
                                String targetAttKey = dirPrefix + pathInJson;
                                if (allFiles.containsKey(targetAttKey)) {
                                    attBytes = allFiles.get(targetAttKey);
                                } else {
                                    String fn = Paths.get(pathInJson).getFileName().toString();
                                    for (String fk : allFiles.keySet()) {
                                        if (fk.startsWith(dirPrefix) && fk.endsWith("/" + fn)) {
                                            attBytes = allFiles.get(fk);
                                            break;
                                        }
                                    }
                                }
                                if (attBytes != null && attBytes.length > 0) {
                                    InMemoryMultipartFile inMemFile = new InMemoryMultipartFile(
                                            attName, attName, "application/octet-stream", attBytes
                                    );
                                    StorageResponseVO storageResp = storageService.uploadResource(inMemFile, "attachments");
                                    if (storageResp != null) {
                                        importedAttachments.add(CtfAttachmentVO.builder()
                                                .name(attName)
                                                .url(storageResp.getFilePath())
                                                .size((long) attBytes.length)
                                                .build());
                                    }
                                }
                            }
                        }
                    }

                    // 2. 如果配置中未指定，选取该目录下除了 JSON 外的所有非目录文件
                    if (importedAttachments.isEmpty()) {
                        for (Map.Entry<String, byte[]> entry : allFiles.entrySet()) {
                            String fk = entry.getKey();
                            if (fk.startsWith(dirPrefix) && !isMetadataJson(fk) && !fk.endsWith("/")) {
                                String attName = Paths.get(fk).getFileName().toString();
                                byte[] attBytes = entry.getValue();
                                if (attBytes != null && attBytes.length > 0) {
                                    InMemoryMultipartFile inMemFile = new InMemoryMultipartFile(
                                            attName, attName, "application/octet-stream", attBytes
                                    );
                                    StorageResponseVO storageResp = storageService.uploadResource(inMemFile, "attachments");
                                    if (storageResp != null) {
                                        importedAttachments.add(CtfAttachmentVO.builder()
                                                .name(attName)
                                                .url(storageResp.getFilePath())
                                                .size((long) attBytes.length)
                                                .build());
                                    }
                                }
                            }
                        }
                    }

                    String attachmentName = !importedAttachments.isEmpty() ? importedAttachments.get(0).getName() : null;
                    String attachmentUrl = !importedAttachments.isEmpty() ? importedAttachments.get(0).getUrl() : null;
                    String attachmentsJson = !importedAttachments.isEmpty() ? JsonUtils.toJsonString(importedAttachments) : null;

                    // 创建并保存实体
                    CtfChallenge newChal = CtfChallenge.builder()
                            .name(chalName)
                            .description(desc)
                            .category(category)
                            .author(author)
                            .type(type)
                            .difficulty(difficulty)
                            .flag(flag)
                            .attachmentName(attachmentName)
                            .attachmentUrl(attachmentUrl)
                            .attachments(attachmentsJson)
                            .imageName(imageName)
                            .containerPort(containerPort)
                            .cpuLimit(cpuLimit)
                            .memoryLimit(memoryLimit)
                            .build();

                    CtfChallenge saved = challengeRepository.save(newChal);
                    importedIds.add(saved.getId());
                    successCount++;

                    // 如果配置了容器镜像，发布事件触发后台异步任务自动下载镜像
                    if (StringUtils.hasText(imageName)) {
                        eventPublisher.publishEvent(new ChallengeImagePullEvent(
                                this,
                                imageName.trim(),
                                saved.getId(),
                                saved.getName(),
                                authorId,
                                author != null ? author.getUsername() : "ADMIN"
                        ));
                        triggeredImages.add(imageName.trim());
                    }
                }
            }
        } catch (Exception ex) {
            log.error("[AdminCtfChallengeService] 解析题目元数据出错: jsonKey={}", jsonKey, ex);
            errors.add(jsonKey + " 解析失败: " + ex.getMessage());
            failedCount++;
        }

        return ChallengeImportResultVO.builder()
                .totalCount(1)
                .successCount(successCount)
                .failedCount(failedCount)
                .errors(errors)
                .importedChallengeIds(importedIds)
                .triggeredImagePullTasks(triggeredImages)
                .build();
    }

    @Override
    public void exportChallenges(List<Long> challengeIds, HttpServletResponse response) {
        List<CtfChallenge> challenges;
        if (challengeIds != null && !challengeIds.isEmpty()) {
            challenges = challengeRepository.findAllById(challengeIds);
        } else {
            challenges = challengeRepository.findAll();
        }

        if (challenges.isEmpty()) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "没有找到可供导出的 CTF 题目");
        }

        String baseDir = systemSettingService.getSettingValue(SystemSettingKeys.STORAGE_LOCAL_UPLOAD_DIR, "uploads/");
        if (!baseDir.endsWith("/")) {
            baseDir += "/";
        }

        try {
            response.setContentType("application/zip");
            String fileName = (challenges.size() == 1 ? challenges.get(0).getName() : "ctf-challenges-export") + ".zip";
            response.setHeader("Content-Disposition", "attachment; filename=\"" + URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20") + "\"");

            try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream(), StandardCharsets.UTF_8)) {
                for (CtfChallenge challenge : challenges) {
                    String folder = challenge.getName() + "/";

                    ObjectNode chalObj = objectMapper.createObjectNode();
                    chalObj.put("Name", challenge.getName());
                    chalObj.put("Description", challenge.getDescription());
                    chalObj.put("Category", challenge.getCategoryName() != null ? challenge.getCategoryName() : "Misc");
                    chalObj.put("Points", 100);
                    chalObj.put("Author", challenge.getAuthor() != null ? challenge.getAuthor().getUsername() : "Admin");
                    chalObj.put("Difficulty", mapDifficultyToString(challenge.getDifficulty()));

                    ObjectNode dynamicType = objectMapper.createObjectNode();
                    dynamicType.put("Mode", challenge.getType() == ChallengeType.STATIC ? "Static" : "Dynamic");
                    if (StringUtils.hasText(challenge.getFlag())) {
                        dynamicType.put("Flag", challenge.getFlag());
                    }
                    chalObj.set("DynamicType", dynamicType);

                    if (StringUtils.hasText(challenge.getImageName())) {
                        ObjectNode container = objectMapper.createObjectNode();
                        container.put("ContainerImage", challenge.getImageName());
                        container.put("ContainerExposePort", challenge.getContainerPort() != null ? challenge.getContainerPort() : 8000);
                        container.put("CPULimit", challenge.getCpuLimit() != null ? challenge.getCpuLimit() : 1);
                        container.put("MemoryLimit", challenge.getMemoryLimit() != null ? challenge.getMemoryLimit() : 128);
                        chalObj.set("Container", container);
                    }

                    List<CtfAttachmentVO> exportAtts = new ArrayList<>();
                    if (StringUtils.hasText(challenge.getAttachments())) {
                        try {
                            List<CtfAttachmentVO> parsed = JsonUtils.parseList(challenge.getAttachments(), CtfAttachmentVO.class);
                            if (parsed != null) exportAtts.addAll(parsed);
                        } catch (Exception ignored) {}
                    }
                    if (exportAtts.isEmpty() && StringUtils.hasText(challenge.getAttachmentName()) && StringUtils.hasText(challenge.getAttachmentUrl())) {
                        exportAtts.add(CtfAttachmentVO.builder()
                                .name(challenge.getAttachmentName())
                                .url(challenge.getAttachmentUrl())
                                .build());
                    }

                    if (!exportAtts.isEmpty()) {
                        ArrayNode atts = objectMapper.createArrayNode();
                        for (CtfAttachmentVO attVO : exportAtts) {
                            if (StringUtils.hasText(attVO.getName())) {
                                ObjectNode att = objectMapper.createObjectNode();
                                att.put("Path", attVO.getName());
                                atts.add(att);
                            }
                        }
                        chalObj.set("Attachments", atts);
                    }

                    ObjectNode root = objectMapper.createObjectNode();
                    root.set("Challenge", chalObj);
                    byte[] jsonBytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(root);

                    zos.putNextEntry(new ZipEntry(folder + "challenge.json"));
                    zos.write(jsonBytes);
                    zos.closeEntry();

                    for (CtfAttachmentVO attVO : exportAtts) {
                        if (StringUtils.hasText(attVO.getUrl()) && StringUtils.hasText(attVO.getName())) {
                            byte[] attBytes = loadAttachmentBytes(attVO.getUrl(), baseDir);
                            if (attBytes != null && attBytes.length > 0) {
                                zos.putNextEntry(new ZipEntry(folder + attVO.getName()));
                                zos.write(attBytes);
                                zos.closeEntry();
                            }
                        }
                    }
                }
                zos.finish();
            }
        } catch (IOException e) {
            log.error("[AdminCtfChallengeService] 导出 CTF 题目压缩包失败", e);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "导出 CTF 题目失败: " + e.getMessage());
        }
    }

    @Override
    public CtfAttachmentVO uploadAttachment(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "上传附件不能为空");
        }
        StorageResponseVO storageResp = storageService.uploadResource(file, "attachments");
        if (storageResp == null) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "附件上传失败");
        }
        return CtfAttachmentVO.builder()
                .name(storageResp.getFileName())
                .url(storageResp.getFilePath())
                .size(file.getSize())
                .build();
    }

    private byte[] loadAttachmentBytes(String attachmentUrl, String baseDir) {
        try {
            String pathStr = attachmentUrl;
            if (pathStr.startsWith("/api/v1/")) {
                pathStr = pathStr.substring("/api/v1/".length());
            }
            Path p = Paths.get(pathStr);
            if (!Files.exists(p)) {
                p = Paths.get(baseDir, pathStr).normalize();
            }
            if (!Files.exists(p)) {
                String filename = Paths.get(pathStr).getFileName().toString();
                Path searchP = Paths.get(baseDir, "attachments", filename);
                if (Files.exists(searchP)) {
                    p = searchP;
                }
            }
            if (Files.exists(p) && Files.isRegularFile(p)) {
                return Files.readAllBytes(p);
            }
        } catch (Exception e) {
            log.warn("[AdminCtfChallengeService] 读取附件文件字节失败: url={}", attachmentUrl, e);
        }
        return null;
    }

    private boolean isMetadataJson(String path) {
        String name = Paths.get(path).getFileName().toString().toLowerCase();
        return name.equals("challenge.json") || name.equals("build.json") || name.equals("chall.json") || name.equals("metadata.json");
    }

    private boolean shouldIgnoreEntry(String path) {
        String name = Paths.get(path).getFileName().toString().toLowerCase();
        return path.contains("__MACOSX/") || name.equals(".ds_store") || name.startsWith("._") || name.endsWith(".git");
    }

    private ChallengeDifficulty parseDifficulty(JsonNode chal) {
        JsonNode diffNode = chal.hasNonNull("Difficulty") ? chal.get("Difficulty") : chal.get("difficulty");
        if (diffNode == null || diffNode.isNull() || diffNode.isMissingNode()) {
            return ChallengeDifficulty.EASY;
        }
        if (diffNode.isNumber()) {
            ChallengeDifficulty diff = ChallengeDifficulty.fromCode(diffNode.asInt());
            return diff != null ? diff : ChallengeDifficulty.EASY;
        }
        ChallengeDifficulty diff = ChallengeDifficulty.fromValue(diffNode.asText());
        return diff != null ? diff : ChallengeDifficulty.EASY;
    }

    private String mapDifficultyToString(ChallengeDifficulty diff) {
        if (diff == null) return "easy";
        return diff.getDescription();
    }

}
