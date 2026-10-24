package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UserServiceImpl implements UserService {
    // 微信接口地址
    public static final String WX_LOGIN = "https://api.weixin.qq.com/sns/jscode2session";

    @Autowired
    private WeChatProperties weChatProperties;

    @Autowired
    private UserMapper userMapper;

    /**
     * 本地开发模拟登录开关，对应 application-dev.yml 中的 sky.wechat.mock-login。
     * <p>
     * 微信开发者工具在「游客模式」（未关联 AppID）下，wx.login 只会返回
     * "the code is a mock one"，后端拿它去调真实微信接口必然失败，
     * 导致本地无法登录、所有需要 token 的接口都返回 401。
     * 打开该开关后，后端直接根据 code 生成稳定的模拟 openid，便于本地调试。
     * <p>
     * 生产环境务必设为 false 或删除该配置。
     */
    @Value("${sky.wechat.mock-login:false}")
    private boolean mockLogin;

    /**
     * 微信登录
     *
     * @param userLoginDTO
     * @return
     */
    @Override
    public User wxLogin(UserLoginDTO userLoginDTO) {
        // 判断openid是否为空，为空登录成功，抛出异常
        String openid = getOpenId(userLoginDTO.getCode());
        log.info("openid: {}", openid);
        log.info("UserLoginDto {}", userLoginDTO);
        if (openid == null) {
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }

        // 判断用户是否为新用户
        User user = userMapper.getByOpenid(openid);
        if (user == null) {
            // 新用户自动完成注册
            user = User.builder()
                    .openid(openid)
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.insert(user);
        }

        // 返回用户对象
        return user;
    }

    private String getOpenId(String code) {
        // 本地开发：跳过真实微信接口，直接生成模拟 openid
        if (mockLogin) {
            String mockOpenid = buildMockOpenid(code);
            log.warn("【本地模拟登录】已跳过微信接口，code={} -> openid={}", code, mockOpenid);
            return mockOpenid;
        }

        Map<String, String> reqParams = new HashMap<>();
        reqParams.put("appid", weChatProperties.getAppid());
        reqParams.put("secret", weChatProperties.getSecret());
        reqParams.put("js_code", code);
        reqParams.put("grant_type", "authorization_code");
        String json = HttpClientUtil.doGet(WX_LOGIN, reqParams);
        log.info("微信接口返回的json: {}", json);

        // 网络异常时 HttpClientUtil 返回 null，直接 parseObject 会抛 NPE（原先会变成 500）
        if (json == null || json.trim().isEmpty()) {
            log.error("调用微信登录接口失败，返回内容为空");
            return null;
        }

        JSONObject parseJson = JSON.parseObject(json);
        return parseJson.getString("openid");
    }

    /**
     * 由 code 生成稳定的模拟 openid。
     * 同一个 code 始终映射到同一个用户，避免每次登录都新建一个用户。
     */
    private String buildMockOpenid(String code) {
        String suffix = (code == null) ? "" : code.replaceAll("[^A-Za-z0-9]", "");
        if (suffix.isEmpty()) {
            suffix = "guest";
        }
        if (suffix.length() > 30) {
            suffix = suffix.substring(0, 30);
        }
        return "mock_" + suffix;
    }
}
