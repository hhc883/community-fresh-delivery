package com.sky.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrdersSubmitDTO implements Serializable {
    private String clientRequestId;
    // 1 送货上门，2 社区自提；旧客户端省略时按唯一履约 ID 推断。
    private Integer deliveryType;
    //地址簿id
    private Long addressBookId;
    //付款方式
    private int payMethod;
    //备注
    private String remark;
    //预计送达时间
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime estimatedDeliveryTime;
    //配送状态  1立即送出  0选择具体时间
    private Integer deliveryStatus;
    //餐具数量
    private Integer tablewareNumber;
    //餐具数量状态  1按餐量提供  0选择具体数量
    private Integer tablewareStatus;
    //打包费
    private Integer packAmount;
    //总金额
    private BigDecimal amount;

    // ===== 自提模式扩展字段 =====
    // 说明：本工程原下单流程强制要求 addressBookId 非空（走进门配送）。
    // 自提模式下不需要地址簿，改为传以下字段；OrderServiceImpl#submitOrder 按
    // pickupPointId 是否为 null 分流到两条校验路径。

    //自提点id（非空即为自提订单）
    private Long pickupPointId;
    //自提点名称
    private String pickupPointName;
    //预约自提时段，如 "明日 14:00-16:00"
    private String pickupTimeSlot;
    //提货人姓名
    private String pickupPersonName;
    //提货人手机号
    private String pickupPersonPhone;

    // 前端提交的商品明细列表（直接下单无需通过购物车）
    private java.util.List<java.util.Map<String, Object>> orderDetailList;
    private java.util.List<java.util.Map<String, Object>> items;

    public void setConsigneeName(String consigneeName) {
        if (this.pickupPersonName == null) {
            this.pickupPersonName = consigneeName;
        }
    }

    public void setConsigneePhone(String consigneePhone) {
        if (this.pickupPersonPhone == null) {
            this.pickupPersonPhone = consigneePhone;
        }
    }
}
