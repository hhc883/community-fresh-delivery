package com.sky.controller.user;

import com.sky.result.Result;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Real gateway callbacks stay disabled while this graduation project uses simulated payments. */
@RestController
@RequestMapping("/notify")
public class PayNotifyController {
    @PostMapping("/paySuccess")
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    public Result<String> paySuccessNotify() {
        return Result.error(501, "真实微信支付回调未启用，请使用已标识的模拟支付流程");
    }
}
