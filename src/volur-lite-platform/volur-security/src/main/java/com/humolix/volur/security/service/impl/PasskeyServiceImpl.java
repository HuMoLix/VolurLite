package com.humolix.volur.security.service.impl;

import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.security.domain.dto.PasskeyRegisterRequestDTO;
import com.humolix.volur.security.domain.session.AuthFlowSession;
import com.humolix.volur.security.domain.vo.PasskeyAuthenticatorOptionVO;
import com.humolix.volur.security.domain.vo.PasskeyRegisterResponseVO;
import com.humolix.volur.security.flow.TransitionLockManager;
import com.humolix.volur.security.service.PasskeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.webauthn.api.AuthenticatorAttachment;
import org.springframework.security.web.webauthn.api.AuthenticatorAttestationResponse;
import org.springframework.security.web.webauthn.api.AuthenticatorSelectionCriteria;
import org.springframework.security.web.webauthn.api.CredentialRecord;
import org.springframework.security.web.webauthn.api.PublicKeyCredential;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions;
import org.springframework.security.web.webauthn.api.ResidentKeyRequirement;
import org.springframework.security.web.webauthn.api.UserVerificationRequirement;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.springframework.security.web.webauthn.management.ImmutablePublicKeyCredentialCreationOptionsRequest;
import org.springframework.security.web.webauthn.management.ImmutableRelyingPartyRegistrationRequest;
import org.springframework.security.web.webauthn.management.RelyingPartyPublicKey;
import org.springframework.security.web.webauthn.management.RelyingPartyRegistrationRequest;
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
import java.time.Duration;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasskeyServiceImpl implements PasskeyService {

    private final TransitionLockManager transitionLockManager;
    private final WebAuthnRelyingPartyOperations rpOperations;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String VREDIS_SECURITY_PASSKEY_CREATION_KEY_PREFIX = "vredis:security:passkey:creation:";
    private static final Duration PASSKEY_CREATION_TTL = Duration.ofMinutes(5);

    @Override
    public List<PasskeyAuthenticatorOptionVO> getSupportedAuthenticators() {
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
        transitionLockManager.validateStepAndTl(dsh, tl, "pt/options");
        AuthFlowSession session = transitionLockManager.getSession(dsh);

        String username = (String) transitionLockManager.getMetadata(dsh, "username");
        if (!StringUtils.hasText(username)) {
            username = session.getIdentifier();
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
        transitionLockManager.validateStepAndTl(dsh, tl, "pt/options");

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
        transitionLockManager.setMetadata(dsh, "passkeyRegistered", true);

        return PasskeyRegisterResponseVO.builder()
                .success(true)
                .credentialId(record.getCredentialId().toBase64UrlString())
                .build();
    }

    private AuthenticatorSelectionCriteria resolveCriteria(String authenticatorType) {
        if (!StringUtils.hasText(authenticatorType)) {
            return null;
        }
        return switch (authenticatorType.trim().toLowerCase()) {
            case "windows_password_manager" -> AuthenticatorSelectionCriteria.builder()
                    .authenticatorAttachment(AuthenticatorAttachment.PLATFORM)
                    .userVerification(UserVerificationRequirement.PREFERRED)
                    .residentKey(ResidentKeyRequirement.REQUIRED)
                    .build();
            case "windows_hello" -> AuthenticatorSelectionCriteria.builder()
                    .authenticatorAttachment(AuthenticatorAttachment.PLATFORM)
                    .userVerification(UserVerificationRequirement.REQUIRED)
                    .residentKey(ResidentKeyRequirement.REQUIRED)
                    .build();
            case "security_key" -> AuthenticatorSelectionCriteria.builder()
                    .authenticatorAttachment(AuthenticatorAttachment.CROSS_PLATFORM)
                    .userVerification(UserVerificationRequirement.PREFERRED)
                    .residentKey(ResidentKeyRequirement.PREFERRED)
                    .build();
            case "mobile_device" -> AuthenticatorSelectionCriteria.builder()
                    .authenticatorAttachment(AuthenticatorAttachment.CROSS_PLATFORM)
                    .userVerification(UserVerificationRequirement.PREFERRED)
                    .residentKey(ResidentKeyRequirement.REQUIRED)
                    .build();
            case "apple_keychain" -> AuthenticatorSelectionCriteria.builder()
                    .authenticatorAttachment(AuthenticatorAttachment.PLATFORM)
                    .userVerification(UserVerificationRequirement.PREFERRED)
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

}
