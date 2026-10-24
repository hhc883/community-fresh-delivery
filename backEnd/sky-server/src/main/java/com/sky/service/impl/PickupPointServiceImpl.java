package com.sky.service.impl;

import com.sky.entity.PickupPoint;
import com.sky.exception.GrouponBusinessException;
import com.sky.mapper.PickupPointMapper;
import com.sky.service.PickupPointService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 社区自提点业务实现
 */
@Service
@Slf4j
public class PickupPointServiceImpl implements PickupPointService {

    @Autowired
    private PickupPointMapper pickupPointMapper;

    @Override
    public void save(PickupPoint pickupPoint) {
        LocalDateTime now = LocalDateTime.now();
        pickupPoint.setCreateTime(now);
        pickupPoint.setUpdateTime(now);
        if (pickupPoint.getStatus() == null) {
            pickupPoint.setStatus(1);
        }
        pickupPointMapper.insert(pickupPoint);
    }

    @Override
    public void update(PickupPoint pickupPoint) {
        pickupPoint.setUpdateTime(LocalDateTime.now());
        pickupPointMapper.update(pickupPoint);
    }

    @Override
    public void deleteById(Long id) {
        pickupPointMapper.deleteById(id);
    }

    @Override
    public PickupPoint getById(Long id) {
        PickupPoint point = pickupPointMapper.getById(id);
        if (point == null) {
            throw new GrouponBusinessException("自提点不存在");
        }
        return point;
    }

    @Override
    public List<PickupPoint> list(PickupPoint pickupPoint) {
        return pickupPointMapper.list(pickupPoint);
    }
}
