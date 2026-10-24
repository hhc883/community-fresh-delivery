package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.Map;

@Mapper
public interface UserMapper {

    /**
     * 根据openid查询用户
     * @param openid
     * @return
     */
    @Select("select * from user where openid = #{openid}")
    User getByOpenid(String openid);

    /**
     * 插入数据
     * @param user
     */
    void insert(User user);

    @Select("select * from user where id = #{id}")
    User getById(Long userId);

    /**
     * 根据手机号查询用户
     * @param phone
     * @return
     */
    @Select("select * from user where phone = #{phone} limit 1")
    User getByPhone(String phone);

    /**
     * 更新用户
     * @param user
     */
    void update(User user);

    /** 旧密码迁移必须匹配读取时的值，避免覆盖并发设置的新密码。 */
    @Update("update user set password=#{password} where id=#{id} and password=#{expectedPassword}")
    int migratePassword(@Param("id") Long id, @Param("expectedPassword") String expectedPassword,
                        @Param("password") String password);

    /**
     * 根据动态条件统计用户数量
     * @param map
     * @return
     */
    Integer countByMap(Map map);
}
