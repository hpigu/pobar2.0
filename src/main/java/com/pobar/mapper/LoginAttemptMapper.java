package com.pobar.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pobar.entity.LoginAttempt;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface LoginAttemptMapper extends BaseMapper<LoginAttempt> {

    @Select("SELECT * FROM login_attempt WHERE account = #{account}")
    LoginAttempt findByAccount(String account);
}
