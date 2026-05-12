package com.pobar.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Refresh Token：opaque 隨機字串（hashed 後存 DB），用來換新 access token。
 * 不像 access token 是 JWT，refresh 用 opaque 是為了能在 DB 端控制 revocation。
 */
@Data
@TableName("refresh_token")
public class RefreshToken {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer userId;
    private String tokenHash;
    private LocalDateTime expiresAt;
    /** true = 信任裝置（本機記住），TTL 較長 */
    private Boolean trusted;
    /** true = 已撤銷（登出 / 改密碼 / 管理員強制） */
    private Boolean revoked;
    private String userAgent;
    private String ip;
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
}
