package com.sky.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 封装分页查询结果
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageResult<T> implements Serializable {

    private long total; //总记录数

    private List<T> records; //当前页数据集合

    public List<T> getList() {
        return records;
    }

    public Object getData() {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        // 此处是计数而非 ID，兼容嵌套信封也保持 JSON number。
        map.put("total", java.math.BigInteger.valueOf(total));
        map.put("records", records);
        map.put("list", records);
        return map;
    }
}
