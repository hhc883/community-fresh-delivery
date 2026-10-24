package com.sky.constant;

/**
 * 信息提示常量类
 */
public class MessageConstant {

    public static final String PASSWORD_ERROR = "密码错误";
    public static final String ACCOUNT_NOT_FOUND = "账号不存在";
    public static final String ALREADY_EXISTS = "账号已存在";
    public static final String ACCOUNT_LOCKED = "账号被锁定";
    public static final String UNKNOWN_ERROR = "未知错误";
    public static final String USER_NOT_LOGIN = "用户未登录";
    public static final String CATEGORY_BE_RELATED_BY_SETMEAL = "当前分类关联了套餐,不能删除";
    public static final String CATEGORY_BE_RELATED_BY_DISH = "当前分类关联了菜品,不能删除";
    public static final String SHOPPING_CART_IS_NULL = "购物车数据为空，不能下单";
    public static final String ADDRESS_BOOK_IS_NULL = "用户地址为空，不能下单";
    public static final String LOGIN_FAILED = "登录失败";
    public static final String UPLOAD_FAILED = "文件上传失败";
    public static final String SETMEAL_ENABLE_FAILED = "套餐内包含未启售菜品，无法启售";
    public static final String PASSWORD_EDIT_FAILED = "密码修改失败";
    public static final String DISH_ON_SALE = "起售中的菜品不能删除";
    public static final String SETMEAL_ON_SALE = "起售中的套餐不能删除";
    public static final String DISH_BE_RELATED_BY_SETMEAL = "当前菜品关联了套餐,不能删除";
    public static final String ORDER_STATUS_ERROR = "订单状态错误";
    public static final String ORDER_NOT_FOUND = "订单不存在";

    // ===== 社区团购相关 =====
    public static final String GROUPON_NOT_FOUND = "拼团活动不存在";
    public static final String GROUPON_NOT_JOINABLE = "该团已满员或已结束，请选择其它团";
    public static final String GROUPON_EXPIRED = "该团已过期";
    public static final String GROUPON_ALREADY_JOINED = "您已参与该团，不能重复参团";
    public static final String GROUPON_LEADER_CANNOT_QUIT = "团长不能退出拼团";
    public static final String DISH_NOT_SUPPORT_GROUPON = "该商品不支持拼团";
    public static final String DISH_OFF_SALE = "商品已下架";
    public static final String DISH_STOCK_NOT_ENOUGH = "商品库存不足";
    public static final String PICKUP_POINT_NOT_FOUND = "自提点不存在";

    // ===== 自提核销（m21） =====
    public static final String PICKUP_POINT_DISABLED = "该自提点已停用，请选择其他自提点";
    public static final String PICKUP_PERSON_REQUIRED = "自提订单必须填写提货人姓名和手机号";
    public static final String PICKUP_CODE_INVALID = "提货码格式不正确（应为 8 位数字）";
    public static final String PICKUP_CODE_NOT_FOUND = "提货码不存在，请核对后重试";
    public static final String PICKUP_CODE_GENERATE_FAILED = "提货码生成失败，请稍后重试";
    public static final String PICKUP_POINT_MISMATCH = "该订单不属于当前自提点，无法核销";
    public static final String ORDER_ALREADY_VERIFIED = "该订单已核销，不能重复领货";
    public static final String ORDER_NOT_PAID = "订单未支付，无法核销";

}
