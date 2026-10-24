package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 拼团活动
 * <p>
 * 一次「开团」对应一条记录，团长开团后 currentSize=1，
 * 其他用户参团则 currentSize 递增，达到 requiredSize 即成团。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Groupon implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 拼团状态：1 拼团中
     */
    public static final Integer IN_PROGRESS = 1;
    /**
     * 拼团状态：2 已成团
     */
    public static final Integer SUCCESS = 2;
    /**
     * 拼团状态：3 已失败（人满前被取消）
     */
    public static final Integer FAILED = 3;
    /**
     * 拼团状态：4 已过期（超时未成团）
     */
    public static final Integer EXPIRED = 4;

    private Long id;

    // Response-only: order belonging to the current participant; never persisted on the group.
    private Long orderId;

    //拼团商品id（dish.id）
    private Long dishId;

    //团长用户id
    private Long leaderUserId;

    //拼团价快照，避免商品改价影响已开团活动
    private BigDecimal grouponPrice;

    //成团所需人数
    private Integer requiredSize;

    //当前已参团人数（含团长）
    private Integer currentSize;

    //拼团状态 1拼团中 2已成团 3已失败 4已过期
    private Integer status;

    //拼团截止时间
    private LocalDateTime expireTime;

    //成团时间
    private LocalDateTime completeTime;

    //自提点id
    private Long pickupPointId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
