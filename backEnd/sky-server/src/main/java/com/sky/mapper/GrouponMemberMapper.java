package com.sky.mapper;

import com.sky.entity.GrouponMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface GrouponMemberMapper {

    /**
     * 新增拼团成员
     *
     * @param member
     */
    void insert(GrouponMember member);

    /**
     * 查询某拼团的全部成员
     *
     * @param grouponId
     */
    @Select("select * from groupon_member where groupon_id = #{grouponId} order by is_leader desc, create_time asc")
    List<GrouponMember> listByGrouponId(Long grouponId);

    /**
     * 查询某用户是否已在某团中（防止重复参团）
     * <p>
     * 注意：多个参数必须加 @Param。
     *
     * @param grouponId
     * @param userId
     */
    @Select("select * from groupon_member where groupon_id = #{grouponId} and user_id = #{userId}")
    GrouponMember getByGrouponIdAndUserId(@Param("grouponId") Long grouponId, @Param("userId") Long userId);

    /**
     * 查询某用户参与的所有拼团
     *
     * @param userId
     */
    @Select("select * from groupon_member where user_id = #{userId} order by create_time desc")
    List<GrouponMember> listByUserId(Long userId);

    /**
     * 根据订单id查询拼团成员记录
     *
     * @param orderId
     */
    @Select("select * from groupon_member where order_id = #{orderId}")
    GrouponMember getByOrderId(Long orderId);
}
