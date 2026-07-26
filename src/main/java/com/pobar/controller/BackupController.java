package com.pobar.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pobar.common.Result;
import com.pobar.entity.BackupLog;
import com.pobar.logging.Audit;
import com.pobar.mapper.BackupLogMapper;
import com.pobar.scheduler.BackupScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 資料庫備份（僅 ADMIN）。
 *
 * 每日 03:00 由 BackupScheduler 自動執行；這裡提供手動觸發與紀錄查詢，
 * 讓備份是否真的能跑成為可驗證的事（過去失敗時只留下 0 bytes 檔案，沒人發現）。
 */
@RestController
@RequestMapping("/api/backups")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class BackupController {

    private final BackupScheduler backupScheduler;
    private final BackupLogMapper backupLogMapper;

    /** 立即執行一次備份，回傳這次的紀錄（狀態、檔案大小、錯誤訊息）。 */
    @PostMapping
    @Audit(action = "RUN_BACKUP", entityType = "BackupLog",
            entityIdExpr = "#result?.data?.id",
            detailExpr = "'status=' + #result?.data?.status")
    public Result<BackupLog> runNow() {
        return Result.ok(backupScheduler.runBackup());
    }

    /** 最近 20 筆備份紀錄，新的在前。 */
    @GetMapping
    public Result<List<BackupLog>> recent() {
        QueryWrapper<BackupLog> query = new QueryWrapper<>();
        query.orderByDesc("id").last("LIMIT 20");
        return Result.ok(backupLogMapper.selectList(query));
    }
}
