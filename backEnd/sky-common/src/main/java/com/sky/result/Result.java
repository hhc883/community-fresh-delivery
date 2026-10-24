package com.sky.result;

import lombok.Data;

import java.io.Serializable;

/**
 * 后端统一返回结果
 * @param <T>
 */
@Data
public class Result<T> implements Serializable {

    private Integer code; // 编码：1成功(管理端/小程序)，200成功(生鲜API)；0或400/500为失败
    private String msg; // 错误信息（兼容历史）
    private String message; // 错误信息（前端规范）
    private T data; // 数据

    public void setMsg(String msg) {
        this.msg = msg;
        if (this.message == null) {
            this.message = msg;
        }
    }

    public void setMessage(String message) {
        this.message = message;
        if (this.msg == null) {
            this.msg = message;
        }
    }

    public String getMessage() {
        return message != null ? message : msg;
    }

    public String getMsg() {
        return msg != null ? msg : message;
    }

    public static <T> Result<T> success() {
        Result<T> result = new Result<T>();
        result.code = 1;
        result.msg = "操作成功";
        result.message = "操作成功";
        return result;
    }

    public static <T> Result<T> success(T object) {
        Result<T> result = new Result<T>();
        result.data = object;
        result.code = 1;
        result.msg = "操作成功";
        result.message = "操作成功";
        return result;
    }

    public static <T> Result<T> error(String msg) {
        Result<T> result = new Result<T>();
        result.msg = msg;
        result.message = msg;
        result.code = 0;
        return result;
    }

    public static <T> Result<T> error(Integer code, String msg) {
        Result<T> result = new Result<T>();
        result.msg = msg;
        result.message = msg;
        result.code = code;
        return result;
    }

}
