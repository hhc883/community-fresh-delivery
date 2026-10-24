package com.sky.handler;

import com.sky.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 统一响应体适配器：
 * 解决管理端原有端与生鲜自提前端的状态码和信封协议兼容性：
 * 1. PC 管理端（/admin/**）与原生微信小程序（/user/**）：
 *    - 严格依赖 `code === 1` 为成功，`code === 0` 为业务失败，弹框读取 `msg`；
 * 2. 生鲜自提前端（/api/**）：
 *    - 严格依赖 `code === 200` 为成功，`code === 400/500/401` 为失败，读取 `message`。
 */
@RestControllerAdvice
@Slf4j
public class ResultResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof Result) {
            Result<?> result = (Result<?>) body;
            String path = request.getURI().getPath();

            // 双向补全 msg 和 message，无论哪端读取都保证有提示文案
            if (result.getMsg() == null && result.getMessage() != null) {
                result.setMsg(result.getMessage());
            } else if (result.getMessage() == null && result.getMsg() != null) {
                result.setMessage(result.getMsg());
            }

            if (path != null && path.startsWith("/api/")) {
                // 生鲜自提前端规范（/api/**）：成功 200，通用业务失败 400
                if (result.getCode() != null && result.getCode() == 1) {
                    result.setCode(200);
                } else if (result.getCode() != null && result.getCode() == 0) {
                    result.setCode(400);
                } else if (result.getCode() == null) {
                    result.setCode(200);
                }
            } else {
                // 管理端/用户端协议（/admin/**, /user/**）：成功 1，通用业务失败 0
                if (result.getCode() != null && result.getCode() == 200) {
                    result.setCode(1);
                } else if (result.getCode() != null && (result.getCode() == 400 || result.getCode() == 500)) {
                    result.setCode(0);
                } else if (result.getCode() == null) {
                    result.setCode(1);
                }
            }
        }
        return body;
    }
}
