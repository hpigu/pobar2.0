package com.pobar.service;

import com.pobar.entity.SystemSetting;
import com.pobar.mapper.SystemSettingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SettingService {

    private final SystemSettingMapper settingMapper;

    public String get(String key) {
        SystemSetting setting = settingMapper.selectById(key);
        return setting != null ? setting.getSettingValue() : null;
    }

    public String get(String key, String defaultValue) {
        String value = get(key);
        return value != null ? value : defaultValue;
    }

    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(get(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public BigDecimal getDecimal(String key, BigDecimal defaultValue) {
        try {
            return new BigDecimal(get(key, defaultValue.toPlainString()));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public void set(String key, String value) {
        SystemSetting setting = new SystemSetting();
        setting.setSettingKey(key);
        setting.setSettingValue(value);
        settingMapper.updateById(setting);
    }

    public List<SystemSetting> listAll() {
        return settingMapper.selectList(null);
    }

    public void batchUpdate(Map<String, String> updates) {
        updates.forEach(this::set);
    }
}
