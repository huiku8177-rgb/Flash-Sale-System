package com.flashsale.authservice.mapper;

import com.flashsale.authservice.domain.po.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
/**
 * @author strive_qin
 * @version 1.0
 * @description UserMapper
 * @date 2026/3/20 00:00
 */


@Mapper
public interface UserMapper {

    @Select("select * from user where username=#{username}")
    User findByUsername(@Param("username") String username);

    @Insert("insert into user(username,password) values(#{username},#{password})")
    void insert(User user);

    @Select("select * from user where id=#{userId}")
    User findById(@Param("userId") Long userId);

    @Update("update user set username=#{username},password=#{password} where id=#{id}")
    void updateUser(User user);
}
