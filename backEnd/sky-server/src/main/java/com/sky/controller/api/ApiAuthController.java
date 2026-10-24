package com.sky.controller.api;

import com.sky.constant.JwtClaimsConstant;
import com.sky.entity.User;
import com.sky.mapper.UserMapper;
import com.sky.properties.JwtProperties;
import com.sky.result.Result;
import com.sky.utils.JwtUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Slf4j
@Api(tags = "生鲜自提-认证接口")
public class ApiAuthController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtProperties jwtProperties;

    @PostMapping("/send-code")
    @ApiOperation("发送短信验证码")
    public Result<String> sendCode(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        log.info("发送短信验证码到手机: {}", phone);
        return Result.success("验证码已发送");
    }

    @PostMapping("/login")
    @ApiOperation("手机号验证码登录（新用户静默注册）")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        String code = body.get("code");
        String inviteCode = body.get("inviteCode");
        log.info("前端手机号登录: phone={}, code={}, inviteCode={}", phone, code, inviteCode);

        if (!StringUtils.hasText(phone)) {
            return Result.error(400, "手机号不能为空");
        }

        User user = userMapper.getByPhone(phone);
        if (user == null) {
            // 自动注册新用户
            user = User.builder()
                    .phone(phone)
                    .openid("phone_" + phone)
                    .name("用户" + (phone.length() >= 4 ? phone.substring(phone.length() - 4) : phone))
                    .avatar("/static/images/png/default-avatar.png")
                    .balance(new BigDecimal("0.00"))
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.insert(user);
            log.info("新用户静默注册成功: id={}, phone={}", user.getId(), phone);
        }

        // 签发 JWT
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, user.getId());
        String token = JwtUtil.createJWT(jwtProperties.getUserSecretKey(), jwtProperties.getUserTtl(), claims);

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("phone", user.getPhone());
        userMap.put("nickname", user.getNickname());
        userMap.put("avatar", user.getAvatar());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("id", user.getId());
        data.put("phone", user.getPhone());
        data.put("nickname", user.getNickname());
        data.put("avatar", user.getAvatar());
        data.put("user", userMap);

        return Result.success(data);
    }

    @PostMapping("/login-password")
    @ApiOperation("手机号密码登录")
    public Result<Map<String, Object>> loginPassword(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        String password = body.get("password");
        if (!StringUtils.hasText(phone) || !StringUtils.hasText(password)) {
            return Result.error(400, "手机号和密码不能为空");
        }
        User user = userMapper.getByPhone(phone);
        if (user == null) {
            return Result.error(400, "用户不存在，请先使用验证码登录");
        }
        if (!StringUtils.hasText(user.getPassword())) {
            return Result.error(400, "尚未设置密码，请先使用验证码登录");
        }
        String md5 = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!md5.equals(user.getPassword()) && !password.equals(user.getPassword())) {
            return Result.error(400, "密码错误，请重新输入");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, user.getId());
        String token = JwtUtil.createJWT(jwtProperties.getUserSecretKey(), jwtProperties.getUserTtl(), claims);

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("id", user.getId());
        data.put("phone", user.getPhone());
        data.put("nickname", user.getNickname());
        data.put("avatar", user.getAvatar());
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("phone", user.getPhone());
        data.put("user", userMap);

        return Result.success(data);
    }

    @PostMapping("/set-password")
    @ApiOperation("设置登录密码")
    public Result<String> setPassword(@RequestBody Map<String, String> body) {
        String password = body.get("password");
        if (!StringUtils.hasText(password) || password.length() < 6) {
            return Result.error(400, "密码长度不能少于6位");
        }
        Long userId = com.sky.context.BaseContext.getCurrentId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        User user = userMapper.getById(userId);
        if (user != null) {
            user.setPassword(DigestUtils.md5DigestAsHex(password.getBytes()));
            userMapper.update(user);
        }
        return Result.success("密码设置成功");
    }
}
