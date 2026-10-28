package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.GoodsSalesDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {

    /**
     * 插入订单数据
     * @param orders
     */
    void insert(Orders orders);

    @Select("select * from orders where user_id=#{userId} and client_request_id=#{clientRequestId}")
    Orders getByClientRequestId(@Param("userId") Long userId, @Param("clientRequestId") String clientRequestId);

    /**
     * 根据订单号和用户id查询订单
     * @param orderNumber
     * @param userId
     */
    @Select("select * from orders where number = #{orderNumber} and user_id= #{userId}")
    Orders getByNumberAndUserId(String orderNumber, Long userId);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);

    /**
     * 分页条件查询并按下单时间排序
     * @param ordersPageQueryDTO
     */
    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根据id查询订单
     * @param id
     */
    @Select("select * from orders where id=#{id}")
    Orders getById(Long id);

    @Select("select * from orders where id=#{id} for update")
    Orders getByIdForUpdate(Long id);

    @Select("select * from orders where number=#{orderNumber} for update")
    Orders getByNumberForUpdate(String orderNumber);

    /**
     * 根据状态统计订单数量
     * @param status
     */
    @Select("select count(id) from orders where status = #{status}")
    Integer countStatus(Integer status);

    /**
     * 根据订单状态和下单时间查询订单
     * @param status
     * @param orderTime
     * @return
     */
    @Select("select * from orders where status = #{status} and order_time < #{orderTime}")
    List<Orders> getByStatusAndOrderTimeLT(Integer status, LocalDateTime orderTime);

    /**
     * 根据动态条件统计营业额数据
     * @param map
     * @return
     */
    Double sumByMap(Map map);

    /**
     * 根据动态条件统计订单数量
     * @param map
     * @return
     */
    Integer countByMap(Map map);

    /**
     * 统计指定时间区间内的销量排名前10
     * @param begin
     * @param end
     * @return
     */
    List<GoodsSalesDTO> getSalesTop10(LocalDateTime begin,LocalDateTime end);

    /**
     * 根据订单号查询订单
     *
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    // ===== 自提核销（m21-pickup-verify-migration.sql，SQL 见 OrderMapper.xml） =====

    /**
     * 按提货码查单（核销入口）
     * @param pickupCode 8 位提货码
     */
    Orders getByPickupCode(String pickupCode);

    /**
     * 抢占式核销：仅当 verify_status 仍为 0 时原子置 1
     *
     * <p>为什么不先查再改：两人同时拿同一张单核销时，先查再改会双双读到 verify_status=0
     * 然后都更新成功（丢更新）。这里用带 {@code and verify_status = 0} 条件的单条 UPDATE，
     * 数据库行锁保证只有一个事务能命中，返回受影响行数即判定依据。
     *
     * @return 1 本次核销成功；0 已被核销或码不存在
     */
    int verifyByCode(@Param("pickupCode") String pickupCode,
                     @Param("verifyTime") LocalDateTime verifyTime,
                     @Param("status") Integer status);
}
