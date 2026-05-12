package com.pobar.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pobar.entity.IpLockout;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface IpLockoutMapper extends BaseMapper<IpLockout> {

    @Select("SELECT * FROM ip_lockout WHERE ip = #{ip}")
    IpLockout findByIp(String ip);

    /** 刪除已過期的鎖定（給排程清理用） */
    @Update("DELETE FROM ip_lockout WHERE locked_until <= NOW()")
    int deleteExpired();
}
