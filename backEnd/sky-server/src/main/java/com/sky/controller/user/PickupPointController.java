package com.sky.controller.user;

import com.sky.entity.PickupPoint;
import com.sky.result.Result;
import com.sky.service.PickupPointService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * C端 - 自提点接口
 */
@RestController("userPickupPointController")
@RequestMapping("/user/pickupPoint")
@Slf4j
@Api(tags = "C端-自提点接口")
public class PickupPointController {

    @Autowired
    private PickupPointService pickupPointService;

    @GetMapping("/list")
    @ApiOperation("查询启用的自提点列表")
    public Result<List<PickupPoint>> list(String community) {
        PickupPoint query = new PickupPoint();
        query.setCommunity(community);
        query.setStatus(1);
        return Result.success(pickupPointService.list(query));
    }

    @GetMapping("/{id}")
    @ApiOperation("查询自提点详情")
    public Result<PickupPoint> getById(@PathVariable Long id) {
        return Result.success(pickupPointService.getById(id));
    }
}
