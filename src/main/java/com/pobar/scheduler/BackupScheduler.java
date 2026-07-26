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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
        runBackup();
    }

    /**
     * 執行一次 mysqldump 並寫入 backup_log。
     * 供排程與 ADMIN 手動觸發共用，回傳這次的紀錄（含狀態與檔案大小）。
     */
    public BackupLog runBackup() {
        String dbName = parseDbName(datasourceUrl);
        String host = parseHost(datasourceUrl);
        String port = parsePort(datasourceUrl);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "pobar_" + timestamp + ".sql";
        File outDir = new File(outputDir);
        if (!outDir.exists()) outDir.mkdirs();
        File outFile = new File(outDir, fileName);
        File errFile = new File(outDir, fileName + ".err");

        BackupLog record = new BackupLog();
        record.setFileName(fileName);
        record.setBackupAt(LocalDateTime.now());

        try {
            // 註：DB 在另一個容器，必須明確指定 -h/-P，不能靠 mysqldump 預設的 localhost。
            // --no-tablespaces：pobar 帳號非 root，沒有 PROCESS 權限，不加會以 1227 失敗。
            ProcessBuilder pb = new ProcessBuilder(
                    mysqldumpPath,
                    "-h", host,
                    "-P", port,
                    "-u", dbUser,
                    "-p" + dbPassword,
                    "--single-transaction",
                    "--no-tablespaces",
                    "--routines",
                    "--triggers",
                    "--default-character-set=utf8mb4",
                    dbName
            );
            pb.redirectOutput(outFile);
            pb.redirectError(errFile);
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                record.setStatus("SUCCESS");
                record.setFileSizeBytes(outFile.length());
                errFile.delete();
                log.info("資料庫備份成功: {}（{} bytes）", fileName, outFile.length());
            } else {
                record.setStatus("FAILED");
                record.setErrorMessage(truncate("mysqldump exit " + exitCode + ": " + readErr(errFile)));
                // 失敗時 redirectOutput 已建出空檔，留著會被誤認為有備份
                discardPartial(outFile, errFile);
                log.error("資料庫備份失敗（exit {}）：{}", exitCode, record.getErrorMessage());
            }
        } catch (IOException | InterruptedException e) {
            record.setStatus("FAILED");
            record.setErrorMessage(truncate(e.getMessage()));
            discardPartial(outFile, errFile);
            log.error("資料庫備份例外: {}", e.getMessage(), e);
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
        }

        backupLogMapper.insert(record);
        return record;
    }

    /** 刪掉失敗留下的空檔 / 半成品與 stderr 暫存檔。 */
    private void discardPartial(File outFile, File errFile) {
        outFile.delete();
        errFile.delete();
    }

    private String readErr(File errFile) {
        try {
            if (!errFile.exists()) return "";
            return new String(Files.readAllBytes(errFile.toPath()), StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            return "";
        }
    }

    private String truncate(String msg) {
        if (msg == null) return null;
        return msg.length() > 500 ? msg.substring(0, 500) : msg;
    }

    private String parseDbName(String url) {
        // jdbc:mysql://host:port/dbname?...
        String path = url.split("\\?")[0];
        return path.substring(path.lastIndexOf('/') + 1);
    }

    /** 從 jdbc:mysql://host:port/db 取出 host，取不到時退回 localhost。 */
    private String parseHost(String url) {
        String authority = authority(url);
        if (authority.isEmpty()) return "localhost";
        int colon = authority.indexOf(':');
        return colon < 0 ? authority : authority.substring(0, colon);
    }

    /** 從 jdbc:mysql://host:port/db 取出 port，未指定時用 3306。 */
    private String parsePort(String url) {
        String authority = authority(url);
        int colon = authority.indexOf(':');
        if (colon < 0) return "3306";
        String port = authority.substring(colon + 1);
        return port.matches("\\d+") ? port : "3306";
    }

    private String authority(String url) {
        int start = url.indexOf("//");
        if (start < 0) return "";
        String rest = url.substring(start + 2);
        int slash = rest.indexOf('/');
        return slash < 0 ? rest : rest.substring(0, slash);
    }
}
