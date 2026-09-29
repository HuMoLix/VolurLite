package com.humolix.volur.framework.security.flow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Google Accounts 规范分布式通用流程状态机模型 (Auth Flow Session)
 * 通用承载登录 (SignIn)、注册 (SignUp)、密码找回与重置 (Recovery) 等全生命周期阶段化流转状态。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthFlowSession implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 分布式会话状态句柄 (Distributed State Handle), 格式如: S-969965009:1790527266429740
     */
    private String dsh;

    /**
     * 业务状态机大模块流名称 (VLIF - Volur Login Interface Framework), 如: VlifWebLogin, VlifWebSignUp, VlifWebRecovery
     */
    @Builder.Default
    private String flowName = "VlifWebLogin";

    /**
     * 大模块下的具体功能入口标识, 如: WebServiceLogin, WebSignUp, WebPasswordReset
     */
    @Builder.Default
    private String flowEntry = "WebServiceLogin";

    /**
     * 当前业务绑定的账号标识 (邮箱或用户名，若已输入或已识别)
     */
    private String identifier;

    /**
     * 当前激活且合法的阶段状态 (如 init, identifier, recaptcha, pwd, 2fa, device, signup_profile, completed 等)
     */
    @Builder.Default
    private String currentStep = "init";

    /**
     * 挑战步骤递增序号 cid (Client Challenge Sequence ID): 0, 1, 2, 3...
     */
    @Builder.Default
    private Integer cid = 0;

    /**
     * 当前步骤唯一合法的状态流转事务锁 (Transition Lock / TL), 格式如: ADG-xxxxxx
     * 一旦流转或核销即刻作废，防止跳步 (Step Skipping) 与重放攻击 (Replay Attack)
     */
    private String activeTL;

    /**
     * 当前会话的风控评级: LOW, MEDIUM, HIGH
     */
    @Builder.Default
    private String riskLevel = "LOW";

    /**
     * 匹配到的系统用户主键 ID (若已识别对应用户)
     */
    private Long userId;

    /**
     * 本会话已按序验证通关的历史挑战阶段记录
     */
    @Builder.Default
    private List<String> completedSteps = new ArrayList<>();

    /**
     * 扩展流程上下文元数据 (用于存储注册草稿、临时校验参数、多步表单中间态等)
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * 会话创建与最近状态流转更新时间戳 (毫秒)
     */
    private Long createdAt;
    private Long updatedAt;

    /**
     * 客户端特征与网络审计上下文
     */
    private String clientIp;
    private String userAgent;

}
