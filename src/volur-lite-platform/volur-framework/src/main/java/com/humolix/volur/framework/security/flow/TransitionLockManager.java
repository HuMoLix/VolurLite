package com.humolix.volur.framework.security.flow;

import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson2.JSON;
import com.humolix.volur.framework.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Objects;

/**
 * Google Accounts 规范通用分布式状态机句柄 (dsh) 与流转锁 (TL) 核心管理器
 * <p>
 * 通用于登录 (SignIn)、注册 (SignUp)、密码找回 (Recovery) 等全生命周期阶段化流转：
 * 1. 负责生成、维护和核销集群唯一分布式会话句柄 (Distributed State Handle / dsh)；
 * 2. 负责基于 HMAC 签名和短期事务锁签发、校验与销毁流转锁票据 (Transition Lock / TL)；
 * 3. 严格防范跃迁跳步（Step Skipping）与重放攻击（Replay Attack）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransitionLockManager {

    private static final String VREDIS_SECURITY_FLOW_SESSION_KEY_PREFIX = "vredis:security:flow:session:";
    private static final Duration FLOW_SESSION_TTL = Duration.ofMinutes(15);
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final byte[] HMAC_SECRET_BYTES = "VolurLiteGoogleAccountsSecretHmac2026!#$".getBytes(StandardCharsets.UTF_8);

    private final StringRedisTemplate redisTemplate;

    /**
     * 生成对齐 Google Accounts 规范的分布式会话句柄 (Distributed State Handle)
     * 格式: S-<9位集群实例号>:<微秒级时间戳与高熵随机数>
     * 示例: S-969965009:1790527266429740
     */
    public String generateDsh() {
        long clusterId = 100000000L + RandomUtil.randomLong(900000000L);
        long timestampMicro = System.currentTimeMillis() * 1000L + RandomUtil.randomLong(1000L);
        return "S-" + clusterId + ":" + timestampMicro;
    }

    /**
     * 构造具有防伪签名与防跳步防重放特性的流转锁票据 (Transition Lock / TL)
     * 格式: ADG-<60位高熵签名哈希>
     * 示例: ADG-GRTbPbNl0pSndZCtatZwpc273Y1KxgoCFYDnvVdrHspzQ9BKk03FfMrdB24l
     */
    public String generateTL(String dsh, int cid, String step, String identifier) {
        try {
            String rawPayload = (dsh != null ? dsh : "") + "|" + cid + "|" + (step != null ? step : "")
                    + "|" + (identifier != null ? identifier : "") + "|" + System.currentTimeMillis()
                    + "|" + RandomUtil.randomString(16);

            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(HMAC_SECRET_BYTES, HMAC_ALGORITHM));
            byte[] hmacBytes = mac.doFinal(rawPayload.getBytes(StandardCharsets.UTF_8));
            String base64Url = Base64.getUrlEncoder().withoutPadding().encodeToString(hmacBytes);

            // 补齐或截取为 60 字符的高熵哈希串 (Google ADG- 标准规格)
            String randomExtra = RandomUtil.randomString("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_-", 32);
            String combined = base64Url + randomExtra;
            String tokenContent = combined.length() >= 60 ? combined.substring(0, 60) : combined + RandomUtil.randomString(60 - combined.length());
            return "ADG-" + tokenContent;
        } catch (Exception e) {
            return "ADG-" + RandomUtil.randomString("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_-", 60);
        }
    }

    /**
     * 创建或获取指定流（登录、注册、密码找回等）的会话
     *
     * @param dsh        已有 dsh 句柄（可选）
     * @param flowName   流程名称 (如 VlifWebLogin, VlifWebSignUp, VlifWebRecovery)
     * @param flowEntry  入口标识 (如 WebServiceLogin, WebSignUp)
     * @param clientIp   客户端 IP
     * @param userAgent  客户端 UA
     * @return 活跃会话
     */
    public AuthFlowSession createOrGetSession(String dsh, String flowName, String flowEntry, String clientIp, String userAgent) {
        if (StringUtils.hasText(dsh)) {
            AuthFlowSession existing = getSessionOrNull(dsh);
            if (existing != null) {
                boolean modified = false;
                if (StringUtils.hasText(flowName) && !Objects.equals(existing.getFlowName(), flowName)) {
                    existing.setFlowName(flowName);
                    modified = true;
                }
                if (StringUtils.hasText(flowEntry) && !Objects.equals(existing.getFlowEntry(), flowEntry)) {
                    existing.setFlowEntry(flowEntry);
                    modified = true;
                }
                if (modified) {
                    saveSession(existing);
                }
                return existing;
            }
        }

        String newDsh = generateDsh();
        AuthFlowSession session = AuthFlowSession.builder()
                .dsh(newDsh)
                .flowName(StringUtils.hasText(flowName) ? flowName : "VlifWebLogin")
                .flowEntry(StringUtils.hasText(flowEntry) ? flowEntry : "WebServiceLogin")
                .currentStep("init")
                .cid(0)
                .clientIp(clientIp)
                .userAgent(userAgent)
                .createdAt(System.currentTimeMillis())
                .updatedAt(System.currentTimeMillis())
                .build();

        saveSession(session);
        return session;
    }

    /**
     * 默认重载：创建或获取登录流程会话 (SignIn)
     */
    public AuthFlowSession createOrGetSession(String dsh, String clientIp, String userAgent) {
        return createOrGetSession(dsh, "VlifWebLogin", "WebServiceLogin", clientIp, userAgent);
    }

    /**
     * 查询指定 dsh 的活跃会话；不存在或过期时抛出统一业务异常
     */
    public AuthFlowSession getSession(String dsh) {
        AuthFlowSession session = getSessionOrNull(dsh);
        if (session == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "登录或业务流程会话句柄 (dsh) 已失效或不存在，请重新开始");
        }
        return session;
    }

    /**
     * 查询指定 dsh 的活跃会话，不存在时安全返回 null
     */
    public AuthFlowSession getSessionOrNull(String dsh) {
        if (!StringUtils.hasText(dsh)) {
            return null;
        }
        String key = VREDIS_SECURITY_FLOW_SESSION_KEY_PREFIX + dsh.trim();
        String json = redisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return JSON.parseObject(json, AuthFlowSession.class);
        } catch (Exception e) {
            log.error("[TransitionLockManager] 解析会话 JSON 失败, dsh={}", dsh, e);
            return null;
        }
    }

    /**
     * 持久化/刷新活跃会话状态并重设 TTL
     */
    public void saveSession(AuthFlowSession session) {
        if (session == null || !StringUtils.hasText(session.getDsh())) {
            return;
        }
        session.setUpdatedAt(System.currentTimeMillis());
        String key = VREDIS_SECURITY_FLOW_SESSION_KEY_PREFIX + session.getDsh().trim();
        redisTemplate.opsForValue().set(key, JSON.toJSONString(session), FLOW_SESSION_TTL);
    }

    /**
     * 严格核验 TL 票据、步骤与序号（防跳步 Step Skipping 与重放攻击 Replay Attack）
     */
    public void validateTransitionTicket(String dsh, String tl, int cid, String expectedStep) {
        AuthFlowSession session = getSession(dsh);

        if (!StringUtils.hasText(tl) || !tl.startsWith("ADG-")) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "认证流转票据 (TL) 缺失或格式非法");
        }

        if (!Objects.equals(session.getActiveTL(), tl)) {
            log.warn("[TransitionLockManager] TL 票据不匹配或已被核销: dsh={}, reqTL={}, activeTL={}", dsh, tl, session.getActiveTL());
            throw new BusinessException(HttpStatus.FORBIDDEN, "流转票据 (TL) 无效或已被核销，防止重放攻击");
        }

        if (!Objects.equals(session.getCurrentStep(), expectedStep)) {
            log.warn("[TransitionLockManager] 检测到跃迁跳步: dsh={}, expectedStep={}, sessionStep={}", dsh, expectedStep, session.getCurrentStep());
            throw new BusinessException(HttpStatus.FORBIDDEN, "检测到非法越级跳转 (Step Skipping)，请按顺序完成验证");
        }

        if (!Objects.equals(session.getCid(), cid)) {
            log.warn("[TransitionLockManager] cid 序号不匹配: dsh={}, reqCid={}, sessionCid={}", dsh, cid, session.getCid());
            throw new BusinessException(HttpStatus.FORBIDDEN, "认证步骤序号 (cid) 不一致，流程已被重置");
        }
    }

    /**
     * 状态机流转推进：核销当前旧 TL，推进 cid，签发下一阶段合法的 TL
     *
     * @param dsh           分布式会话句柄
     * @param currentTl     当前步骤持有的 TL (初次进入时可为空)
     * @param currentCid    当前步骤序号 cid
     * @param completedStep 刚刚通过的阶段
     * @param nextCid       下一个阶段序号 cid
     * @param nextStep      下一个阶段类型 (如 recaptcha, pwd, 2fa, signup_verify 等)
     * @param identifier    账号标识
     * @return 签发的下一阶段流转票据 TL
     */
    public String advanceTransitionTicket(String dsh, String currentTl, int currentCid, String completedStep, int nextCid, String nextStep, String identifier) {
        AuthFlowSession session = getSession(dsh);

        // 若存在前序 TL，先核验
        if (StringUtils.hasText(currentTl)) {
            validateTransitionTicket(dsh, currentTl, currentCid, completedStep);
            if (session.getCompletedSteps() == null) {
                session.setCompletedSteps(new ArrayList<>());
            }
            session.getCompletedSteps().add(completedStep);
        }

        // 销毁旧 TL，签发新 TL
        String nextTL = generateTL(dsh, nextCid, nextStep, identifier);
        session.setActiveTL(nextTL);
        session.setCid(nextCid);
        session.setCurrentStep(nextStep);
        if (StringUtils.hasText(identifier)) {
            session.setIdentifier(identifier);
        }
        saveSession(session);

        log.info("[TransitionLockManager] 推进状态机步骤: flow={}, dsh={}, step: {} -> {}, cid: {} -> {}",
                session.getFlowName(), dsh, completedStep, nextStep, currentCid, nextCid);
        return nextTL;
    }

    /**
     * 记录或更新会话上下文元数据 (如用于 SignUp 临时注册资料保存等)
     */
    public void setMetadata(String dsh, String key, Object value) {
        AuthFlowSession session = getSession(dsh);
        if (session.getMetadata() == null) {
            session.setMetadata(new HashMap<>());
        }
        session.getMetadata().put(key, value);
        saveSession(session);
    }

    /**
     * 读取会话上下文元数据
     */
    public Object getMetadata(String dsh, String key) {
        AuthFlowSession session = getSessionOrNull(dsh);
        if (session == null || session.getMetadata() == null) {
            return null;
        }
        return session.getMetadata().get(key);
    }

    /**
     * 完成全部认证或注册后，核验并彻底销毁会话与 TL，防止二次重放
     */
    public void burnAndCompleteSession(String dsh, String currentTl, int currentCid, String expectedStep) {
        validateTransitionTicket(dsh, currentTl, currentCid, expectedStep);
        destroySession(dsh);
        log.info("[TransitionLockManager] 流程认证完成，已销毁会话与事务锁: dsh={}", dsh);
    }

    /**
     * 销毁指定 dsh 会话
     */
    public void destroySession(String dsh) {
        if (StringUtils.hasText(dsh)) {
            String key = VREDIS_SECURITY_FLOW_SESSION_KEY_PREFIX + dsh.trim();
            redisTemplate.delete(key);
        }
    }
}
