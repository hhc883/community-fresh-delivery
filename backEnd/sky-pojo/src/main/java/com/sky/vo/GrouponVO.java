package com.sky.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 拼团详情 VO：用于拼团详情页展示「还差几人成团」
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrouponVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long orderId;

    private Long dishId;

    //商品名称
    private String dishName;

    //商品图片
    private String dishImage;

    //商品原价
    private BigDecimal originalPrice;

    //拼团价
    private BigDecimal grouponPrice;

    //规格单位
    private String unit;

    //产地
    private String origin;

    //成团所需人数
    private Integer requiredSize;

    //当前已参团人数
    private Integer currentSize;

    //还差几人成团
    private Integer lackSize;

    //拼团状态 1拼团中 2已成团 3已失败 4已过期
    private Integer status;

    //拼团截止时间
    private LocalDateTime expireTime;

    //剩余毫秒数，前端用于倒计时
    private long remainMillis;

    //团长昵称
    private String leaderName;

    //自提点名称
    private String pickupPointName;

    //参团成员列表
    private List<MemberVO> members;

    /**
     * 参团成员简要信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberVO implements Serializable {

        private static final long serialVersionUID = 1L;

        private Long userId;

        private String userName;

        private String avatar;

        //是否团长
        private Integer isLeader;

        private Integer number;

        private LocalDateTime joinTime;
    }
}
