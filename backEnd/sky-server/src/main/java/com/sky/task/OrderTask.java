package com.sky.task;

import com.sky.context.BaseContext;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
@ConditionalOnProperty(name = "sky.scheduling.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class OrderTask {
    @Autowired private OrderMapper orderMapper;
    @Autowired private OrderService orderService;

    @Scheduled(cron = "0 * * * * ?", zone = "Asia/Shanghai")
    public void processTimeoutOrders() {
        BaseContext.removeCurrentId();
        for (Orders order : orderMapper.getByStatusAndOrderTimeLT(Orders.PENDING_PAYMENT,
                LocalDateTime.now(ZoneId.of("Asia/Shanghai")).minusMinutes(15))) {
            try {
                orderService.expireUnpaidOrder(order.getId());
            } catch (RuntimeException failure) {
                log.error("订单超时处理失败，orderId={}", order.getId(), failure);
            }
        }
    }
}
