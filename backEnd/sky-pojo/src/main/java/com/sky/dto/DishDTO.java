package com.sky.dto;

import com.sky.entity.DishFlavor;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class DishDTO implements Serializable {

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
    //口味
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

}
