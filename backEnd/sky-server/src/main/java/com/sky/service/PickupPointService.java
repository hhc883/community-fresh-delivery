package com.sky.service;

import com.sky.entity.PickupPoint;

import java.util.List;

/**
 * 社区自提点业务接口
 */
public interface PickupPointService {

    /**
     * 新增自提点
     */
    void save(PickupPoint pickupPoint);

    /**
     * 修改自提点
     */
    void update(PickupPoint pickupPoint);

    /**
     * 删除自提点
     */
    void deleteById(Long id);

    /**
     * 根据id查询
     */
    PickupPoint getById(Long id);

    /**
     * 条件查询自提点列表
     */
    List<PickupPoint> list(PickupPoint pickupPoint);
}
