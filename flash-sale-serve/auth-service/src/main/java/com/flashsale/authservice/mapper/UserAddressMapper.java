package com.flashsale.authservice.mapper;

import com.flashsale.authservice.domain.po.UserAddressPO;
import com.flashsale.authservice.domain.vo.UserAddressVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author strive_qin
 * @version 1.0
 * @description UserAddressMapper
 * @date 2026/3/23 00:00
 */
@Mapper
public interface UserAddressMapper {

    List<UserAddressVO> listByUserId(@Param("userId") Long userId);

    UserAddressPO findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    UserAddressVO getDetailByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    UserAddressVO findDefaultByUserId(@Param("userId") Long userId);

    int insert(UserAddressPO address);

    int update(UserAddressPO address);

    int clearDefaultByUserId(@Param("userId") Long userId);

    int markDeleted(@Param("id") Long id, @Param("userId") Long userId);
}
