package com.sky.exception;

/**
 * 商品管理业务异常（名称/分类/价格/库存/拼团参数不合法等）
 */
public class ProductBusinessException extends BaseException {

    public ProductBusinessException(String msg) {
        super(msg);
    }

}
