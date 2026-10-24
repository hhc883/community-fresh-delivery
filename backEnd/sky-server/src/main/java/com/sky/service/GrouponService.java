package com.sky.service;

import com.sky.dto.GrouponJoinDTO;
import com.sky.entity.Dish;
import com.sky.entity.Groupon;
import com.sky.vo.GrouponVO;

import java.util.List;

/**
 * 社区团购业务接口
 */
public interface GrouponService {

    /**
     * 查询参与拼团的生鲜商品列表（社区团购首页）
     *
     * @param keyword 商品名模糊搜索，可空
     */
    List<Dish> listGrouponDish(String keyword);

    /**
     * 开团：以拼团价购买商品并创建一个新团（当前用户为团长）
     *
     * @param dto 开团参数（dishId 必填）
     * @return 拼团活动
     */
    Groupon openGroupon(GrouponJoinDTO dto);

    /**
     * 参团：加入一个已存在且仍可加入的团
     *
     * @param dto 参团参数（grouponId 必填）
     * @return 拼团活动（可能已因本次参团而成团）
     */
    Groupon joinGroupon(GrouponJoinDTO dto);

    /**
     * 查询拼团详情（含成员列表、还差几人成团）
     *
     * @param grouponId
     */
    GrouponVO getDetail(Long grouponId);

    /**
     * 查询某商品当前可加入的团列表
     *
     * @param dishId
     */
    List<GrouponVO> listJoinable(Long dishId);

    /**
     * 查询当前登录用户参与的拼团
     *
     * @param status 可空；1拼团中 2已成团 3已失败 4已过期
     */
    List<GrouponVO> listMine(Integer status);

    /**
     * 校验并尝试成团：若人数已满则把团标记为已成团，并通知相关订单。
     * 参团/开团后内部调用，也可由管理端手动触发。
     *
     * @param grouponId
     * @return 是否本次调用促成了成团
     */
    boolean tryComplete(Long grouponId);

    /**
     * 关闭超时未成团的拼团（定时任务调用），并处理退款
     *
     * @return 关闭的团数量
     */
    int closeExpired();
}
