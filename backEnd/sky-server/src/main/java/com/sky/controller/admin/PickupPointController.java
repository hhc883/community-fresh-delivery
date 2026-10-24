package com.sky.controller.admin;

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
 * 管理端 - 自提点管理接口
 */
@RestController("adminPickupPointController")
@RequestMapping("/admin/pickupPoint")
@Slf4j
@Api(tags = "管理端-自提点管理接口")
public class PickupPointController {

    @Autowired
    private PickupPointService pickupPointService;

    @PostMapping
    @ApiOperation("新增自提点")
    public Result save(@RequestBody PickupPoint pickupPoint) {
        log.info("新增自提点：{}", pickupPoint);
        pickupPointService.save(pickupPoint);
        return Result.success();
    }

    @PutMapping
    @ApiOperation("修改自提点")
    public Result update(@RequestBody PickupPoint pickupPoint) {
        log.info("修改自提点：{}", pickupPoint);
        pickupPointService.update(pickupPoint);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除自提点")
    public Result delete(@PathVariable Long id) {
        pickupPointService.deleteById(id);
        return Result.success();
    }

    @GetMapping("/list")
    @ApiOperation("查询自提点列表")
    public Result<List<PickupPoint>> list(PickupPoint pickupPoint) {
        return Result.success(pickupPointService.list(pickupPoint));
    }

    @GetMapping("/{id}")
    @ApiOperation("查询自提点详情")
    public Result<PickupPoint> getById(@PathVariable Long id) {
        return Result.success(pickupPointService.getById(id));
    }
}
