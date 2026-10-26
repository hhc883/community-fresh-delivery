package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.LocalImageStorage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


/**
 * 通用接口
 */
@RestController
@RequestMapping("/admin/common")
@Api(tags = "通用接口")
@Slf4j
public class CommonController {

    // 依赖注入
    private final LocalImageStorage imageStorage;
    public CommonController(LocalImageStorage imageStorage) { this.imageStorage = imageStorage; }

    @PostMapping("upload")
    @ApiOperation("文件上传")
    public Result<String> upload(MultipartFile file) {
        return Result.success(imageStorage.store(file));
    }

}
