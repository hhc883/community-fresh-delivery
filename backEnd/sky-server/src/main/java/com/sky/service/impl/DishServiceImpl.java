package com.sky.service.impl;

import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.ProductBusinessException;
import com.sky.mapper.CategoryMapper;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.OrderStockMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class DishServiceImpl implements DishService {
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private OrderStockMapper orderStockMapper;

    /**
     * 新增菜品以及保存口味
     *
     * @param dishDTO
     */
    @Override
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {
        validateProduct(dishDTO);

        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        if (dish.getStatus() == null) dish.setStatus(StatusConstant.DISABLE);
        if (dish.getStock() == null) dish.setStock(0);
        if (dish.getIsFresh() == null) dish.setIsFresh(1);
        if (dish.getIsGroupon() == null) dish.setIsGroupon(0);

        // 向菜品表插入一条记录
        dishMapper.insert(dish);

        // 获取insert生成的主键id
        Long dishId = dish.getId();

        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && !flavors.isEmpty()) {
            for (DishFlavor flavor : flavors) {
                // 向口味表插入n个数据
                flavor.setDishId(dishId);
            }
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 商品创建的统一校验：必填字段、非负库存与拼团参数。
     */
    private void validateProduct(DishDTO dishDTO) {
        validateCommon(dishDTO);
        if (Integer.valueOf(1).equals(dishDTO.getIsGroupon())) {
            // 新建即进入拼团模式：三个团参数在载荷里必须齐全合法
            validateGrouponMode(dishDTO.getGrouponPrice(), dishDTO.getGrouponSize(), dishDTO.getGrouponValidHours());
        } else {
            validateGrouponScalars(dishDTO);
        }
    }

    /**
     * 商品修改的统一校验。MyBatis update 对 null 保留原值，所以部分更新必须按
     * 「载荷值优先、库中原值兜底」的合并结果校验，否则省略 isGroupon 就能绕过团参数检查。
     */
    private void validateProductUpdate(DishDTO dishDTO, Dish existing) {
        validateCommon(dishDTO);
        Integer effectiveGroupon = dishDTO.getIsGroupon() != null ? dishDTO.getIsGroupon() : existing.getIsGroupon();
        if (Integer.valueOf(1).equals(effectiveGroupon)) {
            BigDecimal price = dishDTO.getGrouponPrice() != null ? dishDTO.getGrouponPrice() : existing.getGrouponPrice();
            Integer size = dishDTO.getGrouponSize() != null ? dishDTO.getGrouponSize() : existing.getGrouponSize();
            Integer hours = dishDTO.getGrouponValidHours() != null ? dishDTO.getGrouponValidHours() : existing.getGrouponValidHours();
            validateGrouponMode(price, size, hours);
        } else {
            validateGrouponScalars(dishDTO);
        }
    }

    /** 名称/分类/价格必填且合法，库存若提供则非负。 */
    private void validateCommon(DishDTO dishDTO) {
        if (dishDTO == null || dishDTO.getName() == null || dishDTO.getName().trim().isEmpty()) {
            throw new ProductBusinessException(MessageConstant.DISH_NAME_REQUIRED);
        }
        if (dishDTO.getCategoryId() == null || categoryMapper.getById(dishDTO.getCategoryId()) == null) {
            throw new ProductBusinessException(MessageConstant.DISH_CATEGORY_INVALID);
        }
        if (dishDTO.getPrice() == null || dishDTO.getPrice().signum() <= 0) {
            throw new ProductBusinessException(MessageConstant.DISH_PRICE_INVALID);
        }
        if (dishDTO.getStock() != null && dishDTO.getStock() < 0) {
            throw new ProductBusinessException(MessageConstant.DISH_STOCK_INVALID);
        }
    }

    /** 拼团参数一旦在载荷中出现就必须合法，与是否处于拼团模式无关，垃圾值不得入库。 */
    private void validateGrouponScalars(DishDTO dishDTO) {
        if (dishDTO.getGrouponPrice() != null && dishDTO.getGrouponPrice().signum() <= 0) {
            throw new ProductBusinessException(MessageConstant.DISH_GROUPON_PRICE_INVALID);
        }
        if (dishDTO.getGrouponSize() != null && dishDTO.getGrouponSize() < 2) {
            throw new ProductBusinessException(MessageConstant.DISH_GROUPON_SIZE_INVALID);
        }
        if (dishDTO.getGrouponValidHours() != null && dishDTO.getGrouponValidHours() <= 0) {
            throw new ProductBusinessException(MessageConstant.DISH_GROUPON_HOURS_INVALID);
        }
    }

    /**
     * 拼团模式下的最终参数校验。部分更新允许由库中原值补齐载荷省略的参数，
     * 但合并结果缺失或非法即拒绝——不能让「isGroupon=1 却没有成团人数」这类半截状态落库。
     */
    private void validateGrouponMode(BigDecimal price, Integer size, Integer hours) {
        if (price == null || price.signum() <= 0) {
            throw new ProductBusinessException(MessageConstant.DISH_GROUPON_PRICE_INVALID);
        }
        if (size == null || size < 2) {
            throw new ProductBusinessException(MessageConstant.DISH_GROUPON_SIZE_INVALID);
        }
        if (hours == null || hours <= 0) {
            throw new ProductBusinessException(MessageConstant.DISH_GROUPON_HOURS_INVALID);
        }
    }

    /**
     * 菜品分页查询
     *
     * @param dishPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());

        List<DishVO> dishVOList = dishMapper.pageQuery(dishPageQueryDTO);

        PageResult pageResult = new PageResult();
        // PageHelper 拦截后 dishVOList 实际是 Page：total 必须取分页器统计的全量行数，
        // 取 size() 只会得到当前页行数，管理端分页器会显示错误总数
        pageResult.setTotal(new com.github.pagehelper.PageInfo<>(dishVOList).getTotal());
        pageResult.setRecords(dishVOList);

        return pageResult;
    }

    /**
     * 分页查询菜品
     *
     * @param dishPageQueryDTO
     */
    // @Override
    // public PageResult<DishVO> queryPage(DishPageQueryDTO dishPageQueryDTO) {
    //     return null;
    // }

    /**
     * 删除菜品
     *
     * @param ids
     */
    @Override
    // 事务一致性
    @Transactional
    public void deleteBatch(Long[] ids) {
        // 判断菜品是否存在、是否在售、是否仍被进行中订单预留库存
        for (Long id: ids) {
            Dish dish = dishMapper.getById(id);
            if (dish == null) {
                throw new DeletionNotAllowedException(MessageConstant.DISH_NOT_FOUND);
            }
            if (Objects.equals(dish.getStatus(), StatusConstant.ENABLE)) {
                // 正在销售的菜品不能删除
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
            if (orderStockMapper.countActiveReservations(id) > 0) {
                throw new DeletionNotAllowedException(MessageConstant.DISH_RESERVED_BY_ACTIVE_ORDER);
            }
        }

        // 判断菜品是否被套餐关联
        List<Long> setmealIdsByDishIds = setmealDishMapper.getSetmealIdsByDishIds(Arrays.asList(ids));
        if (setmealIdsByDishIds != null && !setmealIdsByDishIds.isEmpty()) {
            // 菜品被套餐关联，不能删除
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

        // 删除菜品表的菜品数据
        for (Long id: ids) {
            dishMapper.deleteById(id);

            // 删除菜品关联的口味数据
            dishFlavorMapper.deleteByDishId(id);
        }
    }

    /**
     * 根据id查询菜品
     *
     * @param id
     * @return
     */
    @Override
    public DishVO getByIdWithFlavor(Long id) {
        Dish dish = dishMapper.getById(id);
        DishVO dishVO = new DishVO();

        if (dish != null) {
            BeanUtils.copyProperties(dish, dishVO);
            // 查询菜品对应的口味数据，从dish_flavor表查询
            List<DishFlavor> flavors = dishFlavorMapper.getByDishId(id);
            dishVO.setFlavors(flavors);
        }

        return dishVO;
    }

    /**
     * 更新菜品以及口味
     *
     * @param dishDTO
     */
    @Override
    @Transactional
    public void updateWithFlavor(DishDTO dishDTO) {
        Dish existing = dishDTO == null || dishDTO.getId() == null ? null : dishMapper.getById(dishDTO.getId());
        if (existing == null) {
            throw new ProductBusinessException(MessageConstant.DISH_NOT_FOUND);
        }
        validateProductUpdate(dishDTO, existing);

        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);

        // 修改菜品表基本信息
        dishMapper.update(dish);
        // 退出拼团模式时清空陈旧团参数，避免 isGroupon=0 的商品残留半截拼团配置
        if (!Integer.valueOf(1).equals(dishDTO.getIsGroupon() != null ? dishDTO.getIsGroupon() : existing.getIsGroupon())) {
            dishMapper.clearGroupon(dish.getId());
        }

        // 删除原有口味数据
        dishFlavorMapper.deleteByDishId(dish.getId());

        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && !flavors.isEmpty()) {
            // 批量插入n条数据
            flavors.forEach(flavor -> {
                flavor.setDishId(dish.getId());
            });
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     /**
     * 启用或禁用菜品
     *
     * @param status
     * @param id
     */
    @Override
    @Transactional
    public void startOrStop(Integer status, Long id) {
        Dish dish = Dish.builder()
                .id(id)
                .status(status)
                .updateTime(LocalDateTime.now())
                .updateUser(BaseContext.getCurrentId())
                .build();

        dishMapper.updateStatus(dish);
    }

    /**
     * 根据分类id查询菜品
     *
     * @param categoryId
     * @return
     */
    @Override
    public List<Dish> list(Long categoryId) {
        return dishMapper.getListById(categoryId);
    }

    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    public List<DishVO> listWithFlavor(Dish dish) {
        List<Dish> dishList = dishMapper.list(dish);

        List<DishVO> dishVOList = new ArrayList<>();

        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d,dishVO);

            //根据菜品id查询对应的口味
            List<DishFlavor> flavors = dishFlavorMapper.getByDishId(d.getId());

            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }

        return dishVOList;
    }

    /**
     * 根据分类id查询菜品
     *
     * @param categoryId
     * @return
     */
    // @Override
    // public List<Dish> list(Long categoryId) {
    //     // return List.of();
    // }

    /**
     * 条件查询菜品和口味
     *
     * @param dish
     * @return
     */
    // @Override
    // public List<DishVO> listWithFlavor(Dish dish) {
    //     // return List.of();
    // }
}
