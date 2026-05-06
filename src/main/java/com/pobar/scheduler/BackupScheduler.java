package com.pobar.scheduler;

import com.pobar.entity.BackupLog;
import com.pobar.mapper.BackupLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class BackupScheduler {

    private final BackupLogMapper backupLogMapper;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String dbUser;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Value("${backup.output-dir:./backups}")
    private String outputDir;

    @Value("${backup.mysql-dump-path:mysqldump}")
    private String mysqldumpPath;

    // 每日 03:00 執行（業務日重置前）
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Taipei")
    public void dailyBackup() {
        String dbName = parseDbName(datasourceUrl);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "pobar_" + timestamp + ".sql";
        File outDir = new File(outputDir);
        if (!outDir.exists()) outDir.mkdirs();
        File outFile = new File(outDir, fileName);

        BackupLog log = new BackupLog();
        log.setFileName(fileName);
        log.setCreatedAt(LocalDateTime.now());

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    mysqldumpPath,
                    "-u", dbUser,
                    "-p" + dbPassword,
                    "--single-transaction",
                    "--routines",
                    "--triggers",
                    dbName
            );
            pb.redirectOutput(outFile);
            pb.redirectErrorStream(false);
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                log.setStatus("SUCCESS");
                log.setFileSizeBytes(outFile.length());
                BackupScheduler.log.info("資料庫備份成功: {}", fileName);
            } else {
                log.setStatus("FAILED");
                log.setErrorMessage("mysqldump exit code: " + exitCode);
                BackupScheduler.log.error("資料庫備份失敗，exit code: {}", exitCode);
            }
        } catch (IOException | InterruptedException e) {
            log.setStatus("FAILED");
            log.setErrorMessage(e.getMessage());
            BackupScheduler.log.error("資料庫備份例外: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
        }

        backupLogMapper.insert(log);
    }

    private String parseDbName(String url) {
        // jdbc:mysql://host:port/dbname?...
        String path = url.split("\\?")[0];
        return path.substring(path.lastIndexOf('/') + 1);
    }
}
