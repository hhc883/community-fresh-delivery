package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Orders implements Serializable {

    /**
     * 订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
     */
    public static final Integer PENDING_PAYMENT = 1;
    public static final Integer TO_BE_CONFIRMED = 2;
    public static final Integer CONFIRMED = 3;
    public static final Integer DELIVERY_IN_PROGRESS = 4;
    public static final Integer COMPLETED = 5;
    public static final Integer CANCELLED = 6;

    /**
     * 支付状态 0未支付 1已支付 2退款
     */
    public static final Integer UN_PAID = 0;
    public static final Integer PAID = 1;
    public static final Integer REFUND = 2;

    private static final long serialVersionUID = 1L;

    private Long id;

    //订单号
    private String number;

    //订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消 7退款
    private Integer status;

    //下单用户id
    private Long userId;

    //地址id
    private Long addressBookId;

    //下单时间
    private LocalDateTime orderTime;

    //结账时间
    private LocalDateTime checkoutTime;

    //支付方式 1微信，2支付宝
    private Integer payMethod;

    //支付状态 0未支付 1已支付 2退款
    private Integer payStatus;

    //实收金额
    private BigDecimal amount;

    //备注
    private String remark;

    //用户名
    private String userName;

    //手机号
    private String phone;

    //地址
    private String address;

    //收货人
    private String consignee;

    //订单取消原因
    private String cancelReason;

    //订单拒绝原因
    private String rejectionReason;

    //订单取消时间
    private LocalDateTime cancelTime;

    //预计送达时间
    private LocalDateTime estimatedDeliveryTime;

    //配送状态  1立即送出  0选择具体时间
    private Integer deliveryStatus;

    //送达时间
    private LocalDateTime deliveryTime;

    //打包费
    private Integer packAmount;

    //餐具数量
    private Integer tablewareNumber;

    //餐具数量状态  1按餐量提供  0选择具体数量
    private Integer tablewareStatus;

    // ===== 社区团购扩展字段 =====

    //社区团购自提点id（团购订单使用）
    private Long pickupPointId;

    //自提点名称
    private String pickupPointName;

    //是否拼团订单 0否 1是
    // 注意：orders.is_groupon 在库里是 NOT NULL 列。普通下单流程用 new Orders()，
    // 这里的初始值可以兜底；拼团流程用 builder 并会显式设为 1。
    // ⚠️ 坑：用 @Builder 时这个字段默认值**不会生效**（builder 不走字段初始化），
    //    拼团/自提等走 builder 的路径必须显式 .isGroupon(...)，否则插入 NULL 触发完整性约束异常。
    private Integer isGroupon = 0;

    //关联拼团活动id
    private Long grouponId;

    // ===== 自提核销扩展字段（m21-pickup-verify-migration.sql） =====

    /**
     * 核销状态常量：0 未核销 / 1 已核销
     * 与 status 是两个独立维度——status 描述订单生命周期，verify_status 只描述「有没有被领走」。
     */
    public static final Integer UN_VERIFIED = 0;
    public static final Integer VERIFIED = 1;

    //自提提货核销码（8 位数字，支付成功后生成，UNIQUE）
    private String pickupCode;

    //预约自提时间段（如 "明日 14:00-16:00"）
    private String pickupTimeSlot;

    //提货人姓名（自提订单不依赖地址簿，故随单快照）
    private String pickupPersonName;

    //提货人手机号
    private String pickupPersonPhone;

    //核销状态 0未核销 1已核销
    private Integer verifyStatus = UN_VERIFIED;

    //核销时间
    private LocalDateTime verifyTime;
}
