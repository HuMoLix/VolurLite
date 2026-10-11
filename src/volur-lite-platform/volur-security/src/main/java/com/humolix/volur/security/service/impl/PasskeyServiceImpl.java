package com.humolix.volur.security.service.impl;

import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.framework.common.util.RequestUtil;
import com.humolix.volur.security.domain.dto.PasskeyAssertionVerifyRequestDTO;
import com.humolix.volur.security.domain.dto.PasskeyRegisterRequestDTO;
import com.humolix.volur.security.domain.vo.PasskeyAuthenticatorOptionVO;
import com.humolix.volur.security.domain.vo.PasskeyCredentialVO;
import com.humolix.volur.security.domain.vo.PasskeyRegisterResponseVO;
import com.humolix.volur.security.domain.vo.FlowResponseVO;
import com.humolix.volur.security.flow.TransitionLockManager;
import com.humolix.volur.security.flow.flows.SigninFlow;
import com.humolix.volur.security.service.PasskeyService;
import com.humolix.volur.security.session.SessionManager;
import com.humolix.volur.security.session.domain.Session;
import com.humolix.volur.security.session.validator.SessionDeviceBaselineValidator;
import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.repository.UserRepository;
import com.humolix.volur.user.service.UserTrustedDeviceService;
import com.humolix.volur.system.constant.SystemSettingKeys;
import com.humolix.volur.system.service.SystemSettingService;
import com.humolix.volur.verification.risk.engine.RiskEvaluationEngine;
import com.humolix.volur.verification.risk.enums.CaptchaScene;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.webauthn.api.AuthenticatorAssertionResponse;
import org.springframework.security.web.webauthn.api.AuthenticatorAttachment;
import org.springframework.security.web.webauthn.api.AuthenticatorAttestationResponse;
import org.springframework.security.web.webauthn.api.AuthenticatorSelectionCriteria;
import org.springframework.security.web.webauthn.api.AuthenticatorTransport;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.CredentialRecord;
import org.springframework.security.web.webauthn.api.PublicKeyCredential;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialDescriptor;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRequestOptions;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.api.ResidentKeyRequirement;
import org.springframework.security.web.webauthn.api.UserVerificationRequirement;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.springframework.security.web.webauthn.management.ImmutablePublicKeyCredentialCreationOptionsRequest;
import org.springframework.security.web.webauthn.management.ImmutablePublicKeyCredentialRequestOptionsRequest;
import org.springframework.security.web.webauthn.management.ImmutableRelyingPartyRegistrationRequest;
import org.springframework.security.web.webauthn.management.RelyingPartyAuthenticationRequest;
import org.springframework.security.web.webauthn.management.RelyingPartyPublicKey;
import org.springframework.security.web.webauthn.management.RelyingPartyRegistrationRequest;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasskeyServiceImpl implements PasskeyService {

    private final SessionManager sessionManager;
    private final TransitionLockManager transitionLockManager;
    private final WebAuthnRelyingPartyOperations rpOperations;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final SessionDeviceBaselineValidator sessionDeviceBaselineValidator;
    private final UserRepository userRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final UserTrustedDeviceService userTrustedDeviceService;
    private final RiskEvaluationEngine riskEvaluationEngine;
    private final SystemSettingService systemSettingService;

    private static final String VREDIS_SECURITY_PASSKEY_CREATION_KEY_PREFIX = "vredis:security:passkey:creation:";
    private static final String VREDIS_SECURITY_PASSKEY_ASSERTION_KEY_PREFIX = "vredis:security:passkey:assertion:";
    private static final Duration PASSKEY_CREATION_TTL = Duration.ofMinutes(5);
    private static final Duration PASSKEY_ASSERTION_TTL = Duration.ofMinutes(5);

    @Override
    public List<PasskeyAuthenticatorOptionVO> getSupportedAuthenticators() {
        if (!systemSettingService.getBoolean(SystemSettingKeys.WEBAUTHN_ENABLED, true)) {
            return List.of();
        }
        return List.of(
                PasskeyAuthenticatorOptionVO.builder()
                        .key("windows_password_manager")
                        .name("Windows 密码管理器")
                        .attachment("PLATFORM")
                        .description("使用 Windows 凭据管理器保护的通行密钥")
                        .icon("windows-password-manager")
                        .available(true)
                        .build(),
                PasskeyAuthenticatorOptionVO.builder()
                        .key("windows_hello")
                        .name("Windows Hello")
                        .attachment("PLATFORM")
                        .description("使用面孔、指纹或 PIN 码快速安全登录")
                        .icon("windows-hello")
                        .available(true)
                        .build(),
                PasskeyAuthenticatorOptionVO.builder()
                        .key("security_key")
                        .name("Windows 安全中心的安全密钥U盘")
                        .attachment("CROSS_PLATFORM")
                        .description("使用插入电脑的物理 FIDO2/WebAuthn USB 安全密钥")
                        .icon("usb-key")
                        .available(true)
                        .build(),
                PasskeyAuthenticatorOptionVO.builder()
                        .key("mobile_device")
                        .name("iPhone、iPad 或 Android 设备")
                        .attachment("CROSS_PLATFORM")
                        .description("使用移动设备扫描屏幕二维码进行跨设备配对认证")
                        .icon("mobile-phone")
                        .available(true)
                        .build(),
                PasskeyAuthenticatorOptionVO.builder()
                        .key("apple_keychain")
                        .name("苹果钥匙串")
                        .attachment("PLATFORM")
                        .description("使用 iCloud 钥匙串在 Apple 生态设备间同步通行密钥")
                        .icon("apple-keychain")
                        .available(true)
                        .build()
        );
    }

    @Override
    public PublicKeyCredentialCreationOptions getCreationOptions(String dsh, String tl, String authenticatorType) {
        assertWebAuthnEnabled();
        transitionLockManager.validateTransLock(dsh, tl, "pt/options");
        Session session = sessionManager.getSession(dsh);

        String username = (String) sessionManager.getMetadata(dsh, "username");
        if (!StringUtils.hasText(username) && session.getFlow() != null) {
            username = session.getFlow().getIdentifier();
        }
        if (!StringUtils.hasText(username)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "无法定位当前会话绑定的用户，请重新开始注册");
        }

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                username,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        PublicKeyCredentialCreationOptions options = rpOperations.createPublicKeyCredentialCreationOptions(
                new ImmutablePublicKeyCredentialCreationOptionsRequest(authentication)
        );

        AuthenticatorSelectionCriteria criteria = resolveCriteria(authenticatorType);
        if (criteria != null) {
            options = PublicKeyCredentialCreationOptions.builder()
                    .rp(options.getRp())
                    .user(options.getUser())
                    .challenge(options.getChallenge())
                    .pubKeyCredParams(options.getPubKeyCredParams())
                    .timeout(options.getTimeout())
                    .excludeCredentials(options.getExcludeCredentials())
                    .authenticatorSelection(criteria)
                    .attestation(options.getAttestation())
                    .extensions(options.getExtensions())
                    .build();
        }

        byte[] serialized = serialize(options);
        String base64 = Base64.getEncoder().encodeToString(serialized);
        String key = VREDIS_SECURITY_PASSKEY_CREATION_KEY_PREFIX + dsh;
        redisTemplate.opsForValue().set(key, base64, PASSKEY_CREATION_TTL);

        return options;
    }

    @Override
    public PasskeyRegisterResponseVO registerPasskey(String dsh, String tl, PasskeyRegisterRequestDTO request) {
        assertWebAuthnEnabled();
        transitionLockManager.validateTransLock(dsh, tl, "pt/options");

        String key = VREDIS_SECURITY_PASSKEY_CREATION_KEY_PREFIX + dsh;
        String base64 = redisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(base64)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "通行密钥挑战已失效或不存在，请重新生成");
        }

        PublicKeyCredentialCreationOptions creationOptions;
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            creationOptions = deserialize(bytes, PublicKeyCredentialCreationOptions.class);
        } catch (Exception e) {
            log.error("Failed to deserialize cached passkey creation options: {}", e.getMessage(), e);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "解析通行密钥会话失败");
        }

        if (creationOptions == null) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "通行密钥会话数据异常");
        }

        if (request.getCredential() == null || request.getCredential().isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "通行密钥凭据载荷不能为空");
        }
        PublicKeyCredential<AuthenticatorAttestationResponse> credential;
        try {
            String credentialJson = objectMapper.writeValueAsString(request.getCredential());
            credential = objectMapper.readValue(credentialJson, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to deserialize WebAuthn credential payload", e);
            throw new BusinessException(HttpStatus.BAD_REQUEST, "通行密钥凭据格式错误");
        }

        String label = StringUtils.hasText(request.getLabel()) ? request.getLabel().trim() : "我的通行密钥";
        RelyingPartyPublicKey relyingPartyPublicKey = new RelyingPartyPublicKey(credential, label);
        RelyingPartyRegistrationRequest registrationRequest = new ImmutableRelyingPartyRegistrationRequest(creationOptions, relyingPartyPublicKey);

        CredentialRecord record;
        try {
            record = rpOperations.registerCredential(registrationRequest);
        } catch (Exception e) {
            log.warn("WebAuthn register credential failed: {}", e.getMessage(), e);
            throw new BusinessException(HttpStatus.BAD_REQUEST, "通行密钥校验未通过: " + e.getMessage());
        }

        redisTemplate.delete(key);
        sessionManager.setMetadata(dsh, "passkeyRegistered", true);

        return PasskeyRegisterResponseVO.builder()
                .success(true)
                .credentialId(record.getCredentialId().toBase64UrlString())
                .build();
    }

    @Override
    public PublicKeyCredentialRequestOptions getAssertionOptions(String dsh, String tl) {
        assertWebAuthnEnabled();
        Session session = sessionManager.getSession(dsh);
        sessionDeviceBaselineValidator.validateBaseline(session);

        String currentStep = session.getFlow() != null ? session.getFlow().getCurrentStep() : "pk";
        transitionLockManager.validateTransLock(dsh, tl, currentStep);

        String username = session.getFlow() != null ? session.getFlow().getIdentifier() : null;
        if (!StringUtils.hasText(username)) {
            username = (String) sessionManager.getMetadata(dsh, "username");
        }
        if (!StringUtils.hasText(username)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "无法定位当前会话绑定的用户，请重新输入账号");
        }

        String cleanIdentifier = username.trim();
        User user = userRepository.findByUsername(cleanIdentifier)
                .or(() -> userRepository.findByEmail(cleanIdentifier.toLowerCase()))
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "用户名或邮箱不存在"));

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getUsername(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        PublicKeyCredentialRequestOptions options = rpOperations.createCredentialRequestOptions(
                new ImmutablePublicKeyCredentialRequestOptionsRequest(authentication)
        );

        List<PublicKeyCredentialDescriptor> allowCredentials = options.getAllowCredentials();
        if (allowCredentials == null || allowCredentials.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "当前账号未绑定通行密钥，无法使用此方式验证");
        }

        byte[] serialized = serialize(options);
        String base64 = Base64.getEncoder().encodeToString(serialized);
        String key = VREDIS_SECURITY_PASSKEY_ASSERTION_KEY_PREFIX + dsh;
        redisTemplate.opsForValue().set(key, base64, PASSKEY_ASSERTION_TTL);

        return options;
    }

    @Override
    public FlowResponseVO verifyPasskeyAssertion(PasskeyAssertionVerifyRequestDTO request) {
        assertWebAuthnEnabled();
        String dsh = request.getDsh();
        String tl = request.getTl();
        int cid = request.getCid() != null ? request.getCid() : 1;

        Session session = sessionManager.getSession(dsh);
        sessionDeviceBaselineValidator.validateBaseline(session);

        String currentStep = session.getFlow() != null ? session.getFlow().getCurrentStep() : "pk";
        transitionLockManager.validateTransLock(dsh, tl, currentStep);

        String username = session.getFlow() != null ? session.getFlow().getIdentifier() : null;
        if (!StringUtils.hasText(username)) {
            username = (String) sessionManager.getMetadata(dsh, "username");
        }
        if (!StringUtils.hasText(username)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "无法定位当前会话绑定的用户，请重新输入账号");
        }

        String cleanIdentifier = username.trim();
        User user = userRepository.findByUsername(cleanIdentifier)
                .or(() -> userRepository.findByEmail(cleanIdentifier.toLowerCase()))
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "用户名或邮箱不存在"));

        String key = VREDIS_SECURITY_PASSKEY_ASSERTION_KEY_PREFIX + dsh;
        String base64 = redisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(base64)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "通行密钥挑战已失效或不存在，请重新生成");
        }

        PublicKeyCredentialRequestOptions requestOptions;
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            requestOptions = deserialize(bytes, PublicKeyCredentialRequestOptions.class);
        } catch (Exception e) {
            log.error("Failed to deserialize cached passkey assertion options", e);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "解析通行密钥会话失败");
        }

        if (requestOptions == null) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "通行密钥会话数据异常");
        }

        if (request.getCredential() == null || request.getCredential().isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "通行密钥凭据载荷不能为空");
        }

        PublicKeyCredential<AuthenticatorAssertionResponse> credential;
        try {
            String credentialJson = objectMapper.writeValueAsString(request.getCredential());
            credential = objectMapper.readValue(credentialJson, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to deserialize WebAuthn assertion credential payload", e);
            throw new BusinessException(HttpStatus.BAD_REQUEST, "通行密钥凭据格式错误");
        }

        RelyingPartyAuthenticationRequest authRequest = new RelyingPartyAuthenticationRequest(requestOptions, credential);
        String clientIp = RequestUtil.getClientIp();
        PublicKeyCredentialUserEntity authenticatedUser;
        try {
            authenticatedUser = rpOperations.authenticate(authRequest);
        } catch (Exception e) {
            riskEvaluationEngine.recordFailure(clientIp, CaptchaScene.LOGIN);
            riskEvaluationEngine.recordAccountFailure(cleanIdentifier, CaptchaScene.LOGIN);
            log.warn("WebAuthn assertion authenticate failed: {}", e.getMessage(), e);
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "通行密钥核验未通过: " + e.getMessage());
        }

        if (authenticatedUser == null || !user.getUsername().equalsIgnoreCase(authenticatedUser.getName())) {
            riskEvaluationEngine.recordFailure(clientIp, CaptchaScene.LOGIN);
            riskEvaluationEngine.recordAccountFailure(cleanIdentifier, CaptchaScene.LOGIN);
            log.warn("WebAuthn assertion user mismatch: session user [{}], authenticated user [{}]",
                    user.getUsername(), authenticatedUser != null ? authenticatedUser.getName() : "null");
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "所使用的通行密钥不属于当前登录账号");
        }

        Bytes expectedUserId = new Bytes(ByteBuffer.allocate(Long.BYTES).putLong(user.getId()).array());
        if (authenticatedUser.getId() != null && !expectedUserId.equals(authenticatedUser.getId())) {
            riskEvaluationEngine.recordFailure(clientIp, CaptchaScene.LOGIN);
            riskEvaluationEngine.recordAccountFailure(cleanIdentifier, CaptchaScene.LOGIN);
            log.warn("WebAuthn assertion userId mismatch: session userId [{}], authenticated userId [{}]",
                    user.getId(), authenticatedUser.getId());
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "所使用的通行密钥不属于当前登录账号");
        }

        redisTemplate.delete(key);
        riskEvaluationEngine.recordSuccess(clientIp, CaptchaScene.LOGIN);
        riskEvaluationEngine.recordAccountSuccess(cleanIdentifier, CaptchaScene.LOGIN);

        String deviceHash = session.getRequestDeviceParams() != null
                ? session.getRequestDeviceParams().getDeviceHash()
                : null;
        if (!StringUtils.hasText(deviceHash) && session.getRequestDeviceParams() != null) {
            deviceHash = session.getRequestDeviceParams().getFingerprint();
        }
        if (StringUtils.hasText(deviceHash)) {
            String osFamily = session.getRequestDeviceParams() != null ? session.getRequestDeviceParams().getOsFamily() : null;
            String browserFamily = session.getRequestDeviceParams() != null ? session.getRequestDeviceParams().getBrowserFamily() : null;
            userTrustedDeviceService.recordLoginDevice(user.getId(), deviceHash, osFamily, browserFamily, clientIp);
        }

        SigninFlow signinFlow = SigninFlow.from(session.getFlow());
        signinFlow.completeStep(currentStep);
        session.setFlow(signinFlow);
        sessionManager.saveSession(session);

        int nextCid = cid + 1;
        String nextTL = transitionLockManager.nextTransLock(
                dsh, tl, cid, currentStep, nextCid, SigninFlow.STEP_AUTHENTICATE, cleanIdentifier
        );

        return FlowResponseVO.builder()
                .dsh(dsh)
                .tl(nextTL)
                .cid(nextCid)
                .nextStep(SigninFlow.STEP_AUTHENTICATE)
                .status("CHALLENGE")
                .identifier(cleanIdentifier)
                .build();
    }

    private void assertWebAuthnEnabled() {
        if (!systemSettingService.getBoolean(SystemSettingKeys.WEBAUTHN_ENABLED, true)) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "通行密钥 (WebAuthn) 认证服务已停用");
        }
    }

    private UserVerificationRequirement resolveUserVerificationRequirement() {
        String uv = systemSettingService.getSettingValue(SystemSettingKeys.WEBAUTHN_USER_VERIFICATION, "preferred");
        if (uv != null) {
            String trimmed = uv.trim().toLowerCase();
            if ("required".equals(trimmed)) {
                return UserVerificationRequirement.REQUIRED;
            }
            if ("discouraged".equals(trimmed)) {
                return UserVerificationRequirement.DISCOURAGED;
            }
        }
        return UserVerificationRequirement.PREFERRED;
    }

    private AuthenticatorSelectionCriteria resolveCriteria(String authenticatorType) {
        if (!StringUtils.hasText(authenticatorType)) {
            return null;
        }
        UserVerificationRequirement uv = resolveUserVerificationRequirement();
        return switch (authenticatorType.trim().toLowerCase()) {
            case "windows_password_manager" -> AuthenticatorSelectionCriteria.builder()
                    .authenticatorAttachment(AuthenticatorAttachment.PLATFORM)
                    .userVerification(uv)
                    .residentKey(ResidentKeyRequirement.REQUIRED)
                    .build();
            case "windows_hello" -> AuthenticatorSelectionCriteria.builder()
                    .authenticatorAttachment(AuthenticatorAttachment.PLATFORM)
                    .userVerification(UserVerificationRequirement.REQUIRED)
                    .residentKey(ResidentKeyRequirement.REQUIRED)
                    .build();
            case "security_key" -> AuthenticatorSelectionCriteria.builder()
                    .authenticatorAttachment(AuthenticatorAttachment.CROSS_PLATFORM)
                    .userVerification(uv)
                    .residentKey(ResidentKeyRequirement.PREFERRED)
                    .build();
            case "mobile_device" -> AuthenticatorSelectionCriteria.builder()
                    .authenticatorAttachment(AuthenticatorAttachment.CROSS_PLATFORM)
                    .userVerification(uv)
                    .residentKey(ResidentKeyRequirement.REQUIRED)
                    .build();
            case "apple_keychain" -> AuthenticatorSelectionCriteria.builder()
                    .authenticatorAttachment(AuthenticatorAttachment.PLATFORM)
                    .userVerification(uv)
                    .residentKey(ResidentKeyRequirement.REQUIRED)
                    .build();
            default -> null;
        };
    }

    private byte[] serialize(Serializable object) {
        if (object == null) {
            return new byte[0];
        }
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(object);
            oos.flush();
            return bos.toByteArray();
        } catch (IOException e) {
            log.error("Failed to serialize object: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to serialize object", e);
        }
    }

    private <T extends Serializable> T deserialize(byte[] bytes, Class<T> clazz) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            ois.setObjectInputFilter(ObjectInputFilter.Config.createFilter(
                    "org.springframework.security.web.webauthn.**;java.base/*;!*"
            ));
            Object obj = ois.readObject();
            if (clazz.isInstance(obj)) {
                return clazz.cast(obj);
            }
            throw new IllegalArgumentException("Deserialized object is not an instance of " + clazz.getName());
        } catch (Exception e) {
            log.error("Failed to deserialize object: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to deserialize object", e);
        }
    }

    @Override
    public boolean hasPasskey(Long userId) {
        if (userId == null) {
            return false;
        }
        if (!systemSettingService.getBoolean(SystemSettingKeys.WEBAUTHN_ENABLED, true)) {
            return false;
        }
        Bytes userBytes = new Bytes(ByteBuffer.allocate(Long.BYTES).putLong(userId).array());
        List<CredentialRecord> records = userCredentialRepository.findByUserId(userBytes);
        return !records.isEmpty();
    }

    @Override
    public List<PasskeyCredentialVO> listCredentials(Long userId) {
        if (userId == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "用户未登录");
        }
        Bytes userBytes = new Bytes(ByteBuffer.allocate(Long.BYTES).putLong(userId).array());
        List<CredentialRecord> records = userCredentialRepository.findByUserId(userBytes);
        return records.stream()
                .map(record -> PasskeyCredentialVO.builder()
                        .credentialId(record.getCredentialId().toBase64UrlString())
                        .label(StringUtils.hasText(record.getLabel()) ? record.getLabel() : "通行密钥")
                        .created(record.getCreated())
                        .lastUsed(record.getLastUsed())
                        .signatureCount(record.getSignatureCount())
                        .authenticatorTransports(formatTransports(record.getTransports()))
                        .build())
                .toList();
    }

    @Override
    public void deleteCredential(Long userId, String credentialId) {
        if (userId == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "用户未登录");
        }
        if (!StringUtils.hasText(credentialId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "通行密钥凭据ID不能为空");
        }
        Bytes userBytes = new Bytes(ByteBuffer.allocate(Long.BYTES).putLong(userId).array());
        List<CredentialRecord> records = userCredentialRepository.findByUserId(userBytes);
        CredentialRecord target = records.stream()
                .filter(r -> r.getCredentialId().toBase64UrlString().equals(credentialId.trim())
                        || r.getCredentialId().toString().equals(credentialId.trim()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "未找到指定的通行密钥凭据或无权操作"));

        userCredentialRepository.delete(target.getCredentialId());
    }

    private String formatTransports(Set<AuthenticatorTransport> transports) {
        if (transports == null || transports.isEmpty()) {
            return null;
        }
        return transports.stream()
                .map(AuthenticatorTransport::getValue)
                .collect(Collectors.joining(","));
    }

}
