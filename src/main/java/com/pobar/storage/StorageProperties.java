package com.pobar.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 統一綁定 {@code storage.*} 設定，供 {@link LocalStorageService}、{@link S3StorageService} 共用。
 * <p>
 * 對應 properties：
 * <pre>
 * storage.type=local|s3
 * storage.local.allowed-extensions=jpg,jpeg,png
 * storage.local.max-size-mb=5
 * storage.s3.bucket=...
 * storage.s3.region=...
 * storage.s3.access-key=...
 * storage.s3.secret-key=...
 * </pre>
 */
@Data
@Component
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    /** local 或 s3。 */
    private String type = "local";

    private Local local = new Local();
    private S3 s3 = new S3();

    @Data
    public static class Local {
        private String allowedExtensions = "jpg,jpeg,png";
        private long maxSizeMb = 5;
    }

    @Data
    public static class S3 {
        private String bucket;
        private String region;
        private String accessKey;
        private String secretKey;
    }
}
