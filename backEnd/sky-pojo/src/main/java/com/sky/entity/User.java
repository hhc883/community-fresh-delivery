package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //微信用户唯一标识
    private String openid;

    //姓名
    private String name;

    //手机号
    private String phone;

    //性别 0 女 1 男
    private String sex;

    //身份证号
    private String idNumber;

    //头像
    private String avatar;

    //登录密码
    private String password;

    //余额
    private java.math.BigDecimal balance;

    //注册时间
    private LocalDateTime createTime;

    public String getNickname() {
        return name != null && !name.isEmpty() ? name : (phone != null ? "用户" + phone.substring(Math.max(0, phone.length() - 4)) : "微信用户");
    }

    public void setNickname(String nickname) {
        this.name = nickname;
    }
}
