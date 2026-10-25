package com.sky.vo;

import com.sky.entity.DishFlavor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DishVO implements Serializable {

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
    //更新时间
    private LocalDateTime updateTime;
    //分类名称
    private String categoryName;
    //菜品关联的口味
    private List<DishFlavor> flavors = new ArrayList<>();

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
    //规格单位
    private String unit;
    //产地
    private String origin;
    //是否生鲜 0否 1是
    private Integer isFresh;

    // ===== 自提/秒杀扩展字段 =====
    private Integer soldCount;
    private String promoTag;
    private Integer seckill;
    private BigDecimal seckillPrice;
    private String tags;
    private Integer limitBuy;
    private String goodRate;
    private String category; // meat, vegetable, fastfood, etc.

    /**
     * 兼容前端 detail.vue 中 res.data.data 解构
     */
    public Object getData() {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("id", id);
        map.put("name", name);
        map.put("price", price);
        map.put("image", image);
        map.put("description", description);
        map.put("status", status);
        map.put("soldCount", soldCount);
        map.put("sold", soldCount);
        map.put("promoTag", promoTag);
        map.put("tag", promoTag);
        map.put("seckill", seckill);
        map.put("seckillPrice", seckillPrice);
        map.put("tags", tags);
        map.put("limitBuy", limitBuy);
        map.put("goodRate", goodRate);
        map.put("category", category);
        map.put("flavors", flavors);
        return map;
    }
}
