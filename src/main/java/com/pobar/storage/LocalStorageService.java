package com.pobar.storage;

import com.pobar.common.ErrorCode;
import com.pobar.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 本機檔案系統 storage 實作。
 * <p>
 * 啟用條件：{@code storage.type=local}（未設定亦預設啟用）。
 * 檔案實際存放於 {@code ${file.upload-dir}/{folder}/{uuid.ext}}，
 * 對外回傳 {@code /uploads/{folder}/{uuid.ext}}，由 {@code WebConfig} 靜態資源 handler 提供存取。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${storage.local.allowed-extensions:jpg,jpeg,png}")
    private String allowedExtensionsRaw;

    @Value("${storage.local.max-size-mb:5}")
    private long maxSizeMb;

    @Override
    public String save(byte[] bytes, String originalFilename, String folder) {
        if (bytes == null || bytes.length == 0) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "上傳檔案為空");
        }

        long maxBytes = maxSizeMb * 1024L * 1024L;
        if (bytes.length > maxBytes) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE,
                    "檔案超過限制 " + maxSizeMb + "MB");
        }

        String ext = extractExtension(originalFilename);
        Set<String> allowed = parseAllowedExtensions();
        if (!allowed.contains(ext)) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED,
                    "不支援的檔案類型：" + ext + "，僅允許 " + allowed);
        }

        String safeFolder = (folder == null || folder.isBlank()) ? "misc" : folder.trim();
        String uuidName = UUID.randomUUID() + "." + ext;

        try {
            Path targetDir = Paths.get(uploadDir, safeFolder);
            Files.createDirectories(targetDir);
            Path targetFile = targetDir.resolve(uuidName);
            Files.write(targetFile, bytes);
            log.info("Saved local file: {}", targetFile.toAbsolutePath());
            return "/uploads/" + safeFolder + "/" + uuidName;
        } catch (IOException e) {
            log.error("Failed to write local file to {}/{}/{}", uploadDir, safeFolder, uuidName, e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "檔案寫入失敗：" + e.getMessage());
        }
    }

    @Override
    public void delete(String urlOrKey) {
        Path file = resolve(urlOrKey);
        if (file == null) return;
        try {
            boolean deleted = Files.deleteIfExists(file);
            if (deleted) log.info("Deleted local file: {}", file);
            else log.debug("File not found, skip delete: {}", file);
        } catch (IOException e) {
            log.warn("Failed to delete local file {}: {}", file, e.getMessage());
        }
    }

    @Override
    public boolean exists(String urlOrKey) {
        Path file = resolve(urlOrKey);
        return file != null && Files.exists(file);
    }

    private Path resolve(String urlOrKey) {
        if (urlOrKey == null || urlOrKey.isBlank()) return null;
        String relative = urlOrKey.startsWith("/uploads/")
                ? urlOrKey.substring("/uploads/".length())
                : urlOrKey;
        return Paths.get(uploadDir, relative).normalize();
    }

    private String extractExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) return "";
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private Set<String> parseAllowedExtensions() {
        return Arrays.stream(allowedExtensionsRaw.split(","))
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(HashSet::new));
    }
}
