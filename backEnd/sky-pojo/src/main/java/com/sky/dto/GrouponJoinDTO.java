package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 开团 / 参团 请求参数
 */
@Data
public class GrouponJoinDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 参团方式一：开新团时传商品id（groupId 为空）
     */
    private Long dishId;

    /**
     * 参团方式二：加入已有团时传拼团活动id（dishId 可空，由服务端回填）
     */
    private Long grouponId;

    /**
     * 购买数量
     */
    private Integer number;

    /**
     * 收货地址id
     */
    private Long addressBookId;

    /**
     * 自提点id
     */
    private Long pickupPointId;

    /**
     * 备注
     */
    private String remark;
}
