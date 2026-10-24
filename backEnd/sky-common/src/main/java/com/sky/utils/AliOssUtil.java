package com.sky.utils;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.sky.constant.MessageConstant;
import com.sky.exception.BaseException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;

@Data
@AllArgsConstructor
@Slf4j
public class AliOssUtil {

    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;

    /**
     * 文件上传
     *
     * @param bytes
     * @param objectName
     * @return
     */
    public String upload(byte[] bytes, String objectName) {

        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        try {
            // 创建PutObject请求。
            ossClient.putObject(bucketName, objectName, new ByteArrayInputStream(bytes));
        } catch (OSSException oe) {
            // 修复：原先这里只打印日志，然后仍然返回一个"看起来正常"的 URL，
            // 导致上传失败时后台无任何提示、数据库却存下一个打不开的地址。
            // 现在改为抛出业务异常，让调用方和前端能明确看到失败原因。
            log.error("OSS 上传被拒绝：errorCode={}, errorMessage={}, requestId={}",
                    oe.getErrorCode(), oe.getErrorMessage(), oe.getRequestId());
            throw new BaseException(MessageConstant.UPLOAD_FAILED
                    + "（OSS：" + oe.getErrorCode() + "）");
        } catch (ClientException ce) {
            log.error("OSS 客户端异常（多为网络问题）：{}", ce.getMessage());
            throw new BaseException(MessageConstant.UPLOAD_FAILED + "（无法连接 OSS）");
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }

        //文件访问路径规则 https://BucketName.Endpoint/ObjectName
        StringBuilder stringBuilder = new StringBuilder("https://");
        stringBuilder
                .append(bucketName)
                .append(".")
                .append(endpoint)
                .append("/")
                .append(objectName);

        log.info("文件上传到:{}", stringBuilder.toString());

        return stringBuilder.toString();
    }
}
