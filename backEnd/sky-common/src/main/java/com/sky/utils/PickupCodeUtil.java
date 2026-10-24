package com.sky.utils;

import java.security.SecureRandom;

/**
 * 提货核销码生成器（m21 自提改造）
 *
 * <p><b>为什么是 8 位纯数字</b>：到店场景下提货人要「念得出来、记得住」。
 * 字母数字混合（如 {@code 8K2M9XQ4}）在口头报号时极易混淆
 * （0/O、1/I/l、2/Z、5/S），实际会显著拖慢核销。纯数字 8 位 = 1 亿空间，
 * 配合「支付后生成 + UNIQUE 索引 + 查库防重」三重保证，碰撞概率可忽略。
 *
 * <p><b>唯一性怎么保证</b>：本类只负责「生成一个格式合法的候选值」，
 * <b>不</b>保证全局唯一——唯一性由调用方（OrderServiceImpl#generateUniquePickupCode）
 * 结合 {@code orders.pickup_code} 唯一索引做「生成 → 查库是否存在 → 冲突则重试」闭环。
 * 刻意如此分工：把唯一性判断放在真正持有数据库连接的服务层，工具类保持纯函数、
 * 易测试、无 DB 依赖。
 *
 * <p><b>为什么用 SecureRandom 而不是 Random</b>：提货码本质是可换取实物的凭证。
 * 若用 {@link java.util.Random}（线性同余，种子可预测），攻击者拿到若干张码
 * 就能推算出别人的码，进而到店冒领。SecureRandom 无序可预测性。
 *
 * <p>本类无状态、线程安全（SecureRandom 实例本身线程安全）。
 */
public final class PickupCodeUtil {

    /** 提货码位数：8 位，与产品约定一致（orders.pickup_code VARCHAR(16) 预留空间） */
    public static final int CODE_LENGTH = 8;

    /**
     * 生成上限：10^8 = 100000000。
     * 首位允许 0（补齐为 8 位字符串），故取值区间是 [0, 100000000)。
     */
    private static final int BOUND = 100_000_000;

    private static final SecureRandom RANDOM = new SecureRandom();

    private PickupCodeUtil() {
    }

    /**
     * 生成一个 8 位数字提货码（左侧补 0 保证长度恒定）。
     *
     * <p>示例：{@code "68291034"}、{@code "00048192"}
     *
     * <p><b>注意</b>：本方法不查库，理论上可能返回已存在的码；
     * 需要唯一性时请用 OrderServiceImpl 里的带重试版本。
     *
     * @return 恒为 8 位数字的字符串
     */
    public static String generate() {
        int n = RANDOM.nextInt(BOUND);
        // 左补 0 到 8 位：低位随机值如 48192 会被补成 "00048192"
        return String.format("%08d", n);
    }

    /**
     * 校验一个字符串是否是格式合法的提货码。
     *
     * <p>用于核销接口的入参前置校验，避免把明显非法的值（空、带空格、长度不对、
     * 含非数字字符）打到数据库。
     *
     * @param code 待校验值，可为 null
     * @return true 表示是 8 位纯数字
     */
    public static boolean isValid(String code) {
        if (code == null) {
            return false;
        }
        // 严格 8 位数字：不用 \d（它在某些正则实现下会匹配全角数字）
        return code.matches("[0-9]{8}");
    }
}
