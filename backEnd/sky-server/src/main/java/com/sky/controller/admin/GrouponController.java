package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.GrouponService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端 - 拼团管理接口
 */
@RestController("adminGrouponController")
@RequestMapping("/admin/groupon")
@Slf4j
@Api(tags = "管理端-拼团管理接口")
public class GrouponController {

    @Autowired
    private GrouponService grouponService;

    /**
     * 手动触达成团校验，可用于排查「人数已满但状态未更新」的异常数据
     */
    @PutMapping("/complete/{id}")
    @ApiOperation("手动校验并成团")
    public Result<Boolean> complete(@PathVariable Long id) {
        log.info("管理端手动成团校验：{}", id);
        return Result.success(grouponService.tryComplete(id));
    }

    /**
     * 手动关闭超时未成团的拼团（正常由定时任务自动执行）
     */
    @PutMapping("/closeExpired")
    @ApiOperation("关闭超时未成团的拼团")
    public Result<Integer> closeExpired() {
        log.info("管理端手动关闭超时拼团");
        return Result.success(grouponService.closeExpired());
    }
}
