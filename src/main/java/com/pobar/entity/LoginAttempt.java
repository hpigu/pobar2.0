package com.pobar.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("login_attempt")
public class LoginAttempt {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private String account;
    private String ip;
    private Integer failCount;
    private LocalDateTime lockedUntil;
    private LocalDateTime updatedAt;
}
