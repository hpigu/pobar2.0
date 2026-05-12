package com.pobar.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pobar.entity.RefreshToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface RefreshTokenMapper extends BaseMapper<RefreshToken> {

    /** 找出有效（未過期、未撤銷）的 refresh token */
    @Select("""
            SELECT * FROM refresh_token
            WHERE token_hash = #{tokenHash}
              AND revoked = FALSE
              AND expires_at > NOW()
            """)
    RefreshToken findActive(@Param("tokenHash") String tokenHash);

    /** 撤銷使用者所有 refresh token（改密碼、強制登出時用） */
    @Update("UPDATE refresh_token SET revoked = TRUE WHERE user_id = #{userId} AND revoked = FALSE")
    int revokeAllByUserId(@Param("userId") Integer userId);

    /** 撤銷單一 refresh token（登出時） */
    @Update("UPDATE refresh_token SET revoked = TRUE WHERE token_hash = #{tokenHash}")
    int revokeByHash(@Param("tokenHash") String tokenHash);

    /** 排程清理過期或撤銷已久的 token */
    @Update("DELETE FROM refresh_token WHERE expires_at <= NOW() OR (revoked = TRUE AND created_at <= NOW() - INTERVAL 7 DAY)")
    int deleteExpiredOrOldRevoked();
}
