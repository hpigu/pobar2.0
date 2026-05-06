package com.pobar.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("system_setting")
public class SystemSetting {

    @TableId
    private String settingKey;
    private String settingValue;
    private String description;
}
