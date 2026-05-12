package com.pobar.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * IP 層級鎖定（防分散式暴力破解：同 IP 短時間試多個 account）。
 */
@Data
@TableName("ip_lockout")
public class IpLockout {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private String ip;
    private LocalDateTime lockedUntil;
    private String reason;
    private LocalDateTime createdAt;
}
