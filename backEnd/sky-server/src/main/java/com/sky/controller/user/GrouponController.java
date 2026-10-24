package com.sky.controller.user;

import com.sky.dto.GrouponJoinDTO;
import com.sky.entity.Dish;
import com.sky.entity.Groupon;
import com.sky.result.Result;
import com.sky.service.GrouponService;
import com.sky.vo.GrouponVO;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * C端 - 社区团购接口
 */
@RestController("userGrouponController")
@RequestMapping("/user/groupon")
@Slf4j
@Api(tags = "C端-社区团购接口")
public class GrouponController {

    @Autowired
    private GrouponService grouponService;

    @GetMapping("/dish/list")
    @ApiOperation("拼团商品列表（社区团购首页）")
    public Result<List<Dish>> dishList(@RequestParam(required = false) String keyword) {
        return Result.success(grouponService.listGrouponDish(keyword));
    }

    @PostMapping("/open")
    @ApiOperation("开团（以拼团价购买并成为团长）")
    public Result<Groupon> open(@RequestBody GrouponJoinDTO dto) {
        log.info("用户开团：{}", dto);
        return Result.success(grouponService.openGroupon(dto));
    }

    @PostMapping("/join")
    @ApiOperation("参团（加入他人已开的团）")
    public Result<Groupon> join(@RequestBody GrouponJoinDTO dto) {
        log.info("用户参团：{}", dto);
        return Result.success(grouponService.joinGroupon(dto));
    }

    @GetMapping("/detail/{id}")
    @ApiOperation("拼团详情（含还差几人成团）")
    public Result<GrouponVO> detail(@PathVariable Long id) {
        return Result.success(grouponService.getDetail(id));
    }

    @GetMapping("/joinable/{dishId}")
    @ApiOperation("查询某商品当前可加入的团")
    public Result<List<GrouponVO>> joinable(@PathVariable Long dishId) {
        return Result.success(grouponService.listJoinable(dishId));
    }

    @GetMapping("/mine")
    @ApiOperation("我的拼团")
    public Result<List<GrouponVO>> mine(@RequestParam(required = false) Integer status) {
        return Result.success(grouponService.listMine(status));
    }
}
