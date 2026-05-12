package com.pobar.storage;

import com.pobar.common.ErrorCode;
import com.pobar.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * AWS S3 storage 實作骨架。
 * <p>
 * 啟用條件：{@code storage.type=s3}。
 * <p>
 * <b>未來實作指引</b>（上 AWS 時補完）：
 * <ol>
 *   <li>pom.xml 加入 {@code software.amazon.awssdk:s3} 依賴</li>
 *   <li>注入 {@code S3Client}，以 {@code StaticCredentialsProvider} 帶入 access/secret key</li>
 *   <li>{@code save}：呼叫 {@code s3Client.putObject(PutObjectRequest.builder()
 *       .bucket(bucket).key(folder + "/" + uuidName).build(), RequestBody.fromBytes(bytes))}，
 *       回傳 {@code "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key}</li>
 *   <li>{@code delete}：{@code s3Client.deleteObject(...)}，捕 {@code NoSuchKeyException} 吞掉</li>
 *   <li>{@code exists}：{@code s3Client.headObject(...)}，捕例外回傳 false</li>
 * </ol>
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "s3")
public class S3StorageService implements StorageService {

    @Value("${storage.s3.bucket:}")
    private String bucket;

    @Value("${storage.s3.region:}")
    private String region;

    @Value("${storage.s3.access-key:}")
    private String accessKey;

    @Value("${storage.s3.secret-key:}")
    private String secretKey;

    // TODO: 上 AWS 時補上實作（注入 software.amazon.awssdk.services.s3.S3Client）

    @Override
    public String save(byte[] bytes, String originalFilename, String folder) {
        // TODO: 上 AWS 時補上實作 — S3Client.putObject
        throw new BusinessException(ErrorCode.STORAGE_NOT_CONFIGURED,
                "S3 storage 尚未實作，請先在 pom.xml 加 software.amazon.awssdk:s3 依賴");
    }

    @Override
    public void delete(String urlOrKey) {
        // TODO: 上 AWS 時補上實作 — S3Client.deleteObject
        throw new BusinessException(ErrorCode.STORAGE_NOT_CONFIGURED,
                "S3 storage 尚未實作，請先在 pom.xml 加 software.amazon.awssdk:s3 依賴");
    }

    @Override
    public boolean exists(String urlOrKey) {
        // TODO: 上 AWS 時補上實作 — S3Client.headObject
        throw new BusinessException(ErrorCode.STORAGE_NOT_CONFIGURED,
                "S3 storage 尚未實作，請先在 pom.xml 加 software.amazon.awssdk:s3 依賴");
    }
}
