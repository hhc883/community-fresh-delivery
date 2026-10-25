package com.sky.service;

import com.sky.context.BaseContext;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.*;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.*;
import com.sky.vo.OrderSubmitVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/** One checkout boundary for direct purchases and the persisted shopping cart. */
@Service
public class CheckoutService {
    @Autowired private OrderMapper orders;
    @Autowired private OrderDetailMapper details;
    @Autowired private DishMapper dishes;
    @Autowired private SetmealMapper setmeals;
    @Autowired private ShoppingCartMapper carts;
    @Autowired private PickupPointMapper points;
    @Autowired private AddressBookService addresses;
    @Autowired private UserMapper users;
    @Autowired private SetmealDishMapper compositions;
    @Autowired private OrderStockMapper stockSnapshots;

    @Transactional
    public OrderSubmitVO submit(OrdersSubmitDTO request) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) throw error("请先登录");
        String key = request.getClientRequestId();
        if (key == null || !key.matches("[A-Za-z0-9_-]{1,64}")) throw error("请提供有效的 clientRequestId");
        String hash = requestHash(request);
        // 同用户的下单意图串行；不同用户仍由商品库存条件更新竞争库存。
        if (users.getByIdForUpdate(userId) == null) throw error("用户不存在，请重新登录");
        Orders existing = orders.getByClientRequestId(userId, key);
        if (existing != null) {
            if (!hash.equals(existing.getRequestHash())) throw error("幂等键已用于不同的下单请求");
            return result(existing);
        }
        Orders order = new Orders();
        order.setClientRequestId(key);
        order.setRequestHash(hash);
        setFulfillment(request, order);
        List<Map<String, Object>> items = requestedItems(request, userId);
        List<OrderDetail> lines = new ArrayList<>();
        Map<Long, Integer> reservations = new TreeMap<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> item : items) {
            Long dishId = id(item.get("dishId"));
            Long setmealId = id(item.get("setmealId"));
            if ((dishId == null) == (setmealId == null)) throw error("每行必须选择一个商品或套餐");
            int quantity = positiveInteger(item.containsKey("number") ? item.get("number") : item.get("quantity"));
            OrderDetail line = new OrderDetail();
            line.setDishId(dishId);
            line.setSetmealId(setmealId);
            line.setNumber(quantity);
            if (dishId != null) {
                Dish dish = dishes.getById(dishId);
                if (dish == null || !Integer.valueOf(1).equals(dish.getStatus())) throw error("商品不存在或已下架");
                line.setName(dish.getName());
                line.setImage(dish.getImage());
                line.setAmount(price(dish.getPrice()));
                addReservation(reservations, dish, quantity);
            } else {
                Setmeal setmeal = setmeals.getById(setmealId);
                if (setmeal == null || !Integer.valueOf(1).equals(setmeal.getStatus())) throw error("套餐不存在或已下架");
                line.setName(setmeal.getName());
                line.setImage(setmeal.getImage());
                line.setAmount(price(setmeal.getPrice()));
                List<SetmealDish> components = compositions.listBySetmealId(setmealId);
                if (components.isEmpty()) throw error("套餐未配置商品");
                for (SetmealDish component : components) {
                    int copies = positiveInteger(component.getCopies());
                    addReservation(reservations, dishes.getById(component.getDishId()), Math.multiplyExact(quantity, copies));
                }
            }
            total = total.add(line.getAmount().multiply(BigDecimal.valueOf(quantity)));
            lines.add(line);
        }
        if (total.compareTo(new BigDecimal("99999999.99")) > 0) throw error("订单金额超过限制");
        // 按商品 ID 排序取库存行锁，避免含多个商品的反向购买形成死锁。
        for (Map.Entry<Long, Integer> reservation : reservations.entrySet()) {
            if (dishes.deductStock(reservation.getKey(), reservation.getValue()) != 1) throw error("商品库存不足或已下架");
        }
        order.setUserId(userId);
        order.setNumber(UUID.randomUUID().toString().replace("-", ""));
        order.setOrderTime(LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai")));
        order.setStatus(Orders.PENDING_PAYMENT);
        order.setPayStatus(Orders.UN_PAID);
        order.setPayMethod(1);
        order.setAmount(total.setScale(2, RoundingMode.UNNECESSARY));
        order.setRemark(request.getRemark());
        order.setPackAmount(0);
        order.setTablewareNumber(0);
        order.setTablewareStatus(1);
        order.setDeliveryStatus(1);
        order.setIsGroupon(0);
        order.setVerifyStatus(Orders.UN_VERIFIED);
        order.setEstimatedDeliveryTime(request.getEstimatedDeliveryTime());
        orders.insert(order);
        for (Map.Entry<Long, Integer> reservation : reservations.entrySet()) {
            OrderStock snapshot = new OrderStock();
            snapshot.setOrderId(order.getId()); snapshot.setDishId(reservation.getKey()); snapshot.setNumber(reservation.getValue());
            stockSnapshots.insert(snapshot);
        }
        for (OrderDetail line : lines) line.setOrderId(order.getId());
        details.insertBatch(lines);
        if (request.getItems() == null && request.getOrderDetailList() == null) carts.deleteByUserId(userId);
        return result(order);
    }

    private void setFulfillment(OrdersSubmitDTO request, Orders order) {
        boolean pickup = request.getPickupPointId() != null;
        if (pickup == (request.getAddressBookId() != null)) throw error("请选择收货地址或自提点，不能同时选择");
        int type = pickup ? 2 : 1;
        if (request.getDeliveryType() != null && request.getDeliveryType() != type) throw error("履约方式与地址不一致");
        if (pickup) {
            PickupPoint point = points.getById(request.getPickupPointId());
            if (point == null || !Integer.valueOf(1).equals(point.getStatus())) throw error("自提点不存在或已停用");
            if (!StringUtils.hasText(request.getPickupPersonName()) || request.getPickupPersonName().length() > 32
                    || request.getPickupPersonPhone() == null || !request.getPickupPersonPhone().matches("1[3-9]\\d{9}")) {
                throw error("请填写有效的提货人姓名和手机号");
            }
            order.setAddressBookId(0L);
            order.setPickupPointId(point.getId());
            order.setPickupPointName(point.getName());
            order.setAddress(point.getAddress());
            order.setConsignee(request.getPickupPersonName().trim());
            order.setPhone(request.getPickupPersonPhone());
            order.setPickupPersonName(order.getConsignee());
            order.setPickupPersonPhone(order.getPhone());
            order.setPickupTimeSlot(request.getPickupTimeSlot());
        } else {
            AddressBook address = addresses.getById(request.getAddressBookId());
            order.setAddressBookId(address.getId());
            order.setConsignee(address.getConsignee());
            order.setPhone(address.getPhone());
            order.setAddress(join(address.getProvinceName(), address.getCityName(), address.getDistrictName(), address.getDetail()));
        }
    }

    private List<Map<String, Object>> requestedItems(OrdersSubmitDTO request, Long userId) {
        List<Map<String, Object>> items = request.getItems() != null ? request.getItems() : request.getOrderDetailList();
        if (items == null) {
            ShoppingCart query = new ShoppingCart(); query.setUserId(userId);
            items = new ArrayList<>();
            for (ShoppingCart cart : carts.list(query)) {
                Map<String, Object> item = new HashMap<>();
                item.put("dishId", cart.getDishId()); item.put("setmealId", cart.getSetmealId());
                item.put("number", cart.getNumber()); items.add(item);
            }
        }
        if (items.isEmpty() || items.size() > 100) throw error("商品列表为空或超过100行");
        return items;
    }

    private Long id(Object value) {
        if (value == null) return null;
        try { long id = new BigDecimal(value.toString()).longValueExact(); if (id > 0) return id; }
        catch (RuntimeException ignored) { }
        throw error("商品编号无效");
    }

    private void addReservation(Map<Long, Integer> reservations, Dish dish, int quantity) {
        if (dish == null || !Integer.valueOf(1).equals(dish.getStatus())) throw error("商品不存在或已下架");
        if (dish.getStock() != null) reservations.merge(dish.getId(), quantity, Math::addExact);
    }

    private int positiveInteger(Object value) {
        try { int n = new BigDecimal(String.valueOf(value)).intValueExact(); if (n > 0 && n <= 9999) return n; }
        catch (RuntimeException ignored) { }
        throw error("商品数量必须是1至9999的整数");
    }

    private BigDecimal price(BigDecimal price) {
        if (price == null || price.signum() <= 0) throw error("商品价格无效");
        return price.setScale(2, RoundingMode.UNNECESSARY);
    }

    private String requestHash(OrdersSubmitDTO request) {
        Map<String, Object> intent = new TreeMap<>();
        intent.put("operation", "ordinary-checkout");
        intent.put("address", request.getAddressBookId()); intent.put("pickup", request.getPickupPointId());
        intent.put("deliveryType", request.getDeliveryType() != null ? request.getDeliveryType() : (request.getPickupPointId() == null ? 1 : 2));
        intent.put("recipient", request.getPickupPersonName()); intent.put("phone", request.getPickupPersonPhone());
        intent.put("slot", request.getPickupTimeSlot()); intent.put("remark", request.getRemark());
        intent.put("estimatedDeliveryTime", request.getEstimatedDeliveryTime() == null ? null : request.getEstimatedDeliveryTime().toString());
        List<Map<String, Object>> items = request.getItems() != null ? request.getItems() : request.getOrderDetailList();
        List<String> normalized = new ArrayList<>();
        if (items != null) {
            for (Map<String, Object> item : items) {
                normalized.add(String.valueOf(id(item.get("dishId"))) + ":" + id(item.get("setmealId")) + ":"
                        + positiveInteger(item.containsKey("number") ? item.get("number") : item.get("quantity")));
            }
            Collections.sort(normalized);
            intent.put("items", normalized);
        } else intent.put("items", "persisted-cart");
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256").digest(
                    com.alibaba.fastjson.JSON.toJSONString(intent).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) hex.append(String.format("%02x", b & 255));
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }

    private String join(String... parts) {
        StringBuilder result = new StringBuilder();
        for (String part : parts) if (StringUtils.hasText(part)) result.append(part);
        return result.toString();
    }

    private OrderSubmitVO result(Orders order) {
        return OrderSubmitVO.builder().id(order.getId()).orderNumber(order.getNumber())
                .orderAmount(order.getAmount()).amount(order.getAmount()).orderTime(order.getOrderTime()).build();
    }

    private OrderBusinessException error(String message) { return new OrderBusinessException(message); }
}
