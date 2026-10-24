package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 拼团成员：记录每个参团用户及其关联订单
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrouponMember implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //拼团活动id
    private Long grouponId;

    //参团用户id
    private Long userId;

    //该成员关联的订单id
    private Long orderId;

    //是否团长 0否 1是
    private Integer isLeader;

    //购买数量
    private Integer number;

    //支付金额
    private BigDecimal amount;

    private LocalDateTime createTime;
}
