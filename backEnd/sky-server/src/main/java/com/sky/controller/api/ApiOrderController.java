package com.sky.controller.api;

import com.sky.context.BaseContext;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.PickupPointMapper;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.service.AddressBookService;
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
    private AddressBookService addressBookService;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private PickupPointMapper pickupPointMapper;

    @PostMapping
    @ApiOperation("提交自提订单")
    public Result<OrderSubmitVO> submitOrder(@RequestBody OrdersSubmitDTO submitDTO) {
        return Result.success(orderService.submitOrder(submitDTO));
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
