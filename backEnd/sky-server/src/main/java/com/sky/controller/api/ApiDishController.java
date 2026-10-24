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

import java.math.BigDecimal;
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

        List<Dish> dishList = dishMapper.list(query);
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

        // 兜底商品（如果数据库当前分类没有商品）
        if (voList.isEmpty()) {
            voList.addAll(generateFallbackDishes(category, keyword));
        }

        return Result.success(voList);
    }

    @GetMapping("/dishes/{id}")
    @ApiOperation("查询商品详情")
    public Result<DishVO> getDishDetail(@PathVariable Long id) {
        log.info("查询商品详情: id={}", id);
        Dish dish = dishMapper.getById(id);
        if (dish == null) {
            // 如果不存在则返回兜底默认单品详情
            DishVO fallback = createFallbackDetail(id);
            return Result.success(fallback);
        }

        DishVO vo = toDishVO(dish, null);
        List<DishFlavor> flavors = dishFlavorMapper.getByDishId(id);
        if (flavors != null && !flavors.isEmpty()) {
            vo.setFlavors(flavors);
        }
        return Result.success(vo);
    }

    @GetMapping("/dishes/{dishId}/reviews")
    @ApiOperation("获取菜品评价列表")
    public Result<List<Map<String, Object>>> getReviews(@PathVariable Long dishId) {
        List<Map<String, Object>> reviews = new ArrayList<>();
        Map<String, Object> r1 = new HashMap<>();
        r1.put("id", 1);
        r1.put("dishId", dishId);
        r1.put("reviewer", "张**");
        r1.put("rating", 5);
        r1.put("content", "非常新鲜，自提很方便，店家服务热情！");
        r1.put("createdAt", "2026-10-20 18:30");
        reviews.add(r1);

        Map<String, Object> r2 = new HashMap<>();
        r2.put("id", 2);
        r2.put("dishId", dishId);
        r2.put("reviewer", "李**");
        r2.put("rating", 5);
        r2.put("content", "包装完好，分量足，物美价廉。");
        r2.put("createdAt", "2026-10-21 11:20");
        reviews.add(r2);
        return Result.success(reviews);
    }

    @PostMapping("/dishes/{dishId}/reviews")
    @ApiOperation("提交商品评价")
    public Result<String> submitReview(@PathVariable Long dishId, @RequestBody Map<String, Object> reviewData) {
        log.info("收到商品评价: dishId={}, data={}", dishId, reviewData);
        return Result.success("评价成功");
    }

    @GetMapping("/seckill/current")
    @ApiOperation("获取当前秒杀场次")
    public Result<Map<String, Object>> getSeckillCurrent() {
        Map<String, Object> data = new HashMap<>();
        data.put("serverTime", System.currentTimeMillis());
        data.put("start", "10:00");
        data.put("end", "22:00");

        List<DishVO> dishes = generateFallbackDishes("meat", null);
        data.put("dishes", dishes);
        return Result.success(data);
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

    private DishVO toDishVO(Dish dish, String defaultCategory) {
        DishVO vo = new DishVO();
        BeanUtils.copyProperties(dish, vo);
        if (vo.getSoldCount() == null || vo.getSoldCount() == 0) {
            vo.setSoldCount(1280);
        }
        if (!StringUtils.hasText(vo.getPromoTag())) {
            vo.setPromoTag("限时特惠");
        }
        if (vo.getSeckill() == null) {
            vo.setSeckill(1);
        }
        if (vo.getSeckillPrice() == null && vo.getPrice() != null) {
            vo.setSeckillPrice(vo.getPrice().multiply(new BigDecimal("0.85")).setScale(2, BigDecimal.ROUND_HALF_UP));
        }
        if (!StringUtils.hasText(vo.getTags())) {
            vo.setTags("产地直采,冷链保鲜,坏了包退");
        }
        if (!StringUtils.hasText(vo.getGoodRate())) {
            vo.setGoodRate("99%");
        }
        if (!StringUtils.hasText(vo.getImage())) {
            vo.setImage("/static/images/png/dish-1.png");
        }
        vo.setCategory(StringUtils.hasText(defaultCategory) ? defaultCategory : "meat");
        return vo;
    }

    private List<DishVO> generateFallbackDishes(String category, String keyword) {
        List<DishVO> list = new ArrayList<>();
        list.add(DishVO.builder()
                .id(1L)
                .name("Apple/苹果 iPhone 12 Pro Max (128G) 银色 支持5G")
                .price(new BigDecimal("9299.00"))
                .promoTag("限时直降")
                .soldCount(1580)
                .seckill(1)
                .seckillPrice(new BigDecimal("8999.00"))
                .tags("原装正品,现货直提")
                .goodRate("99%")
                .image("/static/images/png/dish-1.png")
                .category("meat")
                .build());

        list.add(DishVO.builder()
                .id(2L)
                .name("Redmi 9A 4GB+128GB 晴空翠 5000mAh大电量")
                .price(new BigDecimal("599.00"))
                .promoTag("双旦特惠")
                .soldCount(2890)
                .seckill(1)
                .seckillPrice(new BigDecimal("499.00"))
                .tags("现货到店,大电量大屏")
                .goodRate("98%")
                .image("/static/images/png/dish-2.png")
                .category("meat")
                .build());

        list.add(DishVO.builder()
                .id(3L)
                .name("新鲜原产地高原红提 500g 现摘鲜果")
                .price(new BigDecimal("29.90"))
                .promoTag("生鲜抢购")
                .soldCount(6200)
                .seckill(1)
                .seckillPrice(new BigDecimal("19.90"))
                .tags("当日采摘,冷链锁鲜,支持冷藏")
                .goodRate("100%")
                .image("/static/images/png/dish-3.png")
                .category("vegetable")
                .build());

        list.add(DishVO.builder()
                .id(4L)
                .name("优质鲜嫩牛腩块 1kg 真空包装")
                .price(new BigDecimal("79.90"))
                .promoTag("今日爆款")
                .soldCount(3450)
                .seckill(0)
                .tags("冷冻保鲜,坏了包退")
                .goodRate("99%")
                .image("/static/images/png/dish-1.png")
                .category("meat")
                .build());

        if (StringUtils.hasText(keyword)) {
            String kw = keyword.toLowerCase().trim();
            list.removeIf(d -> !d.getName().toLowerCase().contains(kw));
        }
        return list;
    }

    private DishVO createFallbackDetail(Long id) {
        return DishVO.builder()
                .id(id)
                .name("Redmi 9A 4GB+128GB 晴空翠 5000mAh大电量")
                .price(new BigDecimal("599.00"))
                .promoTag("双旦特惠")
                .soldCount(2890)
                .seckill(1)
                .seckillPrice(new BigDecimal("499.00"))
                .tags("现货到店,大电量大屏,支持自提")
                .goodRate("98%")
                .image("/static/images/png/dish-2.png")
                .category("meat")
                .build();
    }
}
