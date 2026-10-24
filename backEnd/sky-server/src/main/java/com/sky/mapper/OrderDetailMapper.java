package com.sky.mapper;

import com.sky.entity.OrderDetail;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderDetailMapper {
    /**
     * 批量插入订单明细数据
     * @param orderDetailList
     */
    void insertBatch(List<OrderDetail> orderDetailList);

    /**
     * 插入单条订单明细。
     * <p>
     * 注意：不要用 insertBatch(Collections.singletonList(...)) 代替本方法。
     * insertBatch 的 XML 里 foreach 使用了 collection="orderDetailList"，
     * 该名称依赖调用方形参名，传匿名 List 会导致 MyBatis 绑定失败。
     *
     * @param orderDetail
     */
    @Insert("insert into order_detail (name, image, order_id, dish_id, setmeal_id, dish_flavor, number, amount) "
            + "values (#{name}, #{image}, #{orderId}, #{dishId}, #{setmealId}, #{dishFlavor}, #{number}, #{amount})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(OrderDetail orderDetail);

    /**
     * 根据订单id查询订单明细
     * @param orderId
     * @return
     */
    @Select("select * from order_detail where order_id = #{orderId}")
    List<OrderDetail> getByOrderId(Long orderId);
}
