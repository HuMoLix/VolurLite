package com.humolix.volur.security.session;

import tools.jackson.databind.ObjectMapper;
import com.humolix.volur.framework.common.exception.BusinessException;
import com.humolix.volur.security.domain.session.RequestDeviceParams;
import com.humolix.volur.security.domain.session.RequestHeader;
import com.humolix.volur.security.session.generator.DeviceSessionHashGenerator;
import com.humolix.volur.security.flow.domain.FlowNames;
import com.humolix.volur.security.flow.domain.Flow;
import com.humolix.volur.security.session.domain.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionManager {

    private static final String VREDIS_SECURITY_FLOW_SESSION_KEY_PREFIX = "vredis:security:flow:session:";
    private static final Duration FLOW_SESSION_TTL = Duration.ofMinutes(15);

    private final StringRedisTemplate redisTemplate;
    private final DeviceSessionHashGenerator deviceSessionHashGenerator;
    private final ObjectMapper objectMapper;

    public String generateDsh() {
        return deviceSessionHashGenerator.generate();
    }

    public Session createSession(String flowName, String flowEntry, RequestHeader requestHeader, RequestDeviceParams requestDeviceParams) {
        FlowNames validatedFlow = FlowNames.validate(flowName, flowEntry);
        String deviceSessionHash = generateDsh();
        Flow flow = validatedFlow == FlowNames.VLIF_WEB_LOGIN
                ? com.humolix.volur.security.flow.flows.SigninFlow.create()
                : Flow.builder()
                .flowName(validatedFlow.getFlowName())
                .flowEntry(validatedFlow.findEntry(flowEntry).getEntry())
                .currentStep("init")
                .cid(0)
                .completedSteps(new ArrayList<>())
                .waitingSteps(new ArrayList<>())
                .build();

        Session session = Session.builder()
                .dsh(deviceSessionHash)
                .flow(flow)
                .requestHeader(requestHeader)
                .requestDeviceParams(requestDeviceParams)
                .metadata(new HashMap<>())
                .createdAt(System.currentTimeMillis())
                .updatedAt(System.currentTimeMillis())
                .build();

        saveSession(session);
        return session;
    }

    public Session recoverySession(String dsh, String flowName, String flowEntry, RequestHeader requestHeader, RequestDeviceParams requestDeviceParams) {
        if (!StringUtils.hasText(dsh)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "登录或业务流程会话句柄 (dsh) 不能为空");
        }
        Session existing = getSession(dsh);
        Flow flow = existing.getFlow();
        if (flow == null) {
            FlowNames targetFlow = FlowNames.VLIF_WEB_LOGIN;
            String targetEntry = FlowNames.FlowEntry.WEB_SERVICE_LOGIN.getEntry();
            if (StringUtils.hasText(flowName) || StringUtils.hasText(flowEntry)) {
                targetFlow = FlowNames.validate(
                        StringUtils.hasText(flowName) ? flowName : targetFlow.getFlowName(),
                        StringUtils.hasText(flowEntry) ? flowEntry : targetEntry
                );
                targetEntry = targetFlow.findEntry(StringUtils.hasText(flowEntry) ? flowEntry : targetEntry).getEntry();
            }
            flow = targetFlow == FlowNames.VLIF_WEB_LOGIN
                    ? com.humolix.volur.security.flow.flows.SigninFlow.create()
                    : Flow.builder()
                    .flowName(targetFlow.getFlowName())
                    .flowEntry(targetEntry)
                    .currentStep("init")
                    .cid(0)
                    .completedSteps(new ArrayList<>())
                    .waitingSteps(new ArrayList<>())
                    .build();
            existing.setFlow(flow);
        }

        boolean modified = false;
        if (StringUtils.hasText(flowName) || StringUtils.hasText(flowEntry)) {
            String checkFlowName = StringUtils.hasText(flowName) ? flowName : flow.getFlowName();
            String checkFlowEntry = StringUtils.hasText(flowEntry) ? flowEntry : flow.getFlowEntry();
            FlowNames validatedFlow = FlowNames.validate(checkFlowName, checkFlowEntry);
            String canonicalFlowName = validatedFlow.getFlowName();
            String canonicalFlowEntry = validatedFlow.findEntry(checkFlowEntry).getEntry();

            if (!Objects.equals(flow.getFlowName(), canonicalFlowName)) {
                flow.setFlowName(canonicalFlowName);
                modified = true;
            }
            if (!Objects.equals(flow.getFlowEntry(), canonicalFlowEntry)) {
                flow.setFlowEntry(canonicalFlowEntry);
                modified = true;
            }
        }
        if (requestHeader != null && !Objects.equals(existing.getRequestHeader(), requestHeader)) {
            existing.setRequestHeader(requestHeader);
            modified = true;
        }
        if (requestDeviceParams != null && !Objects.equals(existing.getRequestDeviceParams(), requestDeviceParams)) {
            existing.setRequestDeviceParams(requestDeviceParams);
            modified = true;
        }
        if (modified) {
            existing.setUpdatedAt(System.currentTimeMillis());
            saveSession(existing);
        }
        return existing;
    }

    public Session getSession(String dsh) {
        Session session = getSessionOrNull(dsh);
        if (session == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "登录或业务流程会话句柄 (dsh) 已失效或不存在，请重新开始");
        }
        return session;
    }

    public Session getSessionOrNull(String dsh) {
        if (!StringUtils.hasText(dsh)) {
            return null;
        }
        String key = VREDIS_SECURITY_FLOW_SESSION_KEY_PREFIX + dsh.trim();
        String json = redisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Session.class);
        } catch (Exception e) {
            log.error("[SessionManager] 解析会话 JSON 失败, dsh={}", dsh, e);
            return null;
        }
    }

    public void saveSession(Session session) {
        if (session == null || !StringUtils.hasText(session.getDsh())) {
            return;
        }
        session.setUpdatedAt(System.currentTimeMillis());
        String key = VREDIS_SECURITY_FLOW_SESSION_KEY_PREFIX + session.getDsh().trim();
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(session), FLOW_SESSION_TTL);
        } catch (Exception e) {
            log.error("[SessionManager] 序列化会话 JSON 失败, dsh={}", session.getDsh(), e);
        }
    }

    public void setMetadata(String dsh, String key, Object value) {
        Session session = getSession(dsh);
        if (session.getMetadata() == null) {
            session.setMetadata(new HashMap<>());
        }
        session.getMetadata().put(key, value);
        saveSession(session);
    }

    public Object getMetadata(String dsh, String key) {
        Session session = getSessionOrNull(dsh);
        if (session == null || session.getMetadata() == null) {
            return null;
        }
        return session.getMetadata().get(key);
    }

    public void removeMetadata(String dsh, String key) {
        Session session = getSessionOrNull(dsh);
        if (session != null && session.getMetadata() != null) {
            session.getMetadata().remove(key);
            saveSession(session);
        }
    }

    public void destroySession(String dsh) {
        if (StringUtils.hasText(dsh)) {
            String key = VREDIS_SECURITY_FLOW_SESSION_KEY_PREFIX + dsh.trim();
            redisTemplate.delete(key);
        }
    }

}
