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

    /**
     * 统计某商品仍被进行中订单（待支付/待确认/已确认/配送中）占用的库存预留条数
     */
    @Select("select count(*) from order_stock os inner join orders o on o.id = os.order_id "
            + "where os.dish_id = #{dishId} and o.status in (1, 2, 3, 4)")
    int countActiveReservations(Long dishId);
}
