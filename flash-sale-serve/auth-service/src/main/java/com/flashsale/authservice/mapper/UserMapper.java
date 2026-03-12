package com.flashsale.authservice.mapper;

import com.flashsale.authservice.domain.dto.UserDTO;
import com.flashsale.authservice.domain.po.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * @author strive_qin
 * @version 1.0
 * @description UserMapper
 * @date 2026/3/12 10:26
 */
@Mapper
public interface UserMapper {

  @Select("select * from user where username=#{username}")
    User findByUsername(String username);

  @Insert("insert into user(username,password) values(#{username},#{password})")
    void insert(User user);
}
