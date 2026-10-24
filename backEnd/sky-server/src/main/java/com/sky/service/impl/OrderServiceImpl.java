package com.sky.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.dto.OrderVerifyDTO;
import com.sky.entity.AddressBook;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.PickupPoint;
import com.sky.entity.ShoppingCart;
// import com.sky.entity.User;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.service.OrderService;
import com.sky.service.AddressBookService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;

import lombok.extern.slf4j.Slf4j;

import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.PickupPointMapper;
import com.sky.mapper.ShoppingCartMapper;
// import com.sky.mapper.UserMapper;
import com.sky.result.PageResult;
import com.sky.utils.PickupCodeUtil;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    /** 提货码「生成→查重」最大重试次数（8 位空间 1 亿，正常一次命中；设上限防异常时无限循环） */
    private static final int PICKUP_CODE_MAX_RETRY = 10;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private AddressBookService addressBookService;

    @Autowired
    private PickupPointMapper pickupPointMapper;

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private WeChatPayUtil weChatPayUtil;

    // @Autowired
    // private UserMapper userMapper;

    @Autowired
    private WebSocketServer webSocketServer;

    /**
     * 用户下单
     *
     * @param ordersSubmitDTO
     * @return
     */
    @Override
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        requireCurrentUserId();
        // ===== 自提分支（m21）=====
        // 原流程第一件事就是强校验 addressBookId 非空，自提订单根本不走地址簿，
        // 若不前置分流会直接被 AddressBookBusinessException 挡住。
        // 故这里以 pickupPointId 是否为 null 作为唯一分流判据：
        //   非空 → 自提：校验自提点存在且启用，落提货人快照
        //   为空 → 保持原有配送流程，一行不改
        PickupPoint pickupPoint = null;
        if (ordersSubmitDTO.getPickupPointId() != null) {
            pickupPoint = pickupPointMapper.getById(ordersSubmitDTO.getPickupPointId());
            if (pickupPoint == null) {
                throw new OrderBusinessException(MessageConstant.PICKUP_POINT_NOT_FOUND);
            }
            if (!Integer.valueOf(1).equals(pickupPoint.getStatus())) {
                throw new OrderBusinessException(MessageConstant.PICKUP_POINT_DISABLED);
            }
            // 提货人信息是核销时核对身份的唯一依据，自提订单必须齐全
            if (!StringUtils.hasText(ordersSubmitDTO.getPickupPersonName())
                    || !StringUtils.hasText(ordersSubmitDTO.getPickupPersonPhone())) {
                throw new OrderBusinessException(MessageConstant.PICKUP_PERSON_REQUIRED);
            }
        }

        // 业务异常处理，地址为空、购物车为空
        // 注意：自提订单跳过地址簿校验（addressBook 保持 null，后续赋值分支也跳过）
        AddressBook addressBook = null;
        if (pickupPoint == null) {
            addressBook = addressBookService.getById(ordersSubmitDTO.getAddressBookId());
        }
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> cartList = shoppingCartMapper.list(shoppingCart);
        if (cartList == null || cartList.isEmpty()) {
            // 抛出异常
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        // 订单表插入一条数据
        Orders orders = new Orders();
        // 先把 DTO 同名字段拷进实体（含 pickupPointId/pickupTimeSlot/pickupPersonName/pickupPersonPhone，
        // 这四个是 m21 给 OrdersSubmitDTO 新加的同名字段，BeanUtils 会自动带上）
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        if (pickupPoint != null) {
            // 自提：地址快照取自提点，收货人取提货人（订单列表/详情无需再关联地址簿）
            orders.setAddress(pickupPoint.getAddress());
            orders.setPhone(ordersSubmitDTO.getPickupPersonPhone());
            orders.setConsignee(ordersSubmitDTO.getPickupPersonName());
            orders.setPickupPointName(pickupPoint.getName());
        } else if (StringUtils.hasText(ordersSubmitDTO.getPickupPointName())) {
            orders.setPickupPointName(ordersSubmitDTO.getPickupPointName());
        } else {
            orders.setAddress(addressBook.getDetail());
            orders.setPhone(addressBook.getPhone());
            orders.setConsignee(addressBook.getConsignee());
        }
        orders.setUserId(userId);
        // orders.is_groupon 是 NOT NULL 列，普通下单必须显式标记为非拼团订单，
        // 否则会以 NULL 插入而触发完整性约束异常（表现为前端「未知错误」）。
        orders.setIsGroupon(0);
        // 核销状态 NOT NULL DEFAULT 0；实体默认值此处生效（普通 new，非 builder）
        orders.setVerifyStatus(Orders.UN_VERIFIED);

        orderMapper.insert(orders);

        // 订单详情表插入多条数据
        List<OrderDetail> orderDetailList = new ArrayList<>();
        cartList.forEach(cart -> {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetailList.add(orderDetail);
        });
        orderDetailMapper.insertBatch(orderDetailList);

        // 清空购物车
        shoppingCartMapper.deleteByUserId(userId);

        // 返回 VO
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .amount(orders.getAmount())
                .build();

        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        requireOwnedOrder(orderMapper.getByNumberForUpdate(ordersPaymentDTO.getOrderNumber()));
        // 当前登录用户id
        // Long userId = BaseContext.getCurrentId();
        // User user = userMapper.getById(userId);

        // 直接调用paySuccess方法，模拟支付成功
        paySuccess(ordersPaymentDTO.getOrderNumber());

        // 调用微信支付接口，生成预支付交易单
        // JSONObject jsonObject = weChatPayUtil.pay(
        // ordersPaymentDTO.getOrderNumber(), // 商户订单号
        // new BigDecimal(0.01), // 支付金额，单位 元
        // "社区生鲜配送订单", // 商品描述
        // user.getOpenid() // 微信用户的openid
        // );

        // if (jsonObject.getString("code") != null &&
        // jsonObject.getString("code").equals("ORDERPAID")) {
        // throw new OrderBusinessException("该订单已支付");
        // }

        // OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        // vo.setPackageStr(jsonObject.getString("package"));

        // return vo;
        return null;
    }

    /**
     * 支付成功，修改订单状态
     *
     * <p><b>提货码在此生成（m21）</b>：无论走旧 {@link #payment} 还是新 {@link #mockPay}，
     * 最终都汇到这里，因此两条支付路径行为完全一致——这是刻意收敛的单一写码点，
     * 避免「两个入口各写一遍」导致其中一条漏生成。
     *
     * <p>幂等：重复支付有效的已付款订单时不重写提货码、付款时间或履约状态。
     * 否则用户重复点「支付」会看到提货码跳变，到店时按旧码核销会失败。
     *
     * @param outTradeNo 订单号
     */
    @Transactional
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumberForUpdate(outTradeNo);
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (Orders.CANCELLED.equals(ordersDB.getStatus()) || Orders.REFUND.equals(ordersDB.getPayStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        if (Orders.PAID.equals(ordersDB.getPayStatus())
                && (Orders.TO_BE_CONFIRMED.equals(ordersDB.getStatus())
                    || Orders.CONFIRMED.equals(ordersDB.getStatus())
                    || Orders.DELIVERY_IN_PROGRESS.equals(ordersDB.getStatus())
                    || Orders.COMPLETED.equals(ordersDB.getStatus()))) {
            return;
        }
        if (!Orders.PENDING_PAYMENT.equals(ordersDB.getStatus())
                || !Orders.UN_PAID.equals(ordersDB.getPayStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        // 提货码：仅首次支付生成；已存在则沿用（幂等，见方法注释）
        if (!StringUtils.hasText(ordersDB.getPickupCode())) {
            orders.setPickupCode(generateUniquePickupCode());
        }

        orderMapper.update(orders);

        // 通过websocket通知商家
        Map<String, Object> map = new HashMap<>();
        map.put("type", 1); // 1:来单通知
        map.put("orderId", ordersDB.getId());
        map.put("content", "订单号: " + outTradeNo);
        String msg = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(msg);
    }

    /**
     * 生成全局唯一的 8 位提货码（m21）。
     *
     * <p>唯一性三层保证：
     * <ol>
     *   <li>{@link PickupCodeUtil} 产出 8 位纯数字随机值（SecureRandom，不可预测）；</li>
     *   <li>生成后查库 {@code getByPickupCode}，若已存在则重试；</li>
     *   <li>数据库 {@code idx_orders_pickup_code} 索引兜底——即便并发下两个事务同时通过第 2 步，
     *       写入时也只有一个能成功。</li>
     * </ol>
     *
     * <p>重试上限 {@link #PICKUP_CODE_MAX_RETRY}：8 位空间 1 亿，实际订单量远小于此，
     * 正常一次就命中；设上限只为防止「索引异常/空间耗尽」时无限循环把请求拖死，
     * 此时宁可报错也不要发出一张可能重复的码。
     */
    private String generateUniquePickupCode() {
        for (int i = 0; i < PICKUP_CODE_MAX_RETRY; i++) {
            String code = PickupCodeUtil.generate();
            if (orderMapper.getByPickupCode(code) == null) {
                return code;
            }
            log.warn("提货码碰撞，重试第 {} 次：{}", i + 1, code);
        }
        // 走到这里说明连续多次撞码，属异常情况（不是用户错误），必须显式失败
        throw new OrderBusinessException(MessageConstant.PICKUP_CODE_GENERATE_FAILED);
    }

    /**
     * 模拟支付（m21 新增独立入口）
     *
     * <p>与旧 {@code payment} 的差别只在入参：那边传订单号，这边传订单 id。
     * 两者都汇入 {@link #paySuccess(String)}，因此提货码生成逻辑只有一份。
     *
     * <p>为什么还要加这条：前端自提确认页拿到的是 {@code orderId}
     * （{@code OrderSubmitVO.id}），原先要再取一次订单号才能调 payment，多一次请求且易错。
     *
     * @return 支付后的提货码
     */
    @Override
    @Transactional
    public String mockPay(Long orderId) {
        Orders ordersDB = orderMapper.getByIdForUpdate(orderId);
        requireOwnedOrder(ordersDB);
        // 所有入口都通过同一状态校验；已完成订单重复支付不会退回待履约。
        paySuccess(ordersDB.getNumber());
        // 回查拿最新码（paySuccess 内部已生成并落库）
        Orders latest = orderMapper.getById(orderId);
        return latest == null ? null : latest.getPickupCode();
    }

    /**
     * 按提货码核销（m21）
     *
     * <p>三重校验 + 一次原子更新：
     * <ol>
     *   <li>码格式（8 位数字）——把明显非法输入挡在 DB 之前；</li>
     *   <li>码存在——不存在报「提货码不存在」；</li>
     *   <li>归属校验——传了 pickupPointId 时必须与订单的自提点一致，防 A 点核销 B 点的货；</li>
     *   <li>原子核销——带 {@code verify_status = 0} 条件的 UPDATE，受影响行数 1 才算成功。</li>
     * </ol>
     *
     * <p>为什么状态同时置为 COMPLETED：自提场景下「核销」即「履约完成」，
     * 与配送场景的「送达」等价。若不置完成，订单会永远停在「待自提」污染列表与统计。
     */
    @Override
    @Transactional
    public OrderVO verifyByCode(OrderVerifyDTO dto) {
        String code = dto == null ? null : dto.getPickupCode();
        if (!PickupCodeUtil.isValid(code)) {
            throw new OrderBusinessException(MessageConstant.PICKUP_CODE_INVALID);
        }

        Orders ordersDB = orderMapper.getByPickupCode(code);
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.PICKUP_CODE_NOT_FOUND);
        }

        // 归属校验：只在调用方声明了自己是哪个自提点时做，避免单点多店场景误伤
        if (dto.getPickupPointId() != null
                && !dto.getPickupPointId().equals(ordersDB.getPickupPointId())) {
            throw new OrderBusinessException(MessageConstant.PICKUP_POINT_MISMATCH);
        }

        // 未支付订单不可能是合法待核销单（提货码本就在支付后生成，此处属纵深防御）
        if (!Orders.PAID.equals(ordersDB.getPayStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_PAID);
        }

        LocalDateTime now = LocalDateTime.now();
        int affected = orderMapper.verifyByCode(code, now, Orders.COMPLETED);
        if (affected == 0) {
            // 行数为 0：要么已被核销，要么并发下被别人抢先——两者对用户是同一句话
            throw new OrderBusinessException(MessageConstant.ORDER_ALREADY_VERIFIED);
        }
        log.info("核销成功：pickupCode={}, orderId={}, orderNumber={}", code, ordersDB.getId(), ordersDB.getNumber());

        // 返回核销后的最新状态（含 verifyTime），供前端展示
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(orderMapper.getById(ordersDB.getId()), vo);
        return vo;
    }

    /**
     * 查询历史订单
     *
     * @param page
     * @param pageSize
     * @param status
     * @return
     */
    @Override
    public PageResult<OrderVO> pageQueryByUser(int page, int pageSize, Integer status) {
        Long userId = requireCurrentUserId();
        // 设置分页
        PageHelper.startPage(page, pageSize);

        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(userId);
        ordersPageQueryDTO.setStatus(status);

        // 分页条件查询
        Page<Orders> pageOrders = orderMapper.pageQuery(ordersPageQueryDTO);

        List<OrderVO> list = new ArrayList<>();

        // 查询出订单明细，并封装入OrderVO进行响应
        if (pageOrders != null && pageOrders.getTotal() > 0) {
            for (Orders orders : pageOrders) {
                Long orderId = orders.getId();// 订单id

                // 查询订单明细
                List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orderId);

                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderDetailList(orderDetails);
                // 自提订单列表也要显示自提点名称/地址（m21），与详情页同口径
                fillPickupPointInfo(orderVO);

                list.add(orderVO);
            }
        }
        long total = Optional.ofNullable(pageOrders).map(Page::getTotal).orElse(0L);
        return new PageResult<>(total, list);
    }

    /**
     * 查询订单详情
     *
     * @param id
     * @return
     */
    public OrderVO details(Long id) {
        // 根据id查询订单
        Orders orders = orderMapper.getById(id);
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        return toOrderVO(orders);
    }

    @Override
    public OrderVO userDetails(Long id) {
        return toOrderVO(requireOwnedOrder(orderMapper.getById(id)));
    }

    private OrderVO toOrderVO(Orders orders) {

        // 查询该订单对应的菜品/套餐明细
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        // 将该订单及其详情封装到OrderVO并返回
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);

        // 自提订单补齐自提点信息（m21）：小程序提货页要显示「去哪儿提、找谁、几点开门」，
        // 若前端自己再发一次 /user/pickupPoint/{id} 就是多一次往返且多一处失败点。
        // pickupCode/pickupTimeSlot/提货人 已在 Orders 上，BeanUtils 已一并拷入。
        fillPickupPointInfo(orderVO);

        return orderVO;
    }

    /**
     * 给 OrderVO 补自提点冗余字段（m21）
     *
     * <p>非自提订单（pickupPointId 为空）直接返回，零开销；
     * 自提点被删除等异常情况静默跳过——订单本身仍应能查看，不该因为查不到自提点就 500。
     */
    private void fillPickupPointInfo(OrderVO orderVO) {
        if (orderVO == null || orderVO.getPickupPointId() == null) {
            return;
        }
        PickupPoint pp = pickupPointMapper.getById(orderVO.getPickupPointId());
        if (pp == null) {
            return;
        }
        orderVO.setPickupPointName(pp.getName());
        orderVO.setPickupPointAddress(pp.getAddress());
        orderVO.setPickupPointPhone(pp.getPhone());
        orderVO.setPickupPointOpenTime(pp.getOpenTime());
    }

    /**
     * 用户取消订单
     *
     * @param id
     */
    @Transactional
    public void userCancelById(Long id) throws Exception {
        userCancel(id);
    }

    /**
     * 再来一单
     *
     * @param id
     */
    public void repetition(Long id) {
        requireOwnedOrder(orderMapper.getById(id));
        // 查询当前用户id
        Long userId = BaseContext.getCurrentId();

        // 根据订单id查询当前订单详情
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

        // 将订单详情对象转换为购物车对象
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(x -> {
            ShoppingCart shoppingCart = new ShoppingCart();

            // 将原订单详情里面的菜品信息重新复制到购物车对象中
            BeanUtils.copyProperties(x, shoppingCart, "id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());

            return shoppingCart;
        }).collect(Collectors.toList());

        // 将购物车对象批量添加到数据库
        shoppingCartMapper.insertBatch(shoppingCartList);
    }

    /**
     * 订单搜索
     *
     * @param ordersPageQueryDTO
     * @return
     */
    public PageResult<OrderVO> conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        // 部分订单状态，需要额外返回订单菜品信息，将Orders转化为OrderVO
        List<OrderVO> orderVOList = getOrderVOList(page);

        return new PageResult<>(page.getTotal(), orderVOList);
    }

    private List<OrderVO> getOrderVOList(Page<Orders> page) {
        // 需要返回订单菜品信息，自定义OrderVO响应结果
        List<OrderVO> orderVOList = new ArrayList<>();

        List<Orders> ordersList = page.getResult();
        if (!CollectionUtils.isEmpty(ordersList)) {
            for (Orders orders : ordersList) {
                // 将共同字段复制到OrderVO
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                String orderDishes = getOrderDishesStr(orders);

                // 将订单菜品信息封装到orderVO中，并添加到orderVOList
                orderVO.setOrderDishes(orderDishes);
                orderVOList.add(orderVO);
            }
        }
        return orderVOList;
    }

    /**
     * 根据订单id获取菜品信息字符串
     *
     * @param orders
     * @return
     */
    private String getOrderDishesStr(Orders orders) {
        // 查询订单菜品详情信息（订单中的菜品和数量）
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        // 将每一条订单菜品信息拼接为字符串（格式：宫保鸡丁*3；）
        List<String> orderDishList = orderDetailList.stream().map(x -> {
            String orderDish = x.getName() + "*" + x.getNumber() + ";";
            return orderDish;
        }).collect(Collectors.toList());

        // 将该订单对应的所有菜品信息拼接在一起
        return String.join("", orderDishList);
    }

    /**
     * 各个状态的订单数量统计
     *
     * @return
     */
    public OrderStatisticsVO statistics() {
        // 根据状态，分别查询出待接单、待派送、派送中的订单数量
        Integer toBeConfirmed = orderMapper.countStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.countStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);

        // 将查询出的数据封装到orderStatisticsVO中响应
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        return orderStatisticsVO;
    }

    /**
     * 接单
     *
     * @param ordersConfirmDTO
     */
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders orders = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(Orders.CONFIRMED)
                .build();

        orderMapper.update(orders);
    }

    /**
     * 拒单
     *
     * @param ordersRejectionDTO
     */
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(ordersRejectionDTO.getId());

        // 订单只有存在且状态为2（待接单）才可以拒单
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // // 支付状态
        // Integer payStatus = ordersDB.getPayStatus();
        // if (payStatus == Orders.PAID) {
        //     // 用户已支付，需要退款
        //     String refund = weChatPayUtil.refund(
        //             ordersDB.getNumber(),
        //             ordersDB.getNumber(),
        //             new BigDecimal(0.01),
        //             new BigDecimal(0.01));
        //     log.info("申请退款：{}", refund);
        // }

        // 拒单需要退款，根据订单id更新订单状态、拒单原因、取消时间
        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        orders.setCancelTime(LocalDateTime.now());

        orderMapper.update(orders);
    }

    /**
     * 取消订单
     *
     * @param ordersCancelDTO
     */
    public void cancel(OrdersCancelDTO ordersCancelDTO) throws Exception {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(ordersCancelDTO.getId());

        // 支付状态
        Integer payStatus = ordersDB.getPayStatus();
        if (payStatus == 1) {
            // 用户已支付，需要退款
            String refund = weChatPayUtil.refund(
                    ordersDB.getNumber(),
                    ordersDB.getNumber(),
                    new BigDecimal(0.01),
                    new BigDecimal(0.01));
            log.info("申请退款：{}", refund);
        }

        // 管理端取消订单需要退款，根据订单id更新订单状态、取消原因、取消时间
        Orders orders = new Orders();
        orders.setId(ordersCancelDTO.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    /**
     * 派送订单
     *
     * @param id
     */
    public void delivery(Long id) {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(id);

        // 校验订单是否存在，并且状态为3
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        // 更新订单状态,状态转为派送中
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);

        orderMapper.update(orders);
    }

    /**
     * 完成订单
     *
     * @param id
     */
    public void complete(Long id) {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(id);

        // 校验订单是否存在，并且状态为4
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        // 更新订单状态,状态转为完成
        orders.setStatus(Orders.COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());

        orderMapper.update(orders);
    }

    /**
     * 用户催单
     *
     * @param id
     */
    public void reminder(Long id) {
        // 查询订单是否存在
        Orders orders = requireOwnedOrder(orderMapper.getById(id));

        // 基于WebSocket实现催单
        Map<String, Object> map = new HashMap<>();
        map.put("type", 2);// 2代表用户催单
        map.put("orderId", id);
        map.put("content", "订单号：" + orders.getNumber());
        webSocketServer.sendToAllClient(JSON.toJSONString(map));
    }
//cancel order
    @Override
    @Transactional
    public void userCancel(Long id) {
        Orders orders = requireOwnedOrder(orderMapper.getByIdForUpdate(id));
        if (Orders.CANCELLED.equals(orders.getStatus())) {
            return;
        }
        if (!Orders.PENDING_PAYMENT.equals(orders.getStatus())
                && !Orders.TO_BE_CONFIRMED.equals(orders.getStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        // 设置为已取消
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelTime(LocalDateTime.now());
        orders.setCancelReason("用户取消");
        if (Orders.PAID.equals(orders.getPayStatus())) {
            // 当前支付链路为模拟支付，仅记录模拟退款，不调用真实微信资金接口。
            orders.setPayStatus(Orders.REFUND);
        }
        orderMapper.update(orders);
    }

    private Long requireCurrentUserId() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new OrderBusinessException(MessageConstant.USER_NOT_LOGIN);
        }
        return userId;
    }

    private Orders requireOwnedOrder(Orders order) {
        Long userId = requireCurrentUserId();
        if (order == null || !userId.equals(order.getUserId())) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        return order;
    }

}
