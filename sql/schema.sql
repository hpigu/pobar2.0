-- Pobar 酒吧系統 完整 Schema
-- MySQL 8.0
-- 換日時間：凌晨 4 點（SYSTEM_SETTING: business_day_reset_hour = 4）

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ─────────────────────────────────────────
-- 員工
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS `user` (
    `id`            INT          NOT NULL AUTO_INCREMENT,
    `account`  VARCHAR(50)  NOT NULL UNIQUE,
    `password` VARCHAR(100) NOT NULL COMMENT 'BCrypt hash，salt 已內含於 hash',
    `email`    VARCHAR(100),
    `phone`         VARCHAR(20),
    `role`          VARCHAR(20)  NOT NULL COMMENT 'ADMIN,MANAGER,WAITER,BARTENDER,KITCHEN',
    `is_active`     TINYINT(1)   NOT NULL DEFAULT 1,
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 登入失敗次數（防暴力破解）
CREATE TABLE IF NOT EXISTS `login_attempt` (
    `id`           INT         NOT NULL AUTO_INCREMENT,
    `account`      VARCHAR(50) NOT NULL,
    `fail_count`   INT         NOT NULL DEFAULT 0,
    `locked_until` DATETIME,
    `updated_at`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_account` (`account`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- JWT 黑名單（登出後 token 失效）
CREATE TABLE IF NOT EXISTS `jwt_blacklist` (
    `id`         INT          NOT NULL AUTO_INCREMENT,
    `token_hash` VARCHAR(64)  NOT NULL UNIQUE COMMENT 'SHA-256 of token',
    `expires_at` DATETIME     NOT NULL,
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_expires_at` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─────────────────────────────────────────
-- 桌位
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS `bar_table` (
    `id`        INT          NOT NULL AUTO_INCREMENT,
    `name`      VARCHAR(20)  NOT NULL COMMENT 'A1、吧台-1',
    `type`      VARCHAR(20)  NOT NULL COMMENT 'REGULAR, BAR_COUNTER',
    `capacity`  INT          NOT NULL DEFAULT 2,
    `pos_x`     DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT '視覺化座位圖 X 座標',
    `pos_y`     DECIMAL(6,2) NOT NULL DEFAULT 0,
    `is_locked` TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '鎖定不開放訂位',
    `is_active` TINYINT(1)   NOT NULL DEFAULT 1,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `table_session` (
    `id`           INT         NOT NULL AUTO_INCREMENT,
    `qr_token`     VARCHAR(36) NOT NULL UNIQUE COMMENT 'UUID，QR code 使用',
    `status`       VARCHAR(20) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN, CLOSED',
    `party_size`   INT         NOT NULL DEFAULT 1,
    `opened_at`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `closed_at`    DATETIME,
    `opened_by_id` INT         NOT NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_qr_token` (`qr_token`),
    INDEX `idx_status` (`status`),
    FOREIGN KEY (`opened_by_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `table_session_table` (
    `session_id` INT NOT NULL,
    `table_id`   INT NOT NULL,
    PRIMARY KEY (`session_id`, `table_id`),
    FOREIGN KEY (`session_id`) REFERENCES `table_session`(`id`),
    FOREIGN KEY (`table_id`)   REFERENCES `bar_table`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─────────────────────────────────────────
-- 訂位
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS `reservation` (
    `id`               INT         NOT NULL AUTO_INCREMENT,
    `customer_name`    VARCHAR(50) NOT NULL,
    `customer_phone`   VARCHAR(20) NOT NULL,
    `seat_type`        VARCHAR(20) NOT NULL COMMENT 'REGULAR, BAR_COUNTER',
    `party_size`       INT         NOT NULL,
    `reserved_at`      DATETIME    NOT NULL,
    `duration_minutes` INT         NOT NULL DEFAULT 120,
    `status`           VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED'
                       COMMENT 'CONFIRMED, CANCELLED, AUTO_CANCELLED, NO_SHOW, COMPLETED',
    `cancel_token`     VARCHAR(36) NOT NULL UNIQUE COMMENT 'UUID，線上取消用',
    `assigned_table_id` INT,
    `notes`            TEXT,
    `created_at`       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `cancelled_at`     DATETIME,
    PRIMARY KEY (`id`),
    INDEX `idx_reserved_at` (`reserved_at`),
    INDEX `idx_status` (`status`),
    INDEX `idx_cancel_token` (`cancel_token`),
    FOREIGN KEY (`assigned_table_id`) REFERENCES `bar_table`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─────────────────────────────────────────
-- 菜單 / 酒單
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS `category` (
    `id`            INT         NOT NULL AUTO_INCREMENT,
    `name_zh`       VARCHAR(50) NOT NULL,
    `name_en`       VARCHAR(50),
    `type`          VARCHAR(10) NOT NULL COMMENT 'FOOD, DRINK',
    `display_order` INT         NOT NULL DEFAULT 0,
    `is_active`     TINYINT(1)  NOT NULL DEFAULT 1,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `product` (
    `id`                  INT            NOT NULL AUTO_INCREMENT,
    `category_id`         INT            NOT NULL,
    `name_zh`             VARCHAR(100)   NOT NULL,
    `name_en`             VARCHAR(100),
    `description_zh`      TEXT,
    `description_en`      TEXT,
    `price`               DECIMAL(10, 0) NOT NULL,
    `type`                VARCHAR(10)    NOT NULL COMMENT 'FOOD, DRINK',
    `image_url`           VARCHAR(255),
    `is_active`           TINYINT(1)     NOT NULL DEFAULT 1 COMMENT '永久上下架',
    `is_available`        TINYINT(1)     NOT NULL DEFAULT 1 COMMENT '臨時售完',
    `available_start_time` TIME          COMMENT '每日可點時段開始，null 表示全天',
    `available_end_time`   TIME          COMMENT '每日可點時段結束',
    `available_from_date`  DATE          COMMENT '季節上架日',
    `available_to_date`    DATE          COMMENT '季節下架日',
    `created_by`          INT,
    `created_at`          DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`          DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_category_id` (`category_id`),
    INDEX `idx_type` (`type`),
    INDEX `idx_is_active_available` (`is_active`, `is_available`),
    FOREIGN KEY (`category_id`) REFERENCES `category`(`id`),
    FOREIGN KEY (`created_by`)  REFERENCES `user`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─────────────────────────────────────────
-- 酒單篩選維度
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS `drink_attribute_type` (
    `id`            INT         NOT NULL AUTO_INCREMENT,
    `name_zh`       VARCHAR(50) NOT NULL COMMENT '如：基酒、甜度、香氣',
    `name_en`       VARCHAR(50),
    `display_order` INT         NOT NULL DEFAULT 0,
    `is_active`     TINYINT(1)  NOT NULL DEFAULT 1,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `drink_attribute_option` (
    `id`              INT         NOT NULL AUTO_INCREMENT,
    `attribute_type_id` INT       NOT NULL,
    `name_zh`         VARCHAR(50) NOT NULL COMMENT '如：琴酒、微甜、花香',
    `name_en`         VARCHAR(50),
    `display_order`   INT         NOT NULL DEFAULT 0,
    `is_active`       TINYINT(1)  NOT NULL DEFAULT 1,
    PRIMARY KEY (`id`),
    INDEX `idx_type_id` (`attribute_type_id`),
    FOREIGN KEY (`attribute_type_id`) REFERENCES `drink_attribute_type`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `product_attribute` (
    `product_id`          INT NOT NULL,
    `attribute_option_id` INT NOT NULL,
    PRIMARY KEY (`product_id`, `attribute_option_id`),
    FOREIGN KEY (`product_id`)          REFERENCES `product`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`attribute_option_id`) REFERENCES `drink_attribute_option`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─────────────────────────────────────────
-- 酒譜 / 食材
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS `ingredient` (
    `id`           INT         NOT NULL AUTO_INCREMENT,
    `name`         VARCHAR(100) NOT NULL,
    `unit`         VARCHAR(20)  NOT NULL COMMENT 'ml, oz, 顆, 片...',
    `is_available` TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '缺貨時 false，連動下架相關酒品',
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `recipe` (
    `id`                INT  NOT NULL AUTO_INCREMENT,
    `product_id`        INT  NOT NULL UNIQUE COMMENT '一支酒一份酒譜',
    `preparation_notes` TEXT COMMENT '作法說明，如：搖盪法，雙重過濾',
    PRIMARY KEY (`id`),
    FOREIGN KEY (`product_id`) REFERENCES `product`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `recipe_ingredient` (
    `id`            INT            NOT NULL AUTO_INCREMENT,
    `recipe_id`     INT            NOT NULL,
    `ingredient_id` INT            NOT NULL,
    `quantity`      DECIMAL(8, 2)  NOT NULL,
    `unit`          VARCHAR(20)    NOT NULL,
    `display_order` INT            NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `idx_recipe_id` (`recipe_id`),
    FOREIGN KEY (`recipe_id`)     REFERENCES `recipe`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`ingredient_id`) REFERENCES `ingredient`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─────────────────────────────────────────
-- 點餐
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS `orders` (
    `id`         INT      NOT NULL AUTO_INCREMENT,
    `session_id` INT      NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_session_id` (`session_id`),
    FOREIGN KEY (`session_id`) REFERENCES `table_session`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `order_item` (
    `id`           INT            NOT NULL AUTO_INCREMENT,
    `order_id`     INT            NOT NULL,
    `product_id`   INT            NOT NULL,
    `quantity`     INT            NOT NULL DEFAULT 1,
    `price`        DECIMAL(10, 0) NOT NULL COMMENT '下單當下的價格快照',
    `notes`        VARCHAR(255)   COMMENT '備註，如：少冰',
    `type`         VARCHAR(10)    NOT NULL COMMENT 'FOOD, DRINK（冗餘存放，方便篩選）',
    `status`       VARCHAR(20)    NOT NULL DEFAULT 'PENDING'
                   COMMENT 'PENDING, IN_PROGRESS, READY, CANCELLED',
    `cancelled_by` INT            COMMENT '取消的服務生 user id',
    `cancelled_at` DATETIME,
    `created_at`   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_order_id` (`order_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_type_status` (`type`, `status`),
    FOREIGN KEY (`order_id`)     REFERENCES `orders`(`id`),
    FOREIGN KEY (`product_id`)   REFERENCES `product`(`id`),
    FOREIGN KEY (`cancelled_by`) REFERENCES `user`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─────────────────────────────────────────
-- 結帳 / 發票
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS `payment` (
    `id`                  INT            NOT NULL AUTO_INCREMENT,
    `session_id`          INT            NOT NULL UNIQUE,
    `subtotal`            DECIMAL(10, 0) NOT NULL,
    `service_charge_rate` DECIMAL(4, 2)  NOT NULL COMMENT '結帳當下費率快照，如 0.10',
    `service_charge`      DECIMAL(10, 0) NOT NULL,
    `total`               DECIMAL(10, 0) NOT NULL,
    `payment_method`      VARCHAR(20)    NOT NULL COMMENT 'CASH, CARD, OTHER',
    `split_count`         INT            NOT NULL DEFAULT 1 COMMENT '1 表示不分帳',
    `amount_per_person`   DECIMAL(10, 0),
    `processed_by`        INT            NOT NULL,
    `paid_at`             DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`session_id`)   REFERENCES `table_session`(`id`),
    FOREIGN KEY (`processed_by`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `invoice` (
    `id`             INT         NOT NULL AUTO_INCREMENT,
    `payment_id`     INT         NOT NULL UNIQUE,
    `invoice_number` VARCHAR(20) NOT NULL COMMENT '統一發票號碼',
    `carrier_type`   VARCHAR(20) NOT NULL COMMENT 'MOBILE_BARCODE, CITIZEN_CERT, PAPER',
    `carrier_id`     VARCHAR(50) COMMENT '手機條碼或憑證號碼',
    `status`         VARCHAR(20) NOT NULL DEFAULT 'ISSUED' COMMENT 'ISSUED, CANCELLED',
    `issued_at`      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`payment_id`) REFERENCES `payment`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─────────────────────────────────────────
-- 促銷（預留結構，暫不實作 UI）
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS `promotion` (
    `id`             INT            NOT NULL AUTO_INCREMENT,
    `name`           VARCHAR(100)   NOT NULL,
    `discount_type`  VARCHAR(20)    NOT NULL COMMENT 'PERCENTAGE, FIXED_AMOUNT',
    `discount_value` DECIMAL(10, 2) NOT NULL,
    `applies_to`     VARCHAR(20)    NOT NULL COMMENT 'ALL, DRINK, FOOD, CATEGORY',
    `category_id`    INT,
    `start_time`     TIME           COMMENT 'Happy Hour 開始時間',
    `end_time`       TIME,
    `start_date`     DATE,
    `end_date`       DATE,
    `is_active`      TINYINT(1)     NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`category_id`) REFERENCES `category`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─────────────────────────────────────────
-- 操作日誌（稽核紀錄）
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS `audit_log` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`     INT          COMMENT '操作者 ID，null 表示匿名（客人）',
    `account`     VARCHAR(50)  COMMENT '操作者帳號快照',
    `role`        VARCHAR(20)  COMMENT '操作者角色快照',
    `action`      VARCHAR(50)  NOT NULL COMMENT '操作代碼，如 LOGIN, CREATE_PRODUCT',
    `entity_type` VARCHAR(50)  COMMENT '操作對象類型，如 PRODUCT, ORDER',
    `entity_id`   VARCHAR(50)  COMMENT '操作對象 ID',
    `result`      VARCHAR(10)  NOT NULL COMMENT 'SUCCESS, FAIL',
    `detail`      TEXT         COMMENT '補充說明（不含敏感資料）',
    `ip`          VARCHAR(45)  NOT NULL,
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_action` (`action`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─────────────────────────────────────────
-- 系統設定
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS `system_setting` (
    `setting_key`   VARCHAR(50)  NOT NULL,
    `setting_value` TEXT         NOT NULL,
    `description`   VARCHAR(255),
    PRIMARY KEY (`setting_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `backup_log` (
    `id`                INT          NOT NULL AUTO_INCREMENT,
    `backup_at`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `file_name`         VARCHAR(100) NOT NULL,
    `file_size_bytes`   BIGINT,
    `status`            VARCHAR(10)  NOT NULL COMMENT 'SUCCESS, FAILED',
    `error_message`     TEXT,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─────────────────────────────────────────
-- 預設系統設定
-- ─────────────────────────────────────────
INSERT INTO `system_setting` (`setting_key`, `setting_value`, `description`) VALUES
('service_charge_rate',         '0.10',  '服務費率'),
('reservation_duration_minutes','120',   '每次訂位佔用時長（分鐘）'),
('no_show_cancel_minutes',      '10',    '逾時自動取消等待分鐘數'),
('business_day_reset_hour',     '4',     '換日時間（凌晨幾點算新的一天）'),
('food_service_start',          '17:00', '廚房開始服務時間'),
('food_service_end',            '22:00', '廚房結束服務時間'),
('drink_service_start',         '17:00', '酒水開始服務時間'),
('drink_service_end',           '02:00', '酒水結束服務時間（跨日）'),
('age_gate_enabled',            'true',  '是否顯示年齡確認彈窗'),
('order_rate_limit_per_min',    '5',     '每個 session 每分鐘最多送單次數'),
('max_items_per_order',         '20',    '每次送單最多品項數量')
ON DUPLICATE KEY UPDATE `setting_value` = VALUES(`setting_value`);

SET FOREIGN_KEY_CHECKS = 1;
