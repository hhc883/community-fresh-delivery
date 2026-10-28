package com.sky.controller.api;

import com.sky.context.BaseContext;
import com.sky.entity.User;
import com.sky.mapper.UserMapper;
import com.sky.result.Result;
import com.sky.entity.Orders;
import com.sky.service.OrderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api")
@Slf4j
@Api(tags = "生鲜自提-用户中心接口")
public class ApiUserController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OrderService orderService;

    @GetMapping("/user/me")
    @ApiOperation("获取当前登录用户信息")
    public Result<Map<String, Object>> getUserMe() {
        Long userId = BaseContext.getCurrentId();
        log.info("查询用户中心数据: userId={}", userId);
        User user = userId != null ? userMapper.getById(userId) : null;
        if (user == null) {
            return Result.error(400, "用户不存在，请重新登录");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("phone", user.getPhone());
        data.put("nickname", user.getNickname());
        data.put("avatar", user.getAvatar());
        data.put("balance", user.getBalance() != null ? user.getBalance() : new BigDecimal("0.00"));
        data.put("couponCount", 0);

        Map<String, Object> orderCount = new HashMap<>();
        orderCount.put("pendingPay", orderService.pageQueryByUser(1, 1, Orders.PENDING_PAYMENT).getTotal());
        orderCount.put("pendingPickup", orderService.pageQueryByUser(1, 1, Orders.TO_BE_CONFIRMED).getTotal());
        data.put("orderCount", orderCount);

        return Result.success(data);
    }

    @PostMapping("/user/avatar")
    @ApiOperation("更新用户头像")
    public Result<Map<String, Object>> updateAvatar(@RequestBody Map<String, Object> body) {
        Object value = body.get("avatar");
        if (!(value instanceof String) || !StringUtils.hasText((String) value)
                || ((String) value).length() > 500) {
            return Result.error(400, "头像地址须为1至500个字符的非空文本");
        }
        String avatar = ((String) value).trim();
        Long userId = BaseContext.getCurrentId();
        if (userId == null || userMapper.getById(userId) == null) {
            return Result.error(400, "用户不存在，请重新登录");
        }
        userMapper.update(User.builder().id(userId).avatar(avatar).build());
        Map<String, Object> data = new HashMap<>();
        data.put("avatar", avatar);
        return Result.success(data);
    }

    @GetMapping({"/coupons", "/coupons/center", "/invite/me", "/invite/cash", "/free/current"})
    @ResponseStatus(org.springframework.http.HttpStatus.NOT_IMPLEMENTED)
    public Result<?> unavailableMarketing() {
        return Result.error(501, "优惠券、邀请与免费活动尚未实现");
    }

    @PostMapping({"/coupons/claim", "/invite/cash/withdraw", "/user/change-phone"})
    @ResponseStatus(org.springframework.http.HttpStatus.NOT_IMPLEMENTED)
    public Result<?> unavailableAccountOperation() {
        return Result.error(501, "该操作尚未实现；手机换绑需要真实验证码服务");
    }
}
