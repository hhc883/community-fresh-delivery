package com.sky.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSubmitVO implements Serializable {
    //订单id
    private Long id;
    //订单号
    private String orderNumber;
    //订单金额
    private BigDecimal orderAmount;
    private BigDecimal amount;
    //下单时间
    private LocalDateTime orderTime;

    public BigDecimal getAmount() {
        return amount != null ? amount : orderAmount;
    }

    public Object getData() {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("id", id);
        map.put("orderNumber", orderNumber);
        map.put("orderAmount", getAmount());
        map.put("amount", getAmount());
        map.put("orderTime", orderTime);
        return map;
    }
}
