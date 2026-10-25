package com.sky.mapper;

import com.sky.entity.OrderStock;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface OrderStockMapper {
    @Insert("insert into order_stock(order_id,dish_id,number) values(#{orderId},#{dishId},#{number})")
    void insert(OrderStock stock);

    @Select("select * from order_stock where order_id=#{orderId} order by dish_id")
    List<OrderStock> list(Long orderId);
}
