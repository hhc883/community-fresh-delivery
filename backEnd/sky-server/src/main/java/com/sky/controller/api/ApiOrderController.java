package com.sky.controller.api;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.PickupPointMapper;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/orders")
@Slf4j
@Api(tags = "生鲜自提-订单接口")
public class ApiOrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private PickupPointMapper pickupPointMapper;

    @PostMapping
    @ApiOperation("提交自提订单")
    public Result<OrderSubmitVO> submitOrder(@RequestBody OrdersSubmitDTO submitDTO) {
        log.info("前端提交自提订单: {}", submitDTO);

        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new com.sky.exception.OrderBusinessException(com.sky.constant.MessageConstant.USER_NOT_LOGIN);
        }

        if (submitDTO.getPackAmount() == null) submitDTO.setPackAmount(0);
        if (submitDTO.getTablewareNumber() == null) submitDTO.setTablewareNumber(0);
        if (submitDTO.getTablewareStatus() == null) submitDTO.setTablewareStatus(1);
        if (submitDTO.getDeliveryStatus() == null) submitDTO.setDeliveryStatus(1);

        // 判断是否有前端直传的 items / orderDetailList
        List<Map<String, Object>> items = submitDTO.getOrderDetailList();
        if (items == null || items.isEmpty()) {
            items = submitDTO.getItems();
        }

        if (items != null && !items.isEmpty()) {
            // 前端直接传了商品清单，无需依赖购物车表直接落库
            Orders orders = new Orders();
            BeanUtils.copyProperties(submitDTO, orders);
            orders.setUserId(userId);
            orders.setOrderTime(LocalDateTime.now());
            orders.setStatus(Orders.PENDING_PAYMENT); // 1 待付款
            orders.setPayStatus(Orders.UN_PAID);
            orders.setNumber(String.valueOf(System.currentTimeMillis()));
            orders.setIsGroupon(0);
            orders.setVerifyStatus(Orders.UN_VERIFIED);

            if (submitDTO.getAmount() != null) {
                orders.setAmount(submitDTO.getAmount());
            } else {
                orders.setAmount(new BigDecimal("599.00"));
            }

            if (StringUtils.hasText(submitDTO.getPickupPersonName())) {
                orders.setConsignee(submitDTO.getPickupPersonName());
                orders.setPickupPersonName(submitDTO.getPickupPersonName());
            }
            if (StringUtils.hasText(submitDTO.getPickupPersonPhone())) {
                orders.setPhone(submitDTO.getPickupPersonPhone());
                orders.setPickupPersonPhone(submitDTO.getPickupPersonPhone());
            }
            if (StringUtils.hasText(submitDTO.getPickupPointName())) {
                orders.setPickupPointName(submitDTO.getPickupPointName());
            } else if (submitDTO.getPickupPointId() != null) {
                com.sky.entity.PickupPoint pp = pickupPointMapper.getById(submitDTO.getPickupPointId());
                if (pp != null) {
                    orders.setPickupPointName(pp.getName());
                    orders.setAddress(pp.getAddress());
                }
            }

            orderMapper.insert(orders);

            // 插入订单明细
            List<OrderDetail> detailList = new ArrayList<>();
            for (Map<String, Object> item : items) {
                OrderDetail detail = new OrderDetail();
                detail.setOrderId(orders.getId());
                detail.setName(item.get("name") != null ? String.valueOf(item.get("name")) : "生鲜优选商品");
                detail.setImage(item.get("image") != null ? String.valueOf(item.get("image")) : "/static/images/png/dish-2.png");
                detail.setDishFlavor(item.get("dishFlavor") != null ? String.valueOf(item.get("dishFlavor")) : "标准规格");

                Object dishIdObj = item.get("dishId");
                if (dishIdObj != null) {
                    try {
                        detail.setDishId(Long.valueOf(dishIdObj.toString()));
                    } catch (Exception ignored) {}
                }

                Object qtyObj = item.get("number") != null ? item.get("number") : item.get("quantity");
                detail.setNumber(qtyObj != null ? Integer.valueOf(qtyObj.toString()) : 1);

                Object priceObj = item.get("amount") != null ? item.get("amount") : item.get("price");
                detail.setAmount(priceObj != null ? new BigDecimal(priceObj.toString()) : orders.getAmount());

                detailList.add(detail);
            }
            orderDetailMapper.insertBatch(detailList);

            OrderSubmitVO submitVO = OrderSubmitVO.builder()
                    .id(orders.getId())
                    .orderNumber(orders.getNumber())
                    .orderAmount(orders.getAmount())
                    .amount(orders.getAmount())
                    .orderTime(orders.getOrderTime())
                    .build();
            return Result.success(submitVO);
        }

        // 走原有购物车提交流程
        OrderSubmitVO submitVO = orderService.submitOrder(submitDTO);
        return Result.success(submitVO);
    }

    @GetMapping
    @ApiOperation("分页查询订单列表")
    public Result<PageResult<OrderVO>> getOrders(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status) {
        log.info("查询自提订单列表: pageNum={}, pageSize={}, status={}", pageNum, pageSize, status);

        Integer statusInt = null;
        if ("pending_pay".equalsIgnoreCase(status)) {
            statusInt = Orders.PENDING_PAYMENT; // 1
        } else if ("pending_pickup".equalsIgnoreCase(status)) {
            statusInt = Orders.TO_BE_CONFIRMED; // 2
        } else if ("completed".equalsIgnoreCase(status)) {
            statusInt = Orders.COMPLETED; // 5
        }

        PageResult<OrderVO> pageResult = orderService.pageQueryByUser(pageNum, pageSize, statusInt);

        return Result.success(pageResult);
    }

    @GetMapping("/{id}")
    @ApiOperation("查询本人订单详情")
    public Result<OrderVO> getOrderDetail(@PathVariable Long id) {
        return Result.success(orderService.userDetails(id));
    }

    @PostMapping("/{id}/cancel")
    @ApiOperation("取消本人订单")
    public Result<String> cancelOrder(@PathVariable Long id) {
        orderService.userCancel(id);
        return Result.success("订单取消成功");
    }
}
