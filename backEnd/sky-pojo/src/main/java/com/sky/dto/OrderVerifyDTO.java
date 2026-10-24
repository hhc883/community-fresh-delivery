package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 团长/自提点端核销入参
 *
 * <p>契约：POST /admin/order/verify  body {"pickupCode":"68291034"}
 *
 * <p>为什么用 pickupCode 而不是 orderId 做核销键：
 * 到店场景下提货人手里只有提货码（6~8 位数字），念号/扫码即可，
 * 让他报订单 id 既不方便也会暴露自增 id 规模。故以 pickupCode 为唯一入口。
 *
 * <p>可选 pickupPointId：多自提点场景下防止 A 点把 B 点的货核销掉。
 * 不传则不校验归属（单自提点/管理员兜底场景）。
 */
@Data
public class OrderVerifyDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 提货核销码（必填，8 位数字） */
    private String pickupCode;

    /** 操作人所在自提点 id（可选：传了则校验订单归属，防止跨点核销） */
    private Long pickupPointId;
}
