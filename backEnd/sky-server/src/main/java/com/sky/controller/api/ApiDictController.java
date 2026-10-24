package com.sky.controller.api;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/api/dict")
@Slf4j
@Api(tags = "生鲜自提-字典接口")
public class ApiDictController {

    private static final Map<String, List<Map<String, Object>>> DICT_MAP = new HashMap<>();

    static {
        // 商品分类
        List<Map<String, Object>> dishCategory = new ArrayList<>();
        dishCategory.add(createDictItem("肉蛋水产", "code:meat", 110));
        dishCategory.add(createDictItem("新鲜蔬菜", "code:vegetable", 100));
        dishCategory.add(createDictItem("速食", "code:fastfood", 90));
        dishCategory.add(createDictItem("酒水", "code:wine", 80));
        dishCategory.add(createDictItem("水果", "code:fruit", 70));
        dishCategory.add(createDictItem("生鲜", "code:fresh", 60));
        dishCategory.add(createDictItem("速冻", "code:frozen", 50));
        dishCategory.add(createDictItem("零食", "code:snack", 40));
        dishCategory.add(createDictItem("乳饮", "code:dairy", 30));
        dishCategory.add(createDictItem("粮油", "code:grain", 20));
        dishCategory.add(createDictItem("百货", "code:general", 10));
        DICT_MAP.put("dish_category", dishCategory);

        // 优惠券配置
        List<Map<String, Object>> couponTemplate = new ArrayList<>();
        couponTemplate.add(createDictItem("满19减4", "{\"threshold\":19,\"amount\":4,\"state\":\"available\"}", 30));
        couponTemplate.add(createDictItem("满30减5", "{\"threshold\":30,\"amount\":5,\"state\":\"locked1\"}", 20));
        couponTemplate.add(createDictItem("满50减21", "{\"threshold\":50,\"amount\":21,\"state\":\"locked2\"}", 10));
        DICT_MAP.put("coupon_template", couponTemplate);

        // 金刚区入口
        List<Map<String, Object>> kingkongEntry = new ArrayList<>();
        kingkongEntry.add(createDictItem("超级秒杀", "code:seckill", 50));
        kingkongEntry.add(createDictItem("免费领商品", "code:free", 40));
        kingkongEntry.add(createDictItem("领券中心", "code:coupon_center", 30));
        kingkongEntry.add(createDictItem("邀请赚现金", "code:invite", 20));
        kingkongEntry.add(createDictItem("其他", "code:more", 10));
        DICT_MAP.put("kingkong_entry", kingkongEntry);

        // 服务保障标签
        List<Map<String, Object>> serviceTags = new ArrayList<>();
        serviceTags.add(createDictItem("坏了包退", "code:refund_bad", 40));
        serviceTags.add(createDictItem("晚到必赔", "code:late_pay", 30));
        serviceTags.add(createDictItem("极速退款", "code:fast_refund", 20));
        serviceTags.add(createDictItem("缺货退款", "code:out_of_stock_refund", 10));
        DICT_MAP.put("service_tags", serviceTags);
    }

    private static Map<String, Object> createDictItem(String word, String definition, int sortOrder) {
        Map<String, Object> item = new HashMap<>();
        item.put("word", word);
        item.put("definition", definition);
        item.put("sortOrder", sortOrder);
        return item;
    }

    @GetMapping
    @ApiOperation("查询字典项列表")
    public Result<List<Map<String, Object>>> getDictGroup(@RequestParam String group) {
        log.info("查询字典分组: {}", group);
        List<Map<String, Object>> list = DICT_MAP.get(group);
        if (list == null) {
            list = Collections.emptyList();
        }
        return Result.success(list);
    }
}
