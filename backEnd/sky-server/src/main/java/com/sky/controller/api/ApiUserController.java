package com.sky.controller.api;

import com.sky.context.BaseContext;
import com.sky.entity.User;
import com.sky.mapper.UserMapper;
import com.sky.result.Result;
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

    @GetMapping("/user/me")
    @ApiOperation("获取当前登录用户信息")
    public Result<Map<String, Object>> getUserMe() {
        Long userId = BaseContext.getCurrentId();
        log.info("查询用户中心数据: userId={}", userId);
        User user = userId != null ? userMapper.getById(userId) : null;
        if (user == null) {
            user = User.builder()
                    .id(1L)
                    .phone("13800138000")
                    .name("自提达人")
                    .avatar("/static/images/png/default-avatar.png")
                    .balance(new BigDecimal("0.00"))
                    .build();
        }

        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("phone", user.getPhone());
        data.put("nickname", user.getNickname());
        data.put("avatar", user.getAvatar());
        data.put("balance", user.getBalance() != null ? user.getBalance() : new BigDecimal("0.00"));
        data.put("couponCount", 3);

        Map<String, Object> orderCount = new HashMap<>();
        orderCount.put("pendingPay", 1);
        orderCount.put("pendingPickup", 2);
        data.put("orderCount", orderCount);

        return Result.success(data);
    }

    @PostMapping("/user/avatar")
    @ApiOperation("更新用户头像")
    public Result<Map<String, Object>> updateAvatar(@RequestBody Map<String, String> body) {
        String avatar = body.get("avatar");
        Long userId = BaseContext.getCurrentId();
        if (userId != null) {
            User user = userMapper.getById(userId);
            if (user != null) {
                user.setAvatar(avatar);
                userMapper.update(user);
            }
        }
        Map<String, Object> data = new HashMap<>();
        data.put("avatar", avatar);
        return Result.success(data);
    }

    @PostMapping("/user/change-phone")
    @ApiOperation("更换绑定手机号")
    public Result<Map<String, Object>> changePhone(@RequestBody Map<String, String> body) {
        String newPhone = body.get("newPhone");
        if (!StringUtils.hasText(newPhone)) {
            return Result.error(400, "新手机号格式不正确");
        }
        Long userId = BaseContext.getCurrentId();
        if (userId != null) {
            User user = userMapper.getById(userId);
            if (user != null) {
                user.setPhone(newPhone);
                userMapper.update(user);
            }
        }
        Map<String, Object> data = new HashMap<>();
        data.put("phone", newPhone);
        return Result.success(data);
    }

    @GetMapping("/coupons")
    @ApiOperation("查询我的优惠券")
    public Result<List<Map<String, Object>>> getCoupons() {
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> c1 = new HashMap<>();
        c1.put("id", 1);
        c1.put("couponKey", "coupon_1");
        c1.put("threshold", 19);
        c1.put("amount", 4);
        c1.put("status", "available");
        c1.put("name", "满19减4生鲜券");
        list.add(c1);

        Map<String, Object> c2 = new HashMap<>();
        c2.put("id", 2);
        c2.put("couponKey", "coupon_2");
        c2.put("threshold", 30);
        c2.put("amount", 5);
        c2.put("status", "available");
        c2.put("name", "满30减5自提专享券");
        list.add(c2);
        return Result.success(list);
    }

    @PostMapping("/coupons/claim")
    @ApiOperation("领取优惠券")
    public Result<String> claimCoupon(@RequestBody Map<String, String> body) {
        String couponKey = body.get("couponKey");
        log.info("用户领券: couponKey={}", couponKey);
        return Result.success("领取成功");
    }

    @GetMapping("/coupons/center")
    @ApiOperation("领券中心列表")
    public Result<List<Map<String, Object>>> getCouponCenter() {
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> c1 = new HashMap<>();
        c1.put("couponKey", "coupon_1");
        c1.put("threshold", 19);
        c1.put("amount", 4);
        c1.put("claimed", false);
        list.add(c1);

        Map<String, Object> c2 = new HashMap<>();
        c2.put("couponKey", "coupon_2");
        c2.put("threshold", 30);
        c2.put("amount", 5);
        c2.put("claimed", true);
        list.add(c2);
        return Result.success(list);
    }

    @GetMapping("/invite/me")
    @ApiOperation("查询我的邀请信息")
    public Result<Map<String, Object>> getInviteInfo() {
        Map<String, Object> data = new HashMap<>();
        data.put("inviteCode", "SF888999");
        data.put("inviteUrl", "#/pages/login/login?invite=SF888999");
        data.put("invitedCount", 5);
        data.put("completedCount", 3);
        data.put("records", Collections.emptyList());
        return Result.success(data);
    }

    @GetMapping("/invite/cash")
    @ApiOperation("查询我的邀请现金")
    public Result<Map<String, Object>> getCashInfo() {
        Map<String, Object> data = new HashMap<>();
        data.put("balance", new BigDecimal("38.50"));
        data.put("frozenTotal", new BigDecimal("10.00"));
        data.put("totalEarned", new BigDecimal("48.50"));
        data.put("canWithdraw", true);
        data.put("withdrawMin", 20);
        data.put("flows", Collections.emptyList());
        return Result.success(data);
    }

    @PostMapping("/invite/cash/withdraw")
    @ApiOperation("提现邀请现金")
    public Result<Map<String, Object>> withdrawCash() {
        Map<String, Object> data = new HashMap<>();
        data.put("balance", BigDecimal.ZERO);
        return Result.success(data);
    }

    @GetMapping("/free/current")
    @ApiOperation("获取今日免费商品")
    public Result<Map<String, Object>> getFreeCurrent() {
        Map<String, Object> data = new HashMap<>();
        data.put("activityStatus", "online");
        data.put("remaining", 100);
        data.put("threshold", 30);
        data.put("todayPaid", 0);
        data.put("claimed", false);
        return Result.success(data);
    }
}
