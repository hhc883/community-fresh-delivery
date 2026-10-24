package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 管理端拼团活动分页查询参数
 */
@Data
public class GrouponPageQueryDTO implements Serializable {

    private int page;

    private int pageSize;

    //拼团状态 1拼团中 2已成团 3已失败 4已过期，可空
    private Integer status;

    //商品名模糊搜索，可空
    private String keyword;

}
