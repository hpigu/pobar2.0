package com.pobar.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pobar.entity.JwtBlacklist;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface JwtBlacklistMapper extends BaseMapper<JwtBlacklist> {

    @Select("SELECT COUNT(1) > 0 FROM jwt_blacklist WHERE token_hash = #{tokenHash} AND expires_at > NOW()")
    boolean existsByTokenHash(String tokenHash);

    /** 清掉已過期的 token 黑名單（給排程用） */
    @Update("DELETE FROM jwt_blacklist WHERE expires_at <= NOW()")
    int deleteExpired();
}
