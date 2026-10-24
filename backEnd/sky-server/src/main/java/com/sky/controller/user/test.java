package com.sky.controller.user;

import com.sky.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user/shop")
public class test {

    @GetMapping("/status")
    public Result<Integer> getStatus() {
        // 直接返回 1（营业中），永远不报错！
        return Result.success(1);
    }
}