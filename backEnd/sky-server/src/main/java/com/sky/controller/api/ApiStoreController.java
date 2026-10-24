package com.sky.controller.api;

import com.sky.entity.PickupPoint;
import com.sky.result.Result;
import com.sky.service.PickupPointService;
import com.sky.vo.StoreVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/api/stores")
@Slf4j
@Api(tags = "生鲜自提-网点接口")
public class ApiStoreController {

    @Autowired
    private PickupPointService pickupPointService;

    @GetMapping
    @ApiOperation("查询自提网点列表")
    public Result<Object> getStores() {
        log.info("查询生鲜自提门店列表");
        List<PickupPoint> points = pickupPointService.list(new PickupPoint());
        List<StoreVO> storeVOList = new ArrayList<>();

        if (points != null && !points.isEmpty()) {
            for (int i = 0; i < points.size(); i++) {
                PickupPoint pp = points.get(i);
                StoreVO vo = new StoreVO();
                BeanUtils.copyProperties(pp, vo);
                vo.setContactName(pp.getContact());
                vo.setContactPhone(pp.getPhone());
                vo.setPic(pp.getImage() != null ? pp.getImage() : "/static/images/png/dish-1.png");
                vo.setImage(vo.getPic());
                vo.setService(pp.getServiceTags() != null ? pp.getServiceTags() : "支持冷冻/冷藏");
                vo.setServiceTags(vo.getService());
                vo.setDistance(pp.getDistance() != null ? pp.getDistance() : ((i + 1) * 260) + "m");
                vo.setStatus(Integer.valueOf(1).equals(pp.getStatus()) ? "open" : "closed");
                storeVOList.add(vo);
            }
        }

        // 兜底保证包含默认门店「高黎柱发士多店」
        boolean hasGaoLi = storeVOList.stream().anyMatch(s -> s.getName() != null && s.getName().contains("高黎柱发士多店"));
        if (!hasGaoLi) {
            StoreVO defaultStore = StoreVO.builder()
                    .id(1L)
                    .name("高黎柱发士多店(请自备袋子谢谢)")
                    .address("广东省佛山市顺德区容桂高黎环涌西路桥灵坊23号")
                    .distance("260m")
                    .contact("柱哥")
                    .contactName("柱哥")
                    .phone("13800138000")
                    .contactPhone("13800138000")
                    .image("/static/images/png/dish-1.png")
                    .pic("/static/images/png/dish-1.png")
                    .service("支持冷冻/冷藏")
                    .serviceTags("支持冷冻/冷藏")
                    .status("open")
                    .build();
            storeVOList.add(0, defaultStore);
        }

        // 同时兼容:
        // 1. pickup.vue: res.data.data.records
        // 2. 其它页面/契约: res.data.records 或直接读 list
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("records", storeVOList);
        responseData.put("list", storeVOList);
        responseData.put("total", storeVOList.size());

        Map<String, Object> subData = new HashMap<>();
        subData.put("records", storeVOList);
        subData.put("total", storeVOList.size());
        responseData.put("data", subData);

        return Result.success(responseData);
    }
}
