package com.humolix.volur.security.webauthn;

import com.humolix.volur.user.domain.entity.User;
import com.humolix.volur.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class UserPublicKeyCredentialUserEntityRepository implements PublicKeyCredentialUserEntityRepository {

    private final UserRepository userRepository;

    @Override
    public PublicKeyCredentialUserEntity findById(Bytes id) {
        if (id == null) {
            return null;
        }
        try {
            byte[] bytes = id.getBytes();
            if (bytes.length == Long.BYTES) {
                Long userId = ByteBuffer.wrap(bytes).getLong();
                return userRepository.findById(userId).map(this::toUserEntity).orElse(null);
            }
            String idStr = new String(bytes, StandardCharsets.UTF_8);
            Long userId = Long.parseLong(idStr);
            return userRepository.findById(userId).map(this::toUserEntity).orElse(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    @Override
    public PublicKeyCredentialUserEntity findByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }
        return userRepository.findByUsername(username.trim())
                .map(this::toUserEntity)
                .orElse(null);
    }

    @Override
    public void save(PublicKeyCredentialUserEntity userEntity) {
    }

    @Override
    public void delete(Bytes id) {
    }

    private PublicKeyCredentialUserEntity toUserEntity(User user) {
        Bytes id = new Bytes(ByteBuffer.allocate(Long.BYTES).putLong(user.getId()).array());
        return ImmutablePublicKeyCredentialUserEntity.builder()
                .id(id)
                .name(user.getUsername())
                .displayName(StringUtils.hasText(user.getUsername()) ? user.getUsername() : "Volur User")
                .build();
    }

}
