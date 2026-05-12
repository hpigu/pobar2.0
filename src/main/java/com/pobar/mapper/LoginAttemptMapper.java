package com.pobar.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pobar.entity.LoginAttempt;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface LoginAttemptMapper extends BaseMapper<LoginAttempt> {

    /** 找出 (account, ip) 組合的失敗紀錄（用於檢查鎖定 + 累計失敗） */
    @Select("SELECT * FROM login_attempt WHERE account = #{account} AND ip = #{ip}")
    LoginAttempt findByAccountAndIp(@Param("account") String account, @Param("ip") String ip);

    /** 統計同一 IP 在指定時間後的累計失敗次數（跨 account） */
    @Select("SELECT COALESCE(SUM(fail_count), 0) FROM login_attempt WHERE ip = #{ip} AND updated_at >= #{since}")
    int sumFailsByIpSince(@Param("ip") String ip, @Param("since") LocalDateTime since);
}
