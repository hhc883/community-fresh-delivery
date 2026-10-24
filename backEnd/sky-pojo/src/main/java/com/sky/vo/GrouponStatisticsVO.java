package com.sky.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 拼团统计 VO（管理端报表）
 */
@Data
public class GrouponStatisticsVO implements Serializable {

    //拼团中活动数
    private Integer ongoing;

    //已成团活动数
    private Integer success;

    //已失败活动数
    private Integer failed;

    //已过期活动数
    private Integer expired;

    //参团人次（区间内活动的成员总数）
    private Integer memberCount;

    //拼团订单数（orders.is_groupon = 1）
    private Integer grouponOrderCount;

    //拼团订单实收金额（仅已支付）
    private BigDecimal grouponTurnover;

}
