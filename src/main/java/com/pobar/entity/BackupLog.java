package com.pobar.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("backup_log")
public class BackupLog {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private String fileName;
    private Long fileSizeBytes;
    private String status;        // SUCCESS, FAILED
    private String errorMessage;
    private LocalDateTime createdAt;
}
