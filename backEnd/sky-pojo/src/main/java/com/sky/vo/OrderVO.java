package com.sky.vo;

import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderVO extends Orders implements Serializable {

    //订单菜品信息
    private String orderDishes;

    //订单详情
    private List<OrderDetail> orderDetailList;

    // ===== 自提信息（冗余自提点表字段，便于小程序直接渲染提货页） =====
    // OrderVO 继承 Orders，pickupPointId / pickupCode / pickupTimeSlot / 提货人 这些在 Orders 上已有，
    // 这里只补「自提点表里的」字段，避免前端为渲染一个地址再发一次请求。

    //自提点名称
    private String pickupPointName;

    //自提点详细地址
    private String pickupPointAddress;

    //自提点联系电话（到店找不到人时用）
    private String pickupPointPhone;

    //自提点营业时间
    private String pickupPointOpenTime;

    /**
     * 兼容前端 order/detail.vue 中 res.data.data 解构
     */
    public Object getData() {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("id", getId());
        map.put("number", getNumber());
        map.put("status", getStatus());
        map.put("userId", getUserId());
        map.put("orderTime", getOrderTime());
        map.put("payMethod", getPayMethod());
        map.put("payStatus", getPayStatus());
        map.put("amount", getAmount());
        map.put("remark", getRemark());
        map.put("userName", getUserName());
        map.put("phone", getPhone());
        map.put("address", getAddress());
        map.put("consignee", getConsignee());
        map.put("pickupCode", getPickupCode());
        map.put("pickupTimeSlot", getPickupTimeSlot());
        map.put("pickupTime", getPickupTimeSlot());
        map.put("pickupPersonName", getPickupPersonName());
        map.put("pickupPersonPhone", getPickupPersonPhone());
        map.put("pickupPointName", pickupPointName != null ? pickupPointName : super.getPickupPointName());
        map.put("pickupPointAddress", pickupPointAddress);
        map.put("pickupPointPhone", pickupPointPhone);
        map.put("verifyStatus", getVerifyStatus());
        map.put("verifyTime", getVerifyTime());
        map.put("orderDetailList", orderDetailList);
        return map;
    }
}
