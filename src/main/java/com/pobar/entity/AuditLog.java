package com.pobar.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@TableName("audit_log")
public class AuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer userId;
    private String account;
    private String role;
    private String action;
    private String entityType;
    private String entityId;
    private String result;
    private String detail;
    private String ip;
    private LocalDateTime createdAt;
}
