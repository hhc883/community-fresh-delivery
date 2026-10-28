package com.sky.controller.api;

import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.result.Result;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
@Slf4j
@Api(tags = "生鲜自提-商品接口")
public class ApiDishController {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @GetMapping("/dishes")
    @ApiOperation("查询商品列表")
    public Result<List<DishVO>> getDishes(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId) {
        log.info("查询生鲜商品列表: category={}, keyword={}, categoryId={}", category, keyword, categoryId);

        Dish query = new Dish();
        query.setStatus(1);
        if (StringUtils.hasText(keyword)) {
            query.setName(keyword.trim());
        }
        if (categoryId != null) {
            query.setCategoryId(categoryId);
        }

        List<Dish> dishList = dishMapper.listAvailable(null, query.getCategoryId(), query.getName());
        List<DishVO> voList = new ArrayList<>();

        if (dishList != null && !dishList.isEmpty()) {
            for (Dish dish : dishList) {
                // 如果传入了 category 且不匹配则跳过（支持肉蛋水产 meat, 蔬菜 vegetable 等过滤）
                if (StringUtils.hasText(category) && !matchesCategory(dish, category)) {
                    continue;
                }
                DishVO vo = toDishVO(dish, category);
                voList.add(vo);
            }
        }

        return Result.success(voList);
    }

    @GetMapping("/dishes/{id}")
    @ApiOperation("查询商品详情")
    public Result<DishVO> getDishDetail(@PathVariable Long id) {
        log.info("查询商品详情: id={}", id);
        List<Dish> available = dishMapper.listAvailable(id, null, null);
        if (available.isEmpty()) {
            return Result.error(400, "商品不存在或已下架");
        }
        Dish dish = available.get(0);

        DishVO vo = toDishVO(dish, null);
        List<DishFlavor> flavors = dishFlavorMapper.getByDishId(id);
        if (flavors != null && !flavors.isEmpty()) {
            vo.setFlavors(flavors);
        }
        return Result.success(vo);
    }

    @GetMapping("/dishes/{dishId}/reviews")
    @ResponseStatus(org.springframework.http.HttpStatus.NOT_IMPLEMENTED)
    public Result<?> getReviews(@PathVariable Long dishId) {
        return Result.error(501, "商品评价尚未实现");
    }

    @PostMapping("/dishes/{dishId}/reviews")
    @ResponseStatus(org.springframework.http.HttpStatus.NOT_IMPLEMENTED)
    public Result<?> submitReview(@PathVariable Long dishId) {
        return Result.error(501, "商品评价尚未实现");
    }

    @GetMapping("/seckill/current")
    @ResponseStatus(org.springframework.http.HttpStatus.NOT_IMPLEMENTED)
    public Result<?> getSeckillCurrent() {
        return Result.error(501, "秒杀活动尚未实现");
    }

    private boolean matchesCategory(Dish dish, String category) {
        if ("all".equalsIgnoreCase(category)) return true;
        String name = dish.getName() != null ? dish.getName().toLowerCase() : "";
        if ("meat".equalsIgnoreCase(category)) {
            return name.contains("肉") || name.contains("牛") || name.contains("羊") || name.contains("鸡") || name.contains("鱼") || name.contains("虾");
        }
        if ("vegetable".equalsIgnoreCase(category)) {
            return name.contains("菜") || name.contains("椒") || name.contains("菇") || name.contains("茄") || name.contains("瓜");
        }
        return true;
    }

    private DishVO toDishVO(Dish dish, String ignoredCategory) {
        DishVO vo = new DishVO();
        BeanUtils.copyProperties(dish, vo);
        return vo;
    }

}
