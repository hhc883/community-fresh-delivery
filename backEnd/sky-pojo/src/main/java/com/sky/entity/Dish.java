package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 菜品
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Dish implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //菜品名称
    private String name;

    //菜品分类id
    private Long categoryId;

    //菜品价格
    private BigDecimal price;

    //图片
    private String image;

    //描述信息
    private String description;

    //0 停售 1 起售
    private Integer status;

    // ===== 社区团购 / 生鲜扩展字段 =====

    //是否参与拼团 0否 1是
    private Integer isGroupon;

    //拼团价（成团价）
    private BigDecimal grouponPrice;

    //成团所需人数
    private Integer grouponSize;

    //拼团有效时长（小时）
    private Integer grouponValidHours;

    //库存
    private Integer stock;

    //规格单位，如 500g/份/箱
    private String unit;

    //产地
    private String origin;

    //是否生鲜 0否 1是
    private Integer isFresh;

    // ===== 自提/秒杀扩展字段 =====
    //销量
    private Integer soldCount;

    //促销标签（如：双旦特惠、直降¥100）
    private String promoTag;

    //是否秒杀：0否 1是
    private Integer seckill;

    //秒杀价
    private BigDecimal seckillPrice;

    //特性标签逗号隔开
    private String tags;

    //限购数量
    private Integer limitBuy;

    //好评率
    private String goodRate;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createUser;

    private Long updateUser;

}
