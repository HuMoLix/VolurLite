package com.humolix.volur.framework.cache;

import java.util.List;

/**
 * 全局缓存名注册表。
 *
 * <p>规范：<b>所有缓存名必须在此声明，禁止在业务代码中硬编码字符串</b>。原因有三：
 * <ol>
 *   <li>{@link VolurCacheManager} 需要一份完整清单在启动时预创建缓存实例，
 *       否则懒创建会导致 Micrometer 指标注册不全、Prometheus 查不到数据；</li>
 *   <li>广播失效与后台清理需要能枚举全部缓存；</li>
 *   <li>缓存名是跨模块的公共契约，散落的字符串拼写错误只能在运行时暴露。</li>
 * </ol>
 *
 * <p><b>命名规范</b>：小写 + 下划线，使用"领域_对象"形式，例如 {@code user_details}、
 * {@code competition_status}。
 */
public final class CacheNames {

    private CacheNames() {
    }

    /**
     * 认证用户详情。
     *
     * <p>全站最热路径：{@code JwtAuthFilter} 每个已认证请求都会读取。变更极少（改密码、改角色、
     * 禁用账号），允许秒级脏读。L1 + L2 双开。
     */
    public static final String USER_DETAILS = "user_details";

    /**
     * 系统设置。
     *
     * <p>读极频、写极少（仅管理端改配置）。L1 长 TTL，变更时走广播失效。
     */
    public static final String SYSTEM_SETTING = "system_setting";

    /**
     * 赛题静态详情。
     *
     * <p>读多写少。回源较慢（多表关联），是"值得上分布式锁"的候选缓存。
     * 仅走 L2，避免 L1 脏读导致赛题可见性判断被绕过。
     */
    public static final String CTF_CHALLENGE_DETAIL = "ctf_challenge_detail";

    /**
     * 枚举字典（赛制、难度、分类等）。
     *
     * <p>几乎不变，适合 L1 长 TTL（小时级）。
     */
    public static final String DICT = "dict";

    /**
     * 竞赛状态。
     *
     * <p><b>强一致要求，必须禁用 L1。</b>该值参与 {@code @RequiredCompetitionStatus} 切面的
     * 状态校验：若 L1 脏读导致已结束的比赛仍返回 RUNNING，用户可继续提交 Flag。
     * 只允许 L2，且 TTL 极短（5-10 秒）。
     */
    public static final String COMPETITION_STATUS = "competition_status";

    /**
     * 启动时预创建的缓存清单。
     *
     * <p>新增缓存时必须同步登记到此列表。
     */
    public static final List<String> ALL = List.of(
            USER_DETAILS,
            SYSTEM_SETTING,
            CTF_CHALLENGE_DETAIL,
            DICT,
            COMPETITION_STATUS
    );
}
