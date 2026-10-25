package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ShoppingCartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ShoppingCartServiceImpl implements ShoppingCartService {
    @Autowired private ShoppingCartMapper shoppingCartMapper;
    @Autowired private DishMapper dishMapper;
    @Autowired private SetmealMapper setmealMapper;
    @Autowired private UserMapper userMapper;

    @Override
    @Transactional
    public void addShoppingCart(ShoppingCartDTO request) {
        ShoppingCart item = ownedItem(request, lockUser());
        ShoppingCart existing = shoppingCartMapper.findOwnedItem(item);
        if (existing != null) {
            if (existing.getNumber() == null || existing.getNumber() < 1 || existing.getNumber() >= 9999) {
                throw error("购物车数量必须为1至9999");
            }
            existing.setNumber(existing.getNumber() + 1);
            shoppingCartMapper.updateNumberById(existing);
            return;
        }
        if (item.getDishId() != null) {
            Dish dish = dishMapper.getById(item.getDishId());
            if (dish == null || !Integer.valueOf(1).equals(dish.getStatus())) throw error("商品不存在或已下架");
            item.setName(dish.getName()); item.setImage(dish.getImage()); item.setAmount(dish.getPrice());
        } else {
            Setmeal meal = setmealMapper.getById(item.getSetmealId());
            if (meal == null || !Integer.valueOf(1).equals(meal.getStatus())) throw error("套餐不存在或已下架");
            item.setName(meal.getName()); item.setImage(meal.getImage()); item.setAmount(meal.getPrice());
        }
        item.setNumber(1);
        item.setCreateTime(LocalDateTime.now());
        shoppingCartMapper.insert(item);
    }

    @Override
    public List<ShoppingCart> showShoppingCart() {
        return shoppingCartMapper.list(ShoppingCart.builder().userId(currentUser()).build());
    }

    @Override
    @Transactional
    public void cleanShoppingCart() {
        shoppingCartMapper.deleteByUserId(lockUser());
    }

    @Override
    @Transactional
    public void subShoppingCart(ShoppingCartDTO request) {
        Long userId = lockUser();
        ShoppingCart item = shoppingCartMapper.findOwnedItem(ownedItem(request, userId));
        if (item == null) return;
        if (item.getNumber() == null || item.getNumber() <= 1) {
            shoppingCartMapper.deleteById(item.getId(), userId);
        } else {
            item.setNumber(item.getNumber() - 1);
            shoppingCartMapper.updateNumberById(item);
        }
    }

    private ShoppingCart ownedItem(ShoppingCartDTO request, Long userId) {
        if (request == null || (request.getDishId() == null) == (request.getSetmealId() == null)
                || (request.getDishId() != null && request.getDishId() <= 0)
                || (request.getSetmealId() != null && request.getSetmealId() <= 0)) {
            throw error("必须且只能选择一个有效商品或套餐");
        }
        if (request.getDishFlavor() != null && request.getDishFlavor().length() > 255) throw error("商品口味过长");
        ShoppingCart item = new ShoppingCart();
        item.setUserId(userId); item.setDishId(request.getDishId()); item.setSetmealId(request.getSetmealId());
        item.setDishFlavor(request.getDishFlavor() == null || request.getDishFlavor().trim().isEmpty()
                ? null : request.getDishFlavor());
        return item;
    }

    private Long lockUser() {
        Long id = currentUser();
        if (userMapper.getByIdForUpdate(id) == null) throw error("用户不存在，请重新登录");
        return id;
    }

    private Long currentUser() {
        Long id = BaseContext.getCurrentId();
        if (id == null) throw error("请先登录");
        return id;
    }

    private ShoppingCartBusinessException error(String message) { return new ShoppingCartBusinessException(message); }
}