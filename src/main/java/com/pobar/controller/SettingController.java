package com.pobar.controller;

import com.pobar.common.Result;
import com.pobar.entity.SystemSetting;
import com.pobar.service.SettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class SettingController {

    private final SettingService settingService;

    @GetMapping
    public Result<List<SystemSetting>> listAll() {
        return Result.ok(settingService.listAll());
    }

    @GetMapping("/{key}")
    public Result<Map<String, String>> get(@PathVariable String key) {
        String value = settingService.get(key);
        return Result.ok(Map.of("key", key, "value", value != null ? value : ""));
    }

    @PutMapping
    public Result<?> batchUpdate(@RequestBody Map<String, String> updates) {
        settingService.batchUpdate(updates);
        return Result.ok();
    }

    @PutMapping("/{key}")
    public Result<?> set(@PathVariable String key, @RequestBody Map<String, String> body) {
        settingService.set(key, body.get("value"));
        return Result.ok();
    }
}
