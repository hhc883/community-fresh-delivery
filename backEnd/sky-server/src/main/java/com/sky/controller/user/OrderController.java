package com.sky.controller.user;

import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController("userOrderController")
@ApiOperation("订单接口")
@RequestMapping("/user/order")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/submit")
    @ApiOperation("用户下单")
    public Result<OrderSubmitVO> submit(@RequestBody OrdersSubmitDTO dto) {
        OrderSubmitVO orderSubmitVO = orderService.submitOrder(dto);
        return Result.success(orderSubmitVO);
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    @PutMapping("/payment")
    @ApiOperation("订单支付")
    public Result<OrderPaymentVO> payment(@RequestBody OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        log.info("订单支付：{}", ordersPaymentDTO);
        log.info("订单支付：{}", ordersPaymentDTO);
        OrderPaymentVO orderPaymentVO = orderService.payment(ordersPaymentDTO);
        log.info("生成预支付交易单：{}", orderPaymentVO);
        return Result.success(orderPaymentVO);
    }

    /**
     * 订单支付
     * @param id
     * @return
     */
    @GetMapping("/orderDetail/{id}")
    @ApiOperation("查询订单详情")
    public Result<OrderVO> getOrderDetail(@PathVariable Long id) {
        OrderVO orderVO = orderService.details(id);
        return Result.success(orderVO);
    }

    @GetMapping("/historyOrders")
    @ApiOperation("分页查询历史订单")
    public Result<PageResult<OrderVO>> historyOrders(int page, int pageSize, Integer status) {
        PageResult<OrderVO> pageResult = orderService.pageQueryByUser(page, pageSize, status);
        return Result.success(pageResult);
    }

    /**
     * 再来一单
     *
     * @param id
     * @return
     */
    @PostMapping("/repetition/{id}")
    @ApiOperation("再来一单")
    public Result<String> repetition(@PathVariable Long id) {
        orderService.repetition(id);
        return Result.success();
    }

    /**
     * 模拟支付（m21 自提改造新增）
     *
     * <p>与上面 {@code PUT /user/order/payment} 的关系：两者都走 {@code paySuccess}，
     * 因此**都会生成 8 位提货码**，行为一致，前端用哪个都行：
     * <ul>
     *   <li>{@code PUT /payment} —— 原有契约，入参订单号，请求体 {@code {orderNumber, payMethod}}</li>
     *   <li>{@code POST /{id}/mock-pay} —— 新增，入参订单 id（前端下单后手里就有），少一次查号</li>
     * </ul>
     *
     * <p>幂等：重复调用返回同一个提货码，不会重新生成（避免到店按旧码核销失败）。
     *
     * @return 提货码字符串（data 直接是码，方便前端 {{code}} 直接用）
     */
    @PostMapping("/{id}/mock-pay")
    @ApiOperation("模拟支付并生成提货码")
    public Result<String> mockPay(@PathVariable Long id) {
        log.info("模拟支付：orderId={}", id);
        String pickupCode = orderService.mockPay(id);
        return Result.success(pickupCode);
    }

    /**
     * 客户催单
     */
    @GetMapping("/reminder/{id}")
    @ApiOperation("催单")
    public Result reminder(@PathVariable Long id) {
        orderService.reminder(id);
        return Result.success();
    }



//    cancel order
    /**
     * 用户取消订单
     */
    @PutMapping("/cancel/{id}")
    @ApiOperation("用户取消订单")
    public Result cancel(@PathVariable Long id) {
        orderService.userCancel(id);
        return Result.success();
    }
}
