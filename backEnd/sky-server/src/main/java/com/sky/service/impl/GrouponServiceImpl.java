package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.GrouponJoinDTO;
import com.sky.dto.GrouponPageQueryDTO;
import com.sky.entity.AddressBook;
import com.sky.entity.Dish;
import com.sky.entity.Groupon;
import com.sky.entity.GrouponMember;
import com.sky.entity.OrderDetail;
import com.sky.entity.OrderStock;
import com.sky.entity.Orders;
import com.sky.entity.PickupPoint;
import com.sky.entity.User;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.GrouponBusinessException;
import com.sky.mapper.AddressBookMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.GrouponMapper;
import com.sky.mapper.GrouponMemberMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.OrderStockMapper;
import com.sky.mapper.PickupPointMapper;
import com.sky.mapper.UserMapper;
import com.sky.result.PageResult;
import com.sky.service.GrouponService;
import com.sky.vo.GrouponVO;
import com.sky.websocket.WebSocketServer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 社区团购业务实现
 * <p>
 * 拼团状态机：
 * 1 拼团中 --(人满)--> 2 已成团
 * 1 拼团中 --(超时)--> 4 已过期
 * 3 已失败 保留给「未成团且被主动取消」的场景
 */
@Service
@Slf4j
public class GrouponServiceImpl implements GrouponService {

    /**
     * 默认成团人数
     */
    private static final int DEFAULT_GROUPON_SIZE = 2;

    /**
     * 默认拼团有效时长（小时）
     */
    private static final int DEFAULT_VALID_HOURS = 24;

    @Autowired
    private GrouponMapper grouponMapper;

    @Autowired
    private GrouponMemberMapper grouponMemberMapper;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private OrderStockMapper orderStockMapper;

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Autowired
    private PickupPointMapper pickupPointMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WebSocketServer webSocketServer;

    /**
     * 拼团商品列表
     */
    @Override
    public List<Dish> listGrouponDish(String keyword) {
        List<Dish> list = dishMapper.listGrouponDish();
        if (keyword == null || keyword.trim().isEmpty()) {
            return list;
        }
        String kw = keyword.trim();
        List<Dish> filtered = new ArrayList<>();
        for (Dish d : list) {
            if (d.getName() != null && d.getName().contains(kw)) {
                filtered.add(d);
            }
        }
        return filtered;
    }

    /**
     * 开团
     */
    @Override
    @Transactional
    public Groupon openGroupon(GrouponJoinDTO dto) {
        validateQuantity(dto);
        if (dto.getDishId() == null) {
            throw new GrouponBusinessException("请选择要拼团的商品");
        }
        if (dto.getGrouponId() != null) throw new GrouponBusinessException("开团时不能指定已有拼团");

        Long userId = authenticatedUser();
        if (userMapper.getByIdForUpdate(userId) == null) throw new GrouponBusinessException("用户不存在，请重新登录");
        Groupon previous = previousSubmission(dto, "groupon-open");
        if (previous != null) return previous;
        Dish dish = dishMapper.getById(dto.getDishId());
        checkDishJoinable(dish);
        checkStock(dish, dto.getNumber());

        int requiredSize = dish.getGrouponSize() == null || dish.getGrouponSize() < 2
                ? DEFAULT_GROUPON_SIZE : dish.getGrouponSize();
        int validHours = dish.getGrouponValidHours() == null || dish.getGrouponValidHours() <= 0
                ? DEFAULT_VALID_HOURS : dish.getGrouponValidHours();

        BigDecimal price = dish.getGrouponPrice() != null ? dish.getGrouponPrice() : dish.getPrice();
        int number = dto.getNumber() == null || dto.getNumber() < 1 ? 1 : dto.getNumber();

        LocalDateTime now = now();

        // 1. 创建拼团活动，团长即为当前用户
        Groupon groupon = Groupon.builder()
                .dishId(dish.getId())
                .leaderUserId(userId)
                .grouponPrice(price)
                .requiredSize(requiredSize)
                .currentSize(1)
                .status(Groupon.IN_PROGRESS)
                .expireTime(now.plusHours(validHours))
                .pickupPointId(dto.getPickupPointId())
                .createTime(now)
                .updateTime(now)
                .build();
        grouponMapper.insert(groupon);

        // 2. 团长生成订单
        Long orderId = createGrouponOrder(dto, dish, price, number, groupon.getId());

        // 3. 记录团长成员信息
        GrouponMember member = GrouponMember.builder()
                .grouponId(groupon.getId())
                .userId(userId)
                .orderId(orderId)
                .isLeader(1)
                .number(number)
                .amount(price.multiply(BigDecimal.valueOf(number)))
                .createTime(now)
                .build();
        grouponMemberMapper.insert(member);

        // 4. 扣减库存
        deductStock(dish, number, orderId);

        log.info("用户{}开团成功，拼团id={}，商品={}，需{}人成团",
                userId, groupon.getId(), dish.getName(), requiredSize);

        // 5. 若成团人数为1（极端配置），立即成团
        tryComplete(groupon.getId());

        Groupon result = grouponMapper.getById(groupon.getId());
        result.setOrderId(orderId);
        return result;
    }

    /**
     * 参团
     */
    @Override
    @Transactional
    public Groupon joinGroupon(GrouponJoinDTO dto) {
        validateQuantity(dto);
        if (dto.getGrouponId() == null) {
            throw new GrouponBusinessException("请选择要参与的拼团");
        }

        Long userId = authenticatedUser();
        if (userMapper.getByIdForUpdate(userId) == null) throw new GrouponBusinessException("用户不存在，请重新登录");
        Groupon previous = previousSubmission(dto, "groupon-join");
        if (previous != null) return previous;
        Groupon groupon = grouponMapper.getByIdForUpdate(dto.getGrouponId());
        if (groupon == null) {
            throw new GrouponBusinessException(MessageConstant.GROUPON_NOT_FOUND);
        }

        // 校验团状态
        if (Groupon.EXPIRED.equals(groupon.getStatus())) {
            throw new GrouponBusinessException(MessageConstant.GROUPON_EXPIRED);
        }
        if (!Groupon.IN_PROGRESS.equals(groupon.getStatus())
                || groupon.getCurrentSize() >= groupon.getRequiredSize()) {
            throw new GrouponBusinessException(MessageConstant.GROUPON_NOT_JOINABLE);
        }
        if (groupon.getExpireTime() == null || !groupon.getExpireTime().isAfter(now())) {
            throw new GrouponBusinessException(MessageConstant.GROUPON_EXPIRED);
        }

        // 团长不能重复参团
        if (userId.equals(groupon.getLeaderUserId())) {
            throw new GrouponBusinessException(MessageConstant.GROUPON_ALREADY_JOINED);
        }

        // 同一用户不能重复参同一个团
        GrouponMember exist = grouponMemberMapper.getByGrouponIdAndUserId(groupon.getId(), userId);
        if (exist != null) {
            throw new GrouponBusinessException(MessageConstant.GROUPON_ALREADY_JOINED);
        }

        Dish dish = dishMapper.getById(groupon.getDishId());
        checkDishJoinable(dish);
        checkStock(dish, dto.getNumber());

        int number = dto.getNumber() == null || dto.getNumber() < 1 ? 1 : dto.getNumber();
        BigDecimal price = groupon.getGrouponPrice();
        LocalDateTime now = now();

        // 1. 原子占位：并发下只有 current_size < required_size 的请求能成功
        int affected = grouponMapper.increaseCurrentSize(groupon.getId(), now());
        if (affected == 0) {
            throw new GrouponBusinessException(MessageConstant.GROUPON_NOT_JOINABLE);
        }

        // 2. 生成参团订单
        Long orderId = createGrouponOrder(dto, dish, price, number, groupon.getId());

        // 3. 记录成员
        GrouponMember member = GrouponMember.builder()
                .grouponId(groupon.getId())
                .userId(userId)
                .orderId(orderId)
                .isLeader(0)
                .number(number)
                .amount(price.multiply(BigDecimal.valueOf(number)))
                .createTime(now)
                .build();
        grouponMemberMapper.insert(member);

        // 4. 扣减库存
        deductStock(dish, number, orderId);

        log.info("用户{}参团成功，拼团id={}，当前人数={}/{}",
                userId, groupon.getId(), groupon.getCurrentSize() + 1, groupon.getRequiredSize());

        // 5. 判断是否成团
        tryComplete(groupon.getId());

        Groupon result = grouponMapper.getById(groupon.getId());
        result.setOrderId(orderId);
        return result;
    }

    /**
     * 校验并成团
     */
    @Override
    @Transactional
    public boolean tryComplete(Long grouponId) {
        Groupon groupon = grouponMapper.getByIdForUpdate(grouponId);
        if (groupon == null || !Groupon.IN_PROGRESS.equals(groupon.getStatus())) {
            return false;
        }
        if (groupon.getCurrentSize() < groupon.getRequiredSize()) {
            return false;
        }
        if (groupon.getExpireTime() == null || !groupon.getExpireTime().isAfter(now())) return false;

        LocalDateTime now = now();
        Groupon update = Groupon.builder()
                .id(groupon.getId())
                .status(Groupon.SUCCESS)
                .completeTime(now)
                .updateTime(now)
                .build();
        grouponMapper.update(update);

        log.info("拼团id={} 已成团，共{}人", groupon.getId(), groupon.getCurrentSize());

        // 成团后通知商家备货（复用外卖的 WebSocket 来单提醒通道）
        Map<String, Object> map = new HashMap<>();
        map.put("type", 3); // 3:拼团成团通知
        map.put("grouponId", groupon.getId());
        map.put("content", "拼团成团，商品id: " + groupon.getDishId() + "，共" + groupon.getCurrentSize() + "人");
        webSocketServer.sendToAllClient(JSON.toJSONString(map));

        return true;
    }

    /**
     * 拼团详情
     */
    @Override
    public GrouponVO getDetail(Long grouponId) {
        Groupon groupon = grouponMapper.getById(grouponId);
        if (groupon == null) {
            throw new GrouponBusinessException(MessageConstant.GROUPON_NOT_FOUND);
        }
        return toVO(groupon, true);
    }

    /**
     * 某商品可加入的团列表
     */
    @Override
    public List<GrouponVO> listJoinable(Long dishId) {
        List<Groupon> list = grouponMapper.listJoinableByDishId(dishId);
        List<GrouponVO> result = new ArrayList<>();
        for (Groupon g : list) {
            result.add(toVO(g, false));
        }
        return result;
    }

    /**
     * 我的拼团
     */
    @Override
    public List<GrouponVO> listMine(Integer status) {
        Long userId = authenticatedUser();
        List<Groupon> list = grouponMapper.listMine(userId, status);
        List<GrouponVO> result = new ArrayList<>();
        for (Groupon g : list) {
            result.add(toVO(g, true));
        }
        return result;
    }

    /**
     * 关闭超时未成团的拼团
     */
    @Override
    @Transactional
    public int closeExpired() {
        int count = 0;
        for (Groupon candidate : grouponMapper.listExpired(now())) {
            Groupon locked = grouponMapper.getByIdForUpdate(candidate.getId());
            if (locked != null && Groupon.IN_PROGRESS.equals(locked.getStatus())
                    && locked.getExpireTime() != null && !locked.getExpireTime().isAfter(now())) {
                closeGroup(locked, Groupon.EXPIRED, "拼团超时未成团，自动取消");
                count++;
            }
        }
        return count;
    }

    @Override
    @Transactional
    public void cancelOrderGroup(Long grouponId, String reason) {
        Groupon locked = grouponMapper.getByIdForUpdate(grouponId);
        if (locked == null) throw new GrouponBusinessException(MessageConstant.GROUPON_NOT_FOUND);
        if (Groupon.SUCCESS.equals(locked.getStatus())) {
            throw new GrouponBusinessException("已成团订单不能取消");
        }
        if (!Groupon.IN_PROGRESS.equals(locked.getStatus())) return;
        closeGroup(locked, Groupon.FAILED, reason);
    }

    private void closeGroup(Groupon locked, Integer finalStatus, String reason) {
        List<GrouponMember> members = grouponMemberMapper.listByGrouponId(locked.getId());
        members.sort(java.util.Comparator.comparing(GrouponMember::getOrderId,
                java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())));
        for (GrouponMember member : members) {
            if (member.getOrderId() == null) continue;
            Orders order = orderMapper.getByIdForUpdate(member.getOrderId());
            if (order == null || Orders.CANCELLED.equals(order.getStatus())) continue;
            if (!Orders.PENDING_PAYMENT.equals(order.getStatus()) && !Orders.TO_BE_CONFIRMED.equals(order.getStatus())) {
                throw new GrouponBusinessException("拼团订单状态不允许关闭");
            }
            Orders update = new Orders();
            update.setId(order.getId());
            update.setStatus(Orders.CANCELLED);
            update.setCancelReason(reason);
            update.setCancelTime(now());
            if (Orders.PAID.equals(order.getPayStatus())) update.setPayStatus(Orders.REFUND);
            orderMapper.update(update);
            for (OrderStock reserved : orderStockMapper.list(order.getId())) {
                restoreStock(reserved.getDishId(), reserved.getNumber());
            }
        }
        grouponMapper.update(Groupon.builder().id(locked.getId()).status(finalStatus).updateTime(now()).build());
    }
    /**
     * 管理端分页查询拼团活动：
     * SQL 一次 JOIN 取齐商品/自提点/团长信息（避免 N+1），
     * lackSize 与 remainMillis 由服务层按行补齐。
     */
    @Override
    public PageResult<GrouponVO> getAdminPage(GrouponPageQueryDTO pageQueryDTO) {
        PageHelper.startPage(pageQueryDTO.getPage(), pageQueryDTO.getPageSize());
        List<GrouponVO> list = grouponMapper.pageQuery(pageQueryDTO);
        PageInfo<GrouponVO> pageInfo = new PageInfo<>(list);
        for (GrouponVO vo : list) {
            int current = vo.getCurrentSize() == null ? 0 : vo.getCurrentSize();
            int required = vo.getRequiredSize() == null ? 0 : vo.getRequiredSize();
            vo.setLackSize(Math.max(required - current, 0));
            if (vo.getExpireTime() != null) {
                long millis = Duration.between(now(), vo.getExpireTime()).toMillis();
                vo.setRemainMillis(Math.max(millis, 0));
            }
        }
        return new PageResult<>(pageInfo.getTotal(), list);
    }

    // ==================== 私有方法 ====================

    private void validateQuantity(GrouponJoinDTO dto) {
        if (dto == null || dto.getNumber() == null || dto.getNumber() < 1) {
            throw new GrouponBusinessException("购买数量必须为正整数");
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai"));
    }

    private Long authenticatedUser() {
        Long id = BaseContext.getCurrentId();
        if (id == null) throw new GrouponBusinessException("请先登录");
        return id;
    }

    private Groupon previousSubmission(GrouponJoinDTO dto, String operation) {
        String key = dto.getClientRequestId();
        if (key == null) return null;
        if (key.trim().isEmpty() || key.length() > 64) {
            throw new GrouponBusinessException("请求标识不能为空且不能超过64字符");
        }
        Orders previous = orderMapper.getByClientRequestId(authenticatedUser(), key);
        if (previous == null) return null;
        if (!requestHash(dto, operation).equals(previous.getRequestHash())
                || !Integer.valueOf(1).equals(previous.getIsGroupon())) {
            throw new GrouponBusinessException("请求标识已用于不同订单");
        }
        Groupon group = grouponMapper.getById(previous.getGrouponId());
        if (group == null) throw new GrouponBusinessException(MessageConstant.GROUPON_NOT_FOUND);
        group.setOrderId(previous.getId());
        return group;
    }

    private String requestHash(GrouponJoinDTO dto, String operation) {
        String payload = JSON.toJSONString(java.util.Arrays.asList(operation,
                "groupon-open".equals(operation) ? dto.getDishId() : dto.getGrouponId(),
                dto.getNumber(), dto.getAddressBookId(), dto.getPickupPointId(),
                dto.getDeliveryType(), dto.getPickupPersonName(), dto.getPickupPersonPhone(),
                dto.getPickupTimeSlot(), dto.getRemark()));
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte value : digest) hex.append(String.format("%02x", value));
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    /**
     * 生成拼团关联订单（复用现有 orders / order_detail 结构）
     */
    private Long createGrouponOrder(GrouponJoinDTO dto, Dish dish, BigDecimal price, int number, Long grouponId) {
        Long userId = authenticatedUser();

        boolean pickup = dto.getPickupPointId() != null;
        if (pickup == (dto.getAddressBookId() != null)) {
            throw new GrouponBusinessException("必须且只能选择收货地址或自提点");
        }
        int deliveryType = pickup ? 2 : 1;
        if (dto.getDeliveryType() != null && !Integer.valueOf(deliveryType).equals(dto.getDeliveryType())) {
            throw new GrouponBusinessException("配送方式与履约信息不一致");
        }

        String address = null;
        String phone = null;
        String consignee = null;
        String pickupPointName = null;

        // 优先使用自提点；无自提点则要求收货地址（支持配送到家）
        if (dto.getPickupPointId() != null) {
            PickupPoint point = pickupPointMapper.getById(dto.getPickupPointId());
            if (point == null || !Integer.valueOf(1).equals(point.getStatus())) {
                throw new GrouponBusinessException(MessageConstant.PICKUP_POINT_NOT_FOUND);
            }
            if (dto.getPickupPersonName() == null || dto.getPickupPersonName().trim().isEmpty()
                    || dto.getPickupPersonName().length() > 32
                    || dto.getPickupPersonPhone() == null || !dto.getPickupPersonPhone().matches("1[3-9]\\d{9}")) {
                throw new GrouponBusinessException("请填写有效的提货人姓名和手机号");
            }
            // community / address 允许为空，避免拼出 "null null（自提点：xx）"
            String community = point.getCommunity() == null ? "" : point.getCommunity();
            String pointAddr = point.getAddress() == null ? "" : point.getAddress();
            address = (community + " " + pointAddr).trim() + "（自提点：" + point.getName() + "）";
            phone = dto.getPickupPersonPhone();
            consignee = dto.getPickupPersonName().trim();
            pickupPointName = point.getName();
        } else {
            if (dto.getAddressBookId() == null) {
                throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
            }
            AddressBook addressBook = addressBookMapper.getById(dto.getAddressBookId());
            if (addressBook == null || !userId.equals(addressBook.getUserId())) {
                // 校验归属，避免越权使用他人收货地址
                throw new AddressBookBusinessException("收货地址不存在或不属于当前用户");
            }
            address = addressBook.getDetail();
            phone = addressBook.getPhone();
            consignee = addressBook.getConsignee();
        }

        BigDecimal amount = price.multiply(BigDecimal.valueOf(number));
        LocalDateTime now = now();

        Orders orders = new Orders();
        orders.setNumber("G" + java.util.UUID.randomUUID().toString().replace("-", ""));
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setPayStatus(Orders.UN_PAID);
        orders.setUserId(userId);
        // orders.address_book_id 为 NOT NULL，自提订单无收货地址时用 0 占位
        orders.setAddressBookId(dto.getAddressBookId() == null ? 0L : dto.getAddressBookId());
        orders.setPickupPointId(dto.getPickupPointId());
        orders.setPickupPointName(pickupPointName);
        orders.setPickupPersonName(pickup ? consignee : null);
        orders.setPickupPersonPhone(pickup ? phone : null);
        orders.setPickupTimeSlot(pickup ? dto.getPickupTimeSlot() : null);
        orders.setIsGroupon(1);
        orders.setGrouponId(grouponId);
        orders.setClientRequestId(dto.getClientRequestId());
        orders.setRequestHash(requestHash(dto, dto.getGrouponId() == null ? "groupon-open" : "groupon-join"));
        orders.setOrderTime(now);
        orders.setAmount(amount);
        orders.setRemark(dto.getRemark());
        orders.setAddress(address);
        orders.setPhone(phone);
        orders.setConsignee(consignee);
        // 以下三个字段在 orders 表中是 NOT NULL：显式插入 null 不会走 DEFAULT，
        // 会直接抛完整性约束异常，因此必须赋默认值（与表 DEFAULT 保持一致）。
        orders.setPayMethod(1);              // 1 微信
        orders.setDeliveryStatus(1);         // 1 立即送出
        orders.setTablewareStatus(1);        // 1 按餐量提供
        orders.setPackAmount(0);
        orders.setTablewareNumber(0);
        orderMapper.insert(orders);

        // 订单明细：拼团商品按 dish 记录，名称加「拼团」标识便于区分。
        // 注意：order_detail.amount 的语义是「单价」而非总额，与购物车保持一致，
        // 前端用 number * amount 计算行小计；总额已记录在 orders.amount。
        OrderDetail detail = new OrderDetail();
        detail.setOrderId(orders.getId());
        detail.setDishId(dish.getId());
        detail.setName(dish.getName() + "（拼团）");
        detail.setImage(dish.getImage());
        detail.setNumber(number);
        detail.setAmount(price);
        orderDetailMapper.insert(detail);

        return orders.getId();
    }

    /**
     * 校验商品是否可拼团
     */
    private void checkDishJoinable(Dish dish) {
        if (dish == null) {
            throw new GrouponBusinessException(MessageConstant.DISH_NOT_SUPPORT_GROUPON);
        }
        if (!Integer.valueOf(1).equals(dish.getStatus())) {
            throw new GrouponBusinessException(MessageConstant.DISH_OFF_SALE);
        }
        if (!Integer.valueOf(1).equals(dish.getIsGroupon())) {
            throw new GrouponBusinessException(MessageConstant.DISH_NOT_SUPPORT_GROUPON);
        }
    }

    private void checkStock(Dish dish, Integer number) {
        int n = number == null || number < 1 ? 1 : number;
        if (dish.getStock() != null && dish.getStock() < n) {
            throw new GrouponBusinessException(MessageConstant.DISH_STOCK_NOT_ENOUGH);
        }
    }

    /**
     * 扣减库存（stock 为 null 视为不管理库存）。
     * 使用数据库原子更新，避免并发下「读-改-写」导致的超卖。
     */
    private void deductStock(Dish dish, Integer number, Long orderId) {
        if (dish.getStock() == null) {
            return;
        }
        int n = number == null || number < 1 ? 1 : number;
        int affected = dishMapper.deductStock(dish.getId(), n);
        if (affected == 0) {
            // 并发下库存已被他人扣完，抛异常回滚整个事务
            throw new GrouponBusinessException(MessageConstant.DISH_STOCK_NOT_ENOUGH);
        }
        OrderStock reservation = new OrderStock();
        reservation.setOrderId(orderId);
        reservation.setDishId(dish.getId());
        reservation.setNumber(n);
        orderStockMapper.insert(reservation);
    }

    /**
     * 归还库存（拼团超时/失败时调用）
     */
    private void restoreStock(Long dishId, Integer number) {
        if (dishId == null || number == null || number < 1) {
            return;
        }
        dishMapper.restoreStock(dishId, number);
    }

    /**
     * 实体转 VO
     *
     * @param withMembers 是否附带成员列表（列表页不需要，减少查询）
     */
    private GrouponVO toVO(Groupon groupon, boolean withMembers) {
        Dish dish = dishMapper.getById(groupon.getDishId());
        User leader = userMapper.getById(groupon.getLeaderUserId());

        int current = groupon.getCurrentSize() == null ? 0 : groupon.getCurrentSize();
        int required = groupon.getRequiredSize() == null ? 0 : groupon.getRequiredSize();
        int lack = Math.max(required - current, 0);

        Long remainMillis = null;
        if (groupon.getExpireTime() != null) {
            long millis = Duration.between(now(), groupon.getExpireTime()).toMillis();
            remainMillis = Math.max(millis, 0);
        }

        String pickupPointName = null;
        if (groupon.getPickupPointId() != null) {
            PickupPoint point = pickupPointMapper.getById(groupon.getPickupPointId());
            if (point != null) {
                pickupPointName = point.getName();
            }
        }

        GrouponVO vo = GrouponVO.builder()
                .id(groupon.getId())
                .dishId(groupon.getDishId())
                .dishName(dish == null ? null : dish.getName())
                .dishImage(dish == null ? null : dish.getImage())
                .originalPrice(dish == null ? null : dish.getPrice())
                .grouponPrice(groupon.getGrouponPrice())
                .unit(dish == null ? null : dish.getUnit())
                .origin(dish == null ? null : dish.getOrigin())
                .requiredSize(required)
                .currentSize(current)
                .lackSize(lack)
                .status(groupon.getStatus())
                .expireTime(groupon.getExpireTime())
                .remainMillis(remainMillis)
                .leaderName(leader == null ? null : leader.getName())
                .pickupPointName(pickupPointName)
                .build();

        Long currentUser = BaseContext.getCurrentId();
        if (currentUser != null) {
            GrouponMember own = grouponMemberMapper.getByGrouponIdAndUserId(groupon.getId(), currentUser);
            if (own != null) vo.setOrderId(own.getOrderId());
        }

        if (withMembers) {
            List<GrouponMember> members = grouponMemberMapper.listByGrouponId(groupon.getId());
            vo.setMembers(members.stream().map(m -> {
                User u = userMapper.getById(m.getUserId());
                return GrouponVO.MemberVO.builder()
                        .userId(m.getUserId())
                        .userName(u == null ? null : u.getName())
                        .avatar(u == null ? null : u.getAvatar())
                        .isLeader(m.getIsLeader())
                        .number(m.getNumber())
                        .joinTime(m.getCreateTime())
                        .build();
            }).collect(Collectors.toList()));
        }

        return vo;
    }
}
