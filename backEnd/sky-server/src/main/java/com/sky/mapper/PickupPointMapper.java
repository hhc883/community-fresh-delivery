package com.sky.mapper;

import com.sky.entity.PickupPoint;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PickupPointMapper {

    /**
     * 新增自提点
     *
     * @param pickupPoint
     */
    void insert(PickupPoint pickupPoint);

    /**
     * 根据id查询自提点
     *
     * @param id
     */
    @Select("select * from pickup_point where id = #{id}")
    PickupPoint getById(Long id);

    /**
     * 动态条件查询自提点
     *
     * @param pickupPoint
     */
    List<PickupPoint> list(PickupPoint pickupPoint);

    /**
     * 修改自提点
     *
     * @param pickupPoint
     */
    void update(PickupPoint pickupPoint);

    /**
     * 根据id删除自提点
     *
     * @param id
     */
    @Delete("delete from pickup_point where id = #{id}")
    void deleteById(Long id);
}
