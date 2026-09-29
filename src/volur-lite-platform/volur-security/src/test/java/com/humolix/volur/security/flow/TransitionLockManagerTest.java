package com.humolix.volur.security.flow;

import com.alibaba.fastjson2.JSON;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.security.domain.session.AuthFlowSession;
import com.humolix.volur.framework.id.generator.SnowflakeIdGenerator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class TransitionLockManagerTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private TransitionLockManager transitionLockManager;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = Mockito.mock(StringRedisTemplate.class);
        valueOperations = Mockito.mock(ValueOperations.class);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        transitionLockManager = new TransitionLockManager(redisTemplate, new DshGenerator(new SnowflakeIdGenerator(1L)), new TransitionLockGenerator());
    }

    @Test
    void generateDsh_shouldMatchGoogleAccountsFormat() {
        String dsh = transitionLockManager.generateDsh();
        Assertions.assertNotNull(dsh);
        Assertions.assertTrue(dsh.startsWith("S-"));
        Assertions.assertTrue(dsh.contains(":"));

        String prefix = dsh.substring(2, dsh.indexOf(':'));
        long clusterPrefix = Long.parseLong(prefix);
        long workerId = (clusterPrefix - 100_000_000L) / 100_000L;
        Assertions.assertEquals(1L, workerId);
    }

    @Test
    void generateTL_shouldGenerateHmacSignatureToken() {
        String dsh = "S-969965009:1790527266429740";
        String tl = transitionLockManager.generateTL(dsh, 0, "identifier", "admin@ctf.local");
        Assertions.assertNotNull(tl);
        Assertions.assertTrue(tl.startsWith("ADG-"));
        Assertions.assertTrue(tl.length() >= 64);
    }

    @Test
    void createOrGetSession_shouldCreateNewSessionAndSaveRedis() {
        String clientIp = "127.0.0.1";
        String ua = "Mozilla/5.0";

        AuthFlowSession session = transitionLockManager.createOrGetSession(null, clientIp, ua);

        Assertions.assertNotNull(session);
        Assertions.assertNotNull(session.getDsh());
        Assertions.assertEquals("VlifWebLogin", session.getFlowName());
        Assertions.assertEquals("WebServiceLogin", session.getFlowEntry());
        Assertions.assertEquals("init", session.getCurrentStep());
        Assertions.assertEquals(0, session.getCid());

        verify(valueOperations).set(eq("vredis:security:flow:session:" + session.getDsh()), any(), eq(Duration.ofMinutes(15)));
    }

    @Test
    void advanceTransitionTicket_shouldValidateAndRotateToken() {
        String dsh = "S-100200300:1790000000";
        String initialTL = transitionLockManager.generateTL(dsh, 0, "init", null);

        AuthFlowSession session = AuthFlowSession.builder()
                .dsh(dsh)
                .flowName("VlifWebLogin")
                .flowEntry("WebServiceLogin")
                .currentStep("init")
                .cid(0)
                .activeTL(initialTL)
                .build();

        given(valueOperations.get("vredis:security:flow:session:" + dsh)).willReturn(JSON.toJSONString(session));

        String nextTL = transitionLockManager.advanceTransitionTicket(
                dsh, initialTL, 0, "init", 1, "recaptcha", "user@ctf.local"
        );

        Assertions.assertNotNull(nextTL);
        Assertions.assertNotEquals(initialTL, nextTL);
        Assertions.assertTrue(nextTL.startsWith("ADG-"));

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq("vredis:security:flow:session:" + dsh), captor.capture(), eq(Duration.ofMinutes(15)));

        AuthFlowSession saved = JSON.parseObject(captor.getValue(), AuthFlowSession.class);
        Assertions.assertEquals(1, saved.getCid());
        Assertions.assertEquals("recaptcha", saved.getCurrentStep());
        Assertions.assertEquals("user@ctf.local", saved.getIdentifier());
        Assertions.assertEquals(nextTL, saved.getActiveTL());
        Assertions.assertTrue(saved.getCompletedSteps().contains("init"));
    }

    @Test
    void validateTransitionTicket_shouldThrowWhenTamperedOrSkipped() {
        String dsh = "S-100200300:1790000000";
        String activeTL = transitionLockManager.generateTL(dsh, 1, "recaptcha", null);

        AuthFlowSession session = AuthFlowSession.builder()
                .dsh(dsh)
                .currentStep("recaptcha")
                .cid(1)
                .activeTL(activeTL)
                .build();

        given(valueOperations.get("vredis:security:flow:session:" + dsh)).willReturn(JSON.toJSONString(session));

        Assertions.assertThrows(BusinessException.class, () ->
                transitionLockManager.validateTransitionTicket(dsh, "ADG-OLD_EXPIRED_TOKEN_AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", 1, "recaptcha")
        );

        Assertions.assertThrows(BusinessException.class, () ->
                transitionLockManager.validateTransitionTicket(dsh, activeTL, 1, "pwd")
        );

        Assertions.assertThrows(BusinessException.class, () ->
                transitionLockManager.validateTransitionTicket(dsh, activeTL, 2, "recaptcha")
        );
    }

}
