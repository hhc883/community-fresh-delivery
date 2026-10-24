package com.sky.service;

import com.sky.dto.*;
import com.sky.result.PageResult;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

public interface OrderService {
    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);

    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功，修改订单状态
     * @param outTradeNo
     */
    void paySuccess(String outTradeNo);

    /**
     * 用户端订单分页查询
     * @param page
     * @param pageSize
     * @param status
     * @return
     */
    PageResult<OrderVO> pageQueryByUser(int page, int pageSize, Integer status);

    /**
     * 查询订单详情
     * @param id
     * @return
     */
    OrderVO details(Long id);

    /** 用户入口只能查看当前用户自己的订单，管理员入口继续使用 details。 */
    OrderVO userDetails(Long id);

    /**
     * 用户取消订单
     * @param id
     */
    void userCancelById(Long id) throws Exception;

    /**
     * 再来一单
     * @param id
     */
    void repetition(Long id);

    /**
     * 条件搜索订单
     * @param ordersPageQueryDTO
     * @return
     */
    PageResult<OrderVO> conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 各个状态的订单数量统计
     * @return
     */
    OrderStatisticsVO statistics();

    /**
     * 接单
     *
     * @param ordersConfirmDTO
     */
    void confirm(OrdersConfirmDTO ordersConfirmDTO);

    /**
     * 拒单
     *
     * @param ordersRejectionDTO
     */
    void rejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception;

    /**
     * 商家取消订单
     *
     * @param ordersCancelDTO
     */
    void cancel(OrdersCancelDTO ordersCancelDTO) throws Exception;

    /**
     * 派送订单
     *
     * @param id
     */
    void delivery(Long id);

    /**
     * 完成订单
     *
     * @param id
     */
    void complete(Long id);

    /**
     * 客户催单
     * @param id
     */
    void reminder(Long id);
//cancel order
    void userCancel(Long id);

    // ===== 自提核销（m21） =====

    /**
     * 模拟支付（自提改造新增的独立入口）
     *
     * <p>与已有 {@link #payment(OrdersPaymentDTO)} 的区别只是入参形式：
     * payment 走订单号（前端支付页原有契约），mockPay 走订单 id（更直观、幂等友好）。
     * 两条路最终都汇入 {@link #paySuccess(String)}，因此**都会生成提货码**，行为一致。
     *
     * @param orderId 订单 id
     * @return 支付后的提货码（便于前端立即展示取货凭证）
     */
    String mockPay(Long orderId);

    /**
     * 按提货码核销（团长/自提点端）
     *
     * <p>幂等且防并发：同一张单被两人同时核销时，只有一人成功，另一人收到「已核销」。
     *
     * @param dto 含 pickupCode，可选 pickupPointId（传了则校验订单归属该自提点）
     * @return 核销后的订单（含 orderId/number/pickupCode/verifyTime，便于前端展示结果）
     */
    com.sky.vo.OrderVO verifyByCode(com.sky.dto.OrderVerifyDTO dto);
}
