package com.sky.mapper;

import com.sky.entity.Groupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface GrouponMapper {

    /**
     * 新增拼团活动（开团）
     *
     * @param groupon
     */
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Groupon groupon);

    /**
     * 根据id查询拼团活动
     *
     * @param id
     */
    @Select("select * from `groupon` where id = #{id}")
    Groupon getById(Long id);

    /**
     * 修改拼团活动（动态 SQL，见 GrouponMapper.xml）
     *
     * @param groupon
     */
    void update(Groupon groupon);

    /**
     * 查询某商品当前仍在进行中、且可加入的团（按剩余人数、创建时间排序）
     *
     * @param dishId
     */
    @Select("select * from `groupon` where dish_id = #{dishId} and status = 1 "
            + "and expire_time > now() and current_size < required_size "
            + "order by (required_size - current_size) asc, create_time asc")
    List<Groupon> listJoinableByDishId(Long dishId);

    /**
     * 查询某用户当前进行中的拼团
     * <p>
     * 注意：多个参数必须加 @Param，否则 XML 中的 #{userId}/#{status} 无法绑定。
     *
     * @param userId
     * @param status
     */
    List<Groupon> listMine(@Param("userId") Long userId, @Param("status") Integer status);

    /**
     * 查询已超时但仍是「拼团中」的活动，用于定时任务关闭
     *
     * @param now
     */
    @Select("select * from `groupon` where status = 1 and expire_time <= #{now}")
    List<Groupon> listExpired(LocalDateTime now);

    /**
     * 原子递增参团人数，并返回受影响行数。
     * 通过 current_size < required_size 与 status = 1 双重条件保证并发下不会超员。
     *
     * @param grouponId
     */
    int increaseCurrentSize(Long grouponId);
}
