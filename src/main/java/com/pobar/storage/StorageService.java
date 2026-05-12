package com.pobar.storage;

/**
 * 檔案儲存抽象層。
 * <p>
 * 提供統一的 API 讓 Service 層存取上傳檔案，底層可切換：
 * <ul>
 *   <li>{@link LocalStorageService} — 開發 / SIT 用，存本機磁碟</li>
 *   <li>{@link S3StorageService}    — 正式環境用，存 AWS S3</li>
 * </ul>
 * 透過 {@code storage.type} property 決定 bean，呼叫方只依賴此介面。
 * <p>
 * URL 格式約定：
 * <ul>
 *   <li>Local：相對路徑 {@code /uploads/{folder}/{uuid.ext}}，由前端透過 static handler 取用</li>
 *   <li>S3   ：完整 https URL 或 {@code s3://bucket/key} 形式（依未來實作決定）</li>
 * </ul>
 */
public interface StorageService {

    /**
     * 儲存檔案。
     *
     * @param bytes            檔案位元組內容（非 null、非空）
     * @param originalFilename 原始檔名，用來推斷副檔名（例：{@code "photo.jpg"}）
     * @param folder           子資料夾名稱（例：{@code "menu"}、{@code "avatar"}）
     * @return 對外可用的 URL 或 key，前端可直接拿來放在 {@code <img src>}
     * @throws com.pobar.exception.BusinessException 檔案過大、型別不允許、寫入失敗時拋出
     */
    String save(byte[] bytes, String originalFilename, String folder);

    /**
     * 盡力刪除指定資源；找不到時不丟例外（log 後吞掉）。
     *
     * @param urlOrKey {@link #save} 回傳的 URL 或 key
     */
    void delete(String urlOrKey);

    /**
     * 檢查資源是否存在。
     *
     * @param urlOrKey {@link #save} 回傳的 URL 或 key
     * @return true 若實際資源仍可被讀取
     */
    boolean exists(String urlOrKey);
}
