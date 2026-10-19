package com.humolix.volur.security.service;

import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.security.service.impl.FlowCryptoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlowCryptoServiceTest {

    private FlowCryptoService flowCryptoService;

    @BeforeEach
    void setUp() {
        flowCryptoService = new FlowCryptoServiceImpl();
    }

    @Test
    void shouldEncryptAndDecryptSuccessfully() {
        String plain = "HuMoLixaasdff12!@#";
        String flowName = "VlifWebLogin";
        String flowEntry = "WebServiceLogin";
        String dsh = "S-100000000:1790902374919000";

        String envelope = flowCryptoService.encrypt(plain, flowName, flowEntry, dsh);
        assertThat(envelope).startsWith("$V1$GCM$");

        String decrypted = flowCryptoService.decrypt(envelope, flowName, flowEntry, dsh);
        assertThat(decrypted).isEqualTo(plain);
    }

    @Test
    void shouldFailWhenDshMismatch() {
        String plain = "test_password";
        String envelope = flowCryptoService.encrypt(plain, "VlifWebLogin", "WebServiceLogin", "dsh_1");

        assertThatThrownBy(() -> flowCryptoService.decrypt(envelope, "VlifWebLogin", "WebServiceLogin", "dsh_2"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldFailWhenExpired() {
        long expiredTimestamp = System.currentTimeMillis() - 400_000L;
        String fakeEnvelope = "$V1$GCM$" + expiredTimestamp + "$AAAA";

        assertThatThrownBy(() -> flowCryptoService.decrypt(fakeEnvelope, "VlifWebLogin", "WebServiceLogin", "dsh"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldFailWhenNotEnvelopeFormat() {
        assertThatThrownBy(() -> flowCryptoService.decrypt("plain_password", "VlifWebLogin", "WebServiceLogin", "dsh"))
                .isInstanceOf(BusinessException.class);
    }
}
