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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Slf4j
@Api(tags = "生鲜自提-认证接口")
public class ApiAuthController {

    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtProperties jwtProperties;

    @Value("${sky.auth.mock-login:false}")
    private boolean mockLogin;

    @Value("${sky.auth.dev-code:}")
    private String developmentCode;

    @PostMapping("/send-code")
    @ApiOperation("发送短信验证码")
    public Result<String> sendCode(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        if (!isPhone(phone)) {
            return Result.error(400, "手机号格式不正确");
        }
        if (!mockLogin || !StringUtils.hasText(developmentCode)) {
            return Result.error(400, "短信登录尚未配置，请使用密码或微信登录");
        }
        return Result.success("开发模式模拟验证码，请使用配置的开发验证码；未发送真实短信");
    }

    @PostMapping("/login")
    @ApiOperation("手机号验证码登录（新用户静默注册）")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        String code = body.get("code");
        if (!isPhone(phone)) {
            return Result.error(400, "手机号格式不正确");
        }
        if (!mockLogin || !StringUtils.hasText(developmentCode)) {
            return Result.error(400, "短信登录尚未配置，请使用密码或微信登录");
        }
        if (!developmentCode.equals(code)) {
            return Result.error(400, "验证码错误");
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
            log.info("开发模式创建用户: id={}", user.getId());
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
        data.put("mock", true);

        return Result.success(data);
    }

    @PostMapping("/login-password")
    @ApiOperation("手机号密码登录")
    public Result<Map<String, Object>> loginPassword(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        String password = body.get("password");
        if (!isPhone(phone) || !StringUtils.hasText(password)
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            return Result.error(400, "手机号和密码不能为空");
        }
        User user = userMapper.getByPhone(phone);
        if (user == null) {
            return Result.error(400, "用户不存在，请先使用验证码登录");
        }
        if (!StringUtils.hasText(user.getPassword())) {
            return Result.error(400, "尚未设置密码，请先使用验证码登录");
        }
        String storedPassword = user.getPassword();
        boolean bcrypt = storedPassword.startsWith("$2");
        boolean matches = bcrypt ? PASSWORD_ENCODER.matches(password, storedPassword)
                : storedPassword.matches("(?i)[0-9a-f]{32}")
                    && DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8))
                        .equalsIgnoreCase(storedPassword);
        if (!matches) {
            return Result.error(400, "密码错误，请重新输入");
        }
        // 旧 MD5 账户在验证真实密码后迁移，不接受存储值本身或明文兜底。
        if (!bcrypt) {
            userMapper.update(User.builder().id(user.getId())
                    .password(PASSWORD_ENCODER.encode(password)).build());
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
        if (!StringUtils.hasText(password) || password.length() < 6
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            return Result.error(400, "密码至少6位且不能超过72字节");
        }
        Long userId = com.sky.context.BaseContext.getCurrentId();
        if (userId == null) {
            return Result.error(401, "请先登录");
        }
        User user = userMapper.getById(userId);
        if (user == null) {
            return Result.error(401, "用户不存在，请重新登录");
        }
        userMapper.update(User.builder().id(userId).password(PASSWORD_ENCODER.encode(password)).build());
        return Result.success("密码设置成功");
    }

    private boolean isPhone(String phone) {
        return phone != null && phone.matches("1[3-9]\\d{9}");
    }
}
