package com.sky.controller.api;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.utils.PickupCodeUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pay")
@Slf4j
@Api(tags = "生鲜自提-支付接口")
public class ApiPayController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderMapper orderMapper;

    @PostMapping("/{orderId}/mock-pay")
    @ApiOperation("模拟支付接口并生成8位提货码")
    public Result<String> mockPay(@PathVariable String orderId) {
        log.info("请求模拟支付: orderId={}", orderId);

        Long numericId = null;
        try {
            numericId = Long.valueOf(orderId);
        } catch (Exception ignored) {}

        Orders order = null;
        if (numericId != null) {
            order = orderMapper.getById(numericId);
        }
        if (order == null) {
            order = orderMapper.getByNumber(orderId);
        }

        String pickupCode;
        if (order != null) {
            // 真实订单调用服务层完成支付并生成唯一提货码
            pickupCode = orderService.mockPay(order.getId());
        } else {
            // 本地离线/mock调试订单生成合法 8 位提货码
            pickupCode = PickupCodeUtil.generate();
            log.info("未找到数据库订单，生成模拟提货码: orderId={}, pickupCode={}", orderId, pickupCode);
        }

        return Result.success(pickupCode);
    }
}
