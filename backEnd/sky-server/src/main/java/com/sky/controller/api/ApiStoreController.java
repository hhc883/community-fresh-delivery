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
import org.springframework.web.bind.annotation.PathVariable;
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
    @ApiOperation("查询自提网点列表（仅返回启用中的真实自提点）")
    public Result<Object> getStores() {
        log.info("查询生鲜自提门店列表");
        PickupPoint query = new PickupPoint();
        query.setStatus(1);
        List<PickupPoint> points = pickupPointService.list(query);
        List<StoreVO> storeVOList = new ArrayList<>();

        if (points != null) {
            for (PickupPoint pp : points) {
                storeVOList.add(toStoreVO(pp));
            }
        }

        // 兼容:
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

    @GetMapping("/{id}")
    @ApiOperation("查询单个自提网点详情（不存在或已停用明确失败）")
    public Result<StoreVO> getStore(@PathVariable Long id) {
        log.info("查询自提网点详情: id={}", id);
        PickupPoint point = pickupPointService.getById(id);
        if (point == null || !Integer.valueOf(1).equals(point.getStatus())) {
            return Result.error(400, "门店不存在或已关闭");
        }
        return Result.success(toStoreVO(point));
    }

    private StoreVO toStoreVO(PickupPoint pp) {
        StoreVO vo = new StoreVO();
        BeanUtils.copyProperties(pp, vo);
        vo.setContactName(pp.getContact());
        vo.setContactPhone(pp.getPhone());
        vo.setImage(pp.getImage());
        vo.setPic(pp.getImage());
        // 距离与服务标签只回填数据库真实值，没有就保持为空，不编造
        vo.setDistance(pp.getDistance());
        vo.setService(pp.getServiceTags());
        vo.setServiceTags(pp.getServiceTags());
        vo.setStatus(Integer.valueOf(1).equals(pp.getStatus()) ? "open" : "closed");
        return vo;
    }
}
