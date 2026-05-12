-- ============================================================
-- Pobar 酒吧系統 — 完整初始化腳本（全新安裝用）
-- MySQL 8.0
-- 包含：Schema、預設系統設定、酒單種子資料、食材與酒譜種子資料
--
-- ⚠️  本檔僅供全新資料庫初始化（docker-entrypoint-initdb.d）
--    既有資料庫升級請改用 sql/migrations/ 下的腳本
-- ============================================================

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
    `must_change_password` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '為 1 時首次登入強制改密碼',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='員工帳號與角色管理';

-- 登入失敗次數（依 (account, ip) 雙維度鎖定）
CREATE TABLE IF NOT EXISTS `login_attempt` (
    `id`           INT         NOT NULL AUTO_INCREMENT,
    `account`      VARCHAR(50) NOT NULL,
    `ip`           VARCHAR(45) NOT NULL COMMENT '來源 IP (IPv4/IPv6)',
    `fail_count`   INT         NOT NULL DEFAULT 0,
    `locked_until` DATETIME,
    `updated_at`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_account_ip` (`account`, `ip`),
    INDEX `idx_ip_updated` (`ip`, `updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登入失敗次數（account+ip 雙維度），防暴力破解鎖定';

-- IP 層級鎖定（短時間內跨多個 account 失敗，視為攻擊行為，鎖 IP）
CREATE TABLE IF NOT EXISTS `ip_lockout` (
    `id`           INT         NOT NULL AUTO_INCREMENT,
    `ip`           VARCHAR(45) NOT NULL,
    `locked_until` DATETIME    NOT NULL,
    `reason`       VARCHAR(100),
    `created_at`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_ip` (`ip`),
    INDEX `idx_locked_until` (`locked_until`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IP 層級鎖定（防分散式暴力破解）';

-- JWT 黑名單（登出後 token 失效）
CREATE TABLE IF NOT EXISTS `jwt_blacklist` (
    `id`         INT          NOT NULL AUTO_INCREMENT,
    `token_hash` VARCHAR(64)  NOT NULL UNIQUE COMMENT 'SHA-256 of token',
    `expires_at` DATETIME     NOT NULL,
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_expires_at` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='已登出的 JWT Token 黑名單';

-- Refresh Token（雙 token 機制：短 access + 長 refresh，支援本機記住）
CREATE TABLE IF NOT EXISTS `refresh_token` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`       INT          NOT NULL,
    `token_hash`    VARCHAR(64)  NOT NULL UNIQUE COMMENT 'SHA-256 of refresh token',
    `expires_at`    DATETIME     NOT NULL,
    `trusted`       TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否為信任裝置（本機記住），決定 TTL',
    `revoked`       TINYINT(1)   NOT NULL DEFAULT 0,
    `user_agent`    VARCHAR(255),
    `ip`            VARCHAR(45),
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `last_used_at`  DATETIME,
    PRIMARY KEY (`id`),
    INDEX `idx_user_revoked` (`user_id`, `revoked`),
    INDEX `idx_expires_at`   (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Refresh Token（雙 token 機制）';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='實體桌位設定（含座位圖座標）';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='桌位使用 Session，產生 QR Code 供客人點餐';

CREATE TABLE IF NOT EXISTS `table_session_table` (
    `session_id` INT NOT NULL,
    `table_id`   INT NOT NULL,
    PRIMARY KEY (`session_id`, `table_id`),
    FOREIGN KEY (`session_id`) REFERENCES `table_session`(`id`),
    FOREIGN KEY (`table_id`)   REFERENCES `bar_table`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Session 與桌位的多對多關聯（合桌使用）';

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
    `booking_code`     VARCHAR(10) NOT NULL DEFAULT '' COMMENT '顧客查詢用 8 位代碼',
    `assigned_table_id` INT,
    `notes`            TEXT,
    `created_at`       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `cancelled_at`     DATETIME,
    PRIMARY KEY (`id`),
    INDEX `idx_reserved_at` (`reserved_at`),
    INDEX `idx_status` (`status`),
    INDEX `idx_cancel_token` (`cancel_token`),
    INDEX `idx_phone_code` (`customer_phone`, `booking_code`),
    FOREIGN KEY (`assigned_table_id`) REFERENCES `bar_table`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客人線上訂位紀錄';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='品項分類（DRINK / FOOD）';

CREATE TABLE IF NOT EXISTS `product` (
    `id`                  INT            NOT NULL AUTO_INCREMENT,
    `category_id`         INT            NOT NULL,
    `name_zh`             VARCHAR(100)   NOT NULL,
    `name_en`             VARCHAR(100),
    `price`               DECIMAL(10, 0) NOT NULL,
    `type`                VARCHAR(10)    NOT NULL COMMENT 'FOOD, DRINK',
    `image_url`           VARCHAR(255),
    `is_active`           TINYINT(1)     NOT NULL DEFAULT 1 COMMENT '永久上下架',
    `is_available`        TINYINT(1)     NOT NULL DEFAULT 1 COMMENT '臨時售完',
    `available_from` DATETIME COMMENT '供應開始時間（含日期與時間，null 表示無限制）',
    `available_to`   DATETIME COMMENT '供應結束時間（含日期與時間）',
    `created_by`          VARCHAR(50),
    `created_at`          DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`          DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_category_id` (`category_id`),
    INDEX `idx_type` (`type`),
    INDEX `idx_is_active_available` (`is_active`, `is_available`),
    FOREIGN KEY (`category_id`) REFERENCES `category`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='酒單與餐點品項（含供應時間與圖片）';

-- ─────────────────────────────────────────
-- 酒譜 / 食材
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS `ingredient` (
    `id`           INT         NOT NULL AUTO_INCREMENT,
    `name`         VARCHAR(100) NOT NULL,
    `unit`         VARCHAR(20)  NOT NULL COMMENT 'ml, oz, 顆, 片...',
    `category`     VARCHAR(30)  NOT NULL DEFAULT 'OTHER' COMMENT 'BASE_SPIRIT/LIQUEUR/WINE/BEER/SYRUP/JUICE/FRESH/GARNISH/OTHER',
    `is_available` TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '缺貨時 false，連動下架相關酒品',
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ingredient_name` (`name`),
    KEY `idx_ingredient_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='調酒用食材（基酒、利口酒、果汁等）';

CREATE TABLE IF NOT EXISTS `recipe` (
    `id`                INT  NOT NULL AUTO_INCREMENT,
    `product_id`        INT  NOT NULL UNIQUE COMMENT '一支酒一份酒譜',
    `preparation_notes` TEXT COMMENT '作法說明，如：搖盪法，雙重過濾',
    PRIMARY KEY (`id`),
    FOREIGN KEY (`product_id`) REFERENCES `product`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='品項對應的調酒酒譜（一品項一份）';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='酒譜食材明細與用量';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客人在一個 session 內的送單紀錄';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='訂單品項明細，追蹤廚房 / 吧台製作狀態';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='結帳記錄（含服務費與支付方式）';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='統一發票紀錄';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系統操作稽核日誌';

-- ─────────────────────────────────────────
-- 系統設定
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS `system_setting` (
    `setting_key`   VARCHAR(50)  NOT NULL,
    `setting_value` TEXT         NOT NULL,
    `description`   VARCHAR(255),
    PRIMARY KEY (`setting_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系統參數（KV 結構）';

CREATE TABLE IF NOT EXISTS `backup_log` (
    `id`                INT          NOT NULL AUTO_INCREMENT,
    `backup_at`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `file_name`         VARCHAR(100) NOT NULL,
    `file_size_bytes`   BIGINT,
    `status`            VARCHAR(10)  NOT NULL COMMENT 'SUCCESS, FAILED',
    `error_message`     TEXT,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='資料庫備份紀錄';

SET FOREIGN_KEY_CHECKS = 1;

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

-- ============================================================
-- 種子資料：預設酒單
-- 來源：29_酒譜(43款)1100602更新.docx（共 42 款，Sangria 略）
-- ============================================================

-- ─────────────────────────────────────────
-- 分類（category）
-- ─────────────────────────────────────────
INSERT INTO category (name_zh, name_en, type, display_order, is_active) VALUES
  ('琴酒調酒',   'Gin Cocktails',     'DRINK', 1, 1),
  ('威士忌調酒', 'Whisky Cocktails',  'DRINK', 2, 1),
  ('白蘭地調酒', 'Brandy Cocktails',  'DRINK', 3, 1),
  ('蘭姆酒調酒', 'Rum Cocktails',     'DRINK', 4, 1),
  ('伏特加調酒', 'Vodka Cocktails',   'DRINK', 5, 1),
  ('綜合調酒',   'Mixed Cocktails',   'DRINK', 6, 1);

-- ─────────────────────────────────────────
-- 品項（product）
-- ─────────────────────────────────────────

-- 琴酒調酒（9 款）
INSERT INTO product (category_id, name_zh, name_en, price, type, is_active, is_available, created_by) VALUES
((SELECT id FROM category WHERE name_zh = '琴酒調酒' LIMIT 1), '琴費士',     'Gin Fizz',          350, 'DRINK', 1, 1, 'admin'),
((SELECT id FROM category WHERE name_zh = '琴酒調酒' LIMIT 1), '藍鳥',       'Blue Bird',         350, 'DRINK', 1, 1, 'admin'),
((SELECT id FROM category WHERE name_zh = '琴酒調酒' LIMIT 1), '白色佳人',   'White Lady',        350, 'DRINK', 1, 1, 'admin'),
((SELECT id FROM category WHERE name_zh = '琴酒調酒' LIMIT 1), '橘花',       'Orange Blossom',    350, 'DRINK', 1, 1, 'admin'),
((SELECT id FROM category WHERE name_zh = '琴酒調酒' LIMIT 1), '新加坡司令', 'Singapore Sling',   350, 'DRINK', 1, 1, 'admin'),
((SELECT id FROM category WHERE name_zh = '琴酒調酒' LIMIT 1), '琴蕾',       'Gimlet',            350, 'DRINK', 1, 1, 'admin'),
((SELECT id FROM category WHERE name_zh = '琴酒調酒' LIMIT 1), '不甜馬丁尼', 'Dry Martini',       350, 'DRINK', 1, 1, 'admin'),
((SELECT id FROM category WHERE name_zh = '琴酒調酒' LIMIT 1), '尼格尼羅',   'Negroni',           350, 'DRINK', 1, 1, 'admin'),
((SELECT id FROM category WHERE name_zh = '琴酒調酒' LIMIT 1), '茶通寧',     'Tea Tonic',         350, 'DRINK', 1, 1, 'admin');

-- 威士忌調酒（15 款）
INSERT INTO product (category_id, name_zh, name_en, price, type, is_active, is_available, created_by) VALUES
((SELECT id FROM category WHERE name_zh = '威士忌調酒' LIMIT 1), '蘇格蘭蘇打',     'Scotch Soda',      350, 'DRINK', 1, 1, 'admin'),
((SELECT id FROM category WHERE name_zh = '威士忌調酒' LIMIT 1), '加拿大七喜',     'Canadian 7-UP',    350, 'DRINK', 1, 1, 'admin'),
((SELECT id FROM category WHERE name_zh = '威士忌調酒' LIMIT 1), '約翰可林',       'John Collins',     350, 'DRINK', 1, 1, 'admin'),
((SELECT id FROM category WHERE name_zh = '威士忌調酒' LIMIT 1), '領航者可林',     'Captain Collins',  350, 'DRINK', 1, 1, 'admin'),
((SELECT id FROM category WHERE name_zh = '威士忌調酒' LIMIT 1), '曼哈頓',         'Manhattan',        350, 'DRINK', 1, 1, 'admin'),
((SELECT id FROM category WHERE name_zh = '威士忌調酒' LIMIT 1), '不甜曼哈頓',     'Dry Manhattan',    350, 'DRINK', 1, 1, 'admin'),
((SELECT id FROM category WHERE name_zh = '威士忌調酒' LIMIT 1), '威士忌酸酒',     'Whisky Sour',      350, 'DRINK', 1, 1, 'admin'),
((SELECT id FROM category WHERE name_zh = '威士忌調酒' LIMIT 1), '沉默的第三者',   'Silent Third',     350, 'DRINK', 1, 1, 'admin'),
((SELECT id FROM category WHERE name_zh = '威士忌調酒' LIMIT 1), '教父',           'God Father',       350, 'DRINK', 1, 1, 'admin'),
((SELECT id FROM category WHERE name_zh = '威士忌調酒' LIMIT 1), '鏽釘子',         'Rusty Nail',       350, 'DRINK', 1, 1, 'admin'),
((SELECT id FROM category WHERE name_zh = '威士忌調酒' LIMIT 1), '古典酒',         'Old Fashioned',    350, 'DRINK', 1, 1, 'admin'),
((SELECT id FROM category WHERE name_zh = '威士忌調酒' LIMIT 1), '花花公子',       'Boulevardier',     350, 'DRINK', 1, 1, 'admin'),
((SELECT id FROM category WHERE name_zh = '威士忌調酒' LIMIT 1), '蘋果曼哈頓',     'Apple Manhattan',  350, 'DRINK', 1, 1, 'admin'),
((SELECT id FROM category WHERE name_zh = '威士忌調酒' LIMIT 1), '愛爾蘭汽車炸彈', 'Irish Car Bomb',   350, 'DRINK', 1, 1, 'admin'),
((SELECT id FROM category WHERE name_zh = '威士忌調酒' LIMIT 1), '老夥伴',         'Old Pal',          350, 'DRINK', 1, 1, 'admin');

-- 白蘭地調酒（11 款）
INSERT INTO product (category_id, name_zh, name_en, price, type, is_active, is_available, created_by) VALUES
((SELECT id FROM category WHERE name_zh = '白蘭地調酒' LIMIT 1), '熱托地',       'Hot Toddy',             350, 'DRINK', 1, 1, 'admin'),
((SELECT id FROM category WHERE name_zh = '白蘭地調酒' LIMIT 1), '馬頸',         'Horse''s Neck',         350, 'DRINK', 1, 1, 'admin'),
((SELECT id FROM category WHERE name_zh = '白蘭地調酒' LIMIT 1), '白蘭地巴克',   'Brandy Back',           350, 'DRINK', 1, 1, 'admin'),
((SELECT id FROM category WHERE name_zh = '白蘭地調酒' LIMIT 1), '白蘭地蘇打',   'Brandy Soda',           350, 'DRINK', 1, 1, 'admin'),
((SELECT id FROM category WHERE name_zh = '白蘭地調酒' LIMIT 1), '白蘭地亞歷山大','Alexander',            350, 'DRINK', 1, 1, 'admin'),
((SELECT id FROM category WHERE name_zh = '白蘭地調酒' LIMIT 1), '東印度大樓',   'East India House',      350, 'DRINK', 1, 1, 'admin'),
((SELECT id FROM category WHERE name_zh = '白蘭地調酒' LIMIT 1), '床第之間',     'Between the Sheets',    350, 'DRINK', 1, 1, 'admin'),
((SELECT id FROM category WHERE name_zh = '白蘭地調酒' LIMIT 1), '傑克蘿絲',     'Jack Rose',             350, 'DRINK', 1, 1, 'admin'),
((SELECT id FROM category WHERE name_zh = '白蘭地調酒' LIMIT 1), '尼古拉斯',     'Nikolaschka',           350, 'DRINK', 1, 1, 'admin'),
((SELECT id FROM category WHERE name_zh = '白蘭地調酒' LIMIT 1), '醉漢',         'Stingery',              350, 'DRINK', 1, 1, 'admin'),
((SELECT id FROM category WHERE name_zh = '白蘭地調酒' LIMIT 1), '蛋酒',         'Egg Nog',               350, 'DRINK', 1, 1, 'admin');

-- 蘭姆酒調酒（3 款）
INSERT INTO product (category_id, name_zh, name_en, price, type, is_active, is_available, created_by) VALUES
((SELECT id FROM category WHERE name_zh = '蘭姆酒調酒' LIMIT 1), '莫希多',     'Mojito',          350, 'DRINK', 1, 1, 'admin'),
((SELECT id FROM category WHERE name_zh = '蘭姆酒調酒' LIMIT 1), '拓荒者賓治', 'Planter''s Punch', 350, 'DRINK', 1, 1, 'admin'),
((SELECT id FROM category WHERE name_zh = '蘭姆酒調酒' LIMIT 1), '邁泰',       'Mai Tai',         350, 'DRINK', 1, 1, 'admin');

-- 伏特加調酒（2 款）
INSERT INTO product (category_id, name_zh, name_en, price, type, is_active, is_available, created_by) VALUES
((SELECT id FROM category WHERE name_zh = '伏特加調酒' LIMIT 1), '螺絲起子',   'Screwdriver',             350, 'DRINK', 1, 1, 'admin'),
((SELECT id FROM category WHERE name_zh = '伏特加調酒' LIMIT 1), '灰狗／鹹狗', 'Greyhound / Salty Dog',   350, 'DRINK', 1, 1, 'admin');

-- 綜合調酒（2 款）
INSERT INTO product (category_id, name_zh, name_en, price, type, is_active, is_available, created_by) VALUES
((SELECT id FROM category WHERE name_zh = '綜合調酒' LIMIT 1), '環遊世界', 'Around the World',        350, 'DRINK', 1, 1, 'admin'),
((SELECT id FROM category WHERE name_zh = '綜合調酒' LIMIT 1), '長島冰茶', 'Long Island Iced Tea',    350, 'DRINK', 1, 1, 'admin');

-- ============================================================
-- 種子資料：食材與酒譜
-- 依據 29_酒譜(43款)1100602更新.docx
-- ============================================================

-- 重跑前先清除酒譜資料（ingredient 有 UNIQUE KEY 可安全 INSERT IGNORE，不需清）
DELETE FROM recipe_ingredient;
DELETE FROM recipe;

-- ─────────────────────────────────────────
-- 食材（ingredient）
-- ─────────────────────────────────────────
INSERT IGNORE INTO ingredient (name, unit, category, is_available) VALUES
-- 基酒
('琴酒',          'ml', 'BASE_SPIRIT', 1),
('蘇格蘭威士忌',  'ml', 'BASE_SPIRIT', 1),
('加拿大威士忌',  'ml', 'BASE_SPIRIT', 1),
('波本威士忌',    'ml', 'BASE_SPIRIT', 1),
('愛爾蘭調和威士忌', 'ml', 'BASE_SPIRIT', 1),
('愛爾蘭威士忌',  'ml', 'BASE_SPIRIT', 1),
('蘇格蘭調和威士忌', 'ml', 'BASE_SPIRIT', 1),
('裸麥威士忌',    'ml', 'BASE_SPIRIT', 1),
('白蘭地',        'ml', 'BASE_SPIRIT', 1),
('蘋果白蘭地',    'ml', 'BASE_SPIRIT', 1),
('干邑白蘭地',    'ml', 'BASE_SPIRIT', 1),
('香澄干邑白蘭地','ml', 'BASE_SPIRIT', 1),
('白色蘭姆酒',    'ml', 'BASE_SPIRIT', 1),
('深色蘭姆酒',    'ml', 'BASE_SPIRIT', 1),
('伏特加',        'ml', 'BASE_SPIRIT', 1),
('龍舌蘭',        'ml', 'BASE_SPIRIT', 1),
-- 利口酒 / 香甜酒
('藍柑橘香甜酒',  'ml', 'LIQUEUR', 1),
('君度橙皮香甜酒','ml', 'LIQUEUR', 1),
('杏仁糖漿',      'ml', 'SYRUP',   1),
('甜苦艾酒',      'ml', 'LIQUEUR', 1),
('不甜苦艾酒',    'ml', 'LIQUEUR', 1),
('金巴利苦酒',    'ml', 'LIQUEUR', 1),
('班尼狄克丁',    'ml', 'LIQUEUR', 1),
('紅石榴糖漿',    'ml', 'SYRUP',   1),
('Triple Sec',    'ml', 'LIQUEUR', 1),
('杏仁香甜酒',    'ml', 'LIQUEUR', 1),
('蜂蜜香甜酒',    'ml', 'LIQUEUR', 1),
('青蘋果香甜酒',  'ml', 'LIQUEUR', 1),
('奶酒',          'ml', 'LIQUEUR', 1),
('深可可香甜酒',  'ml', 'LIQUEUR', 1),
('白薄荷香甜酒',  'ml', 'LIQUEUR', 1),
('柑橘香甜酒',    'ml', 'LIQUEUR', 1),
('櫻桃白蘭地',    'ml', 'LIQUEUR', 1),
-- 果汁 / 汽水 / 其他飲料
('新鮮檸檬汁',    'ml', 'JUICE', 1),
('新鮮柳橙汁',    'ml', 'JUICE', 1),
('鳳梨汁',        'ml', 'JUICE', 1),
('葡萄柚汁',      'ml', 'JUICE', 1),
('萊姆汁',        'ml', 'JUICE', 1),
('薑汁汽水',      'ml', 'JUICE', 1),
('蘇打水',        'ml', 'JUICE', 1),
('通寧水',        'ml', 'JUICE', 1),
('七喜',          'ml', 'JUICE', 1),
('可樂',          'ml', 'JUICE', 1),
('健力士黑啤酒',  'ml', 'BEER',  1),
('熱開水',        'ml', 'JUICE', 1),
-- 調味料 / 糖漿
('果糖',          'ml', 'SYRUP', 1),
('安格式苦精',    'dash','OTHER', 1),
('糖',            'g',  'SYRUP', 1),
-- 其他食材
('新鮮薄荷葉',    '片', 'FRESH', 1),
('鮮奶',          'ml', 'FRESH', 1),
('蛋黃',          '顆', 'FRESH', 1),
('奶精',          'ml', 'OTHER', 1),
('伯爵茶包',      '包', 'OTHER', 1);

-- ─────────────────────────────────────────
-- 酒譜與食材聯動
-- ─────────────────────────────────────────

-- 琴費士
INSERT INTO recipe (product_id, preparation_notes) SELECT id, '搖盪法 / 高飛球杯' FROM product WHERE name_zh = '琴費士' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 45, 'ml', 1 FROM ingredient WHERE name = '琴酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 30, 'ml', 2 FROM ingredient WHERE name = '新鮮檸檬汁';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 15, 'ml', 3 FROM ingredient WHERE name = '果糖';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 0, '適量', 4 FROM ingredient WHERE name = '蘇打水';

-- 藍鳥
INSERT INTO recipe (product_id, preparation_notes) SELECT id, '搖盪法 / 雞尾酒杯' FROM product WHERE name_zh = '藍鳥' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 30, 'ml', 1 FROM ingredient WHERE name = '琴酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 15, 'ml', 2 FROM ingredient WHERE name = '藍柑橘香甜酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 15, 'ml', 3 FROM ingredient WHERE name = '新鮮檸檬汁';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 10, 'ml', 4 FROM ingredient WHERE name = '杏仁糖漿';

-- 白色佳人
INSERT INTO recipe (product_id, preparation_notes) SELECT id, '搖盪法 / 雞尾酒杯' FROM product WHERE name_zh = '白色佳人' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 40, 'ml', 1 FROM ingredient WHERE name = '琴酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 20, 'ml', 2 FROM ingredient WHERE name = '君度橙皮香甜酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 15, 'ml', 3 FROM ingredient WHERE name = '新鮮檸檬汁';

-- 橘花
INSERT INTO recipe (product_id, preparation_notes) SELECT id, '搖盪法 / 雞尾酒杯' FROM product WHERE name_zh = '橘花' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 30, 'ml', 1 FROM ingredient WHERE name = '琴酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 15, 'ml', 2 FROM ingredient WHERE name = '甜苦艾酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 30, 'ml', 3 FROM ingredient WHERE name = '新鮮柳橙汁';

-- 新加坡司令
INSERT INTO recipe (product_id, preparation_notes) SELECT id, '搖盪法 / 可林杯' FROM product WHERE name_zh = '新加坡司令' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 30, 'ml', 1 FROM ingredient WHERE name = '琴酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 15, 'ml', 2 FROM ingredient WHERE name = '櫻桃白蘭地';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 10, 'ml', 3 FROM ingredient WHERE name = '君度橙皮香甜酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 10, 'ml', 4 FROM ingredient WHERE name = '班尼狄克丁';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 10, 'ml', 5 FROM ingredient WHERE name = '紅石榴糖漿';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 90, 'ml', 6 FROM ingredient WHERE name = '鳳梨汁';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 30, 'ml', 7 FROM ingredient WHERE name = '新鮮檸檬汁';

-- 琴蕾
INSERT INTO recipe (product_id, preparation_notes) SELECT id, '攪拌法 / 雞尾酒杯' FROM product WHERE name_zh = '琴蕾' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 45, 'ml', 1 FROM ingredient WHERE name = '琴酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 30, 'ml', 2 FROM ingredient WHERE name = '萊姆汁';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 15, 'ml', 3 FROM ingredient WHERE name = '果糖';

-- 不甜馬丁尼
INSERT INTO recipe (product_id, preparation_notes) SELECT id, '攪拌法 / 馬丁尼杯，紅心橄欖裝飾' FROM product WHERE name_zh = '不甜馬丁尼' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 45, 'ml', 1 FROM ingredient WHERE name = '琴酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 15, 'ml', 2 FROM ingredient WHERE name = '不甜苦艾酒';

-- 尼格尼羅
INSERT INTO recipe (product_id, preparation_notes) SELECT id, '攪拌法 / 古典酒杯，柳橙皮裝飾' FROM product WHERE name_zh = '尼格尼羅' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 30, 'ml', 1 FROM ingredient WHERE name = '琴酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 30, 'ml', 2 FROM ingredient WHERE name = '甜苦艾酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 30, 'ml', 3 FROM ingredient WHERE name = '金巴利苦酒';

-- 茶通寧
INSERT INTO recipe (product_id, preparation_notes) SELECT id, '直接注入法 / 雞尾酒杯' FROM product WHERE name_zh = '茶通寧' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 30, 'ml', 1 FROM ingredient WHERE name = '琴酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 150, 'ml', 2 FROM ingredient WHERE name = '通寧水';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 1, '包', 3 FROM ingredient WHERE name = '伯爵茶包';

-- 蘇格蘭蘇打
INSERT INTO recipe (product_id, preparation_notes) SELECT id, '直接注入法 / 高飛球杯，檸檬片裝飾' FROM product WHERE name_zh = '蘇格蘭蘇打' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 30, 'ml', 1 FROM ingredient WHERE name = '蘇格蘭威士忌';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 0, '適量', 2 FROM ingredient WHERE name = '蘇打水';

-- 加拿大七喜
INSERT INTO recipe (product_id, preparation_notes) SELECT id, '直接注入法 / 高飛球杯，檸檬角裝飾' FROM product WHERE name_zh = '加拿大七喜' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 30, 'ml', 1 FROM ingredient WHERE name = '加拿大威士忌';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 0, '適量', 2 FROM ingredient WHERE name = '七喜';

-- 約翰可林
INSERT INTO recipe (product_id, preparation_notes) SELECT id, '直接注入法 / 可林杯' FROM product WHERE name_zh = '約翰可林' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 45, 'ml', 1 FROM ingredient WHERE name = '波本威士忌';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 30, 'ml', 2 FROM ingredient WHERE name = '新鮮檸檬汁';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 15, 'ml', 3 FROM ingredient WHERE name = '果糖';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 0, '適量', 4 FROM ingredient WHERE name = '蘇打水';

-- 領航者可林
INSERT INTO recipe (product_id, preparation_notes) SELECT id, '搖盪法 / 可林杯' FROM product WHERE name_zh = '領航者可林' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 30, 'ml', 1 FROM ingredient WHERE name = '加拿大威士忌';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 30, 'ml', 2 FROM ingredient WHERE name = '新鮮檸檬汁';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 10, 'ml', 3 FROM ingredient WHERE name = '果糖';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 0, '適量', 4 FROM ingredient WHERE name = '蘇打水';

-- 曼哈頓
INSERT INTO recipe (product_id, preparation_notes) SELECT id, '攪拌法 / 馬丁尼杯，櫻桃裝飾' FROM product WHERE name_zh = '曼哈頓' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 45, 'ml', 1 FROM ingredient WHERE name = '波本威士忌';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 15, 'ml', 2 FROM ingredient WHERE name = '甜苦艾酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 2, 'dash', 3 FROM ingredient WHERE name = '安格式苦精';

-- 不甜曼哈頓
INSERT INTO recipe (product_id, preparation_notes) SELECT id, '攪拌法 / 馬丁尼杯，檸檬皮裝飾' FROM product WHERE name_zh = '不甜曼哈頓' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 45, 'ml', 1 FROM ingredient WHERE name = '波本威士忌';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 15, 'ml', 2 FROM ingredient WHERE name = '不甜苦艾酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 2, 'dash', 3 FROM ingredient WHERE name = '安格式苦精';

-- 威士忌酸酒
INSERT INTO recipe (product_id, preparation_notes) SELECT id, '搖盪法 / 酸酒杯' FROM product WHERE name_zh = '威士忌酸酒' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 45, 'ml', 1 FROM ingredient WHERE name = '波本威士忌';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 30, 'ml', 2 FROM ingredient WHERE name = '新鮮檸檬汁';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 30, 'ml', 3 FROM ingredient WHERE name = '果糖';

-- 沉默的第三者
INSERT INTO recipe (product_id, preparation_notes) SELECT id, '搖盪法 / 雞尾酒杯' FROM product WHERE name_zh = '沉默的第三者' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 15, 'ml', 1 FROM ingredient WHERE name = '愛爾蘭調和威士忌';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 20, 'ml', 2 FROM ingredient WHERE name = 'Triple Sec';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 20, 'ml', 3 FROM ingredient WHERE name = '新鮮檸檬汁';

-- 教父
INSERT INTO recipe (product_id, preparation_notes) SELECT id, '直接注入法 / 古典酒杯' FROM product WHERE name_zh = '教父' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 45, 'ml', 1 FROM ingredient WHERE name = '蘇格蘭調和威士忌';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 15, 'ml', 2 FROM ingredient WHERE name = '杏仁香甜酒';

-- 鏽釘子
INSERT INTO recipe (product_id, preparation_notes) SELECT id, '攪拌法 / 雞尾酒杯' FROM product WHERE name_zh = '鏽釘子' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 45, 'ml', 1 FROM ingredient WHERE name = '蘇格蘭調和威士忌';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 30, 'ml', 2 FROM ingredient WHERE name = '蜂蜜香甜酒';

-- 古典酒
INSERT INTO recipe (product_id, preparation_notes) SELECT id, '直接注入法 / 古典酒杯' FROM product WHERE name_zh = '古典酒' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 45, 'ml', 1 FROM ingredient WHERE name = '波本威士忌';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 2, 'dash', 2 FROM ingredient WHERE name = '安格式苦精';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 8, 'g', 3 FROM ingredient WHERE name = '糖';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 0, '少許', 4 FROM ingredient WHERE name = '蘇打水';

-- 花花公子
INSERT INTO recipe (product_id, preparation_notes) SELECT id, '攪拌法 / 雞尾酒杯，柳橙皮裝飾' FROM product WHERE name_zh = '花花公子' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 30, 'ml', 1 FROM ingredient WHERE name = '波本威士忌';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 30, 'ml', 2 FROM ingredient WHERE name = '金巴利苦酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 30, 'ml', 3 FROM ingredient WHERE name = '甜苦艾酒';

-- 蘋果曼哈頓
INSERT INTO recipe (product_id, preparation_notes) SELECT id, '攪拌法 / 雞尾酒杯' FROM product WHERE name_zh = '蘋果曼哈頓' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 30, 'ml', 1 FROM ingredient WHERE name = '波本威士忌';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 15, 'ml', 2 FROM ingredient WHERE name = '青蘋果香甜酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 15, 'ml', 3 FROM ingredient WHERE name = 'Triple Sec';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 15, 'ml', 4 FROM ingredient WHERE name = '甜苦艾酒';

-- 愛爾蘭汽車炸彈
INSERT INTO recipe (product_id, preparation_notes) SELECT id, '冰鎮啤酒杯 + 烈酒杯，炸彈式飲法' FROM product WHERE name_zh = '愛爾蘭汽車炸彈' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 15, 'ml', 1 FROM ingredient WHERE name = '愛爾蘭威士忌';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 15, 'ml', 2 FROM ingredient WHERE name = '奶酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 250, 'ml', 3 FROM ingredient WHERE name = '健力士黑啤酒';

-- 老夥伴
INSERT INTO recipe (product_id, preparation_notes) SELECT id, '攪拌法 / 古典酒杯' FROM product WHERE name_zh = '老夥伴' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 30, 'ml', 1 FROM ingredient WHERE name = '裸麥威士忌';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 22, 'ml', 2 FROM ingredient WHERE name = '金巴利苦酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 22, 'ml', 3 FROM ingredient WHERE name = '不甜苦艾酒';

-- 熱托地
INSERT INTO recipe (product_id, preparation_notes) SELECT id, '直接注入法 / 托地杯，檸檬片 + 肉桂粉裝飾' FROM product WHERE name_zh = '熱托地' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 45, 'ml', 1 FROM ingredient WHERE name = '白蘭地';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 15, 'ml', 2 FROM ingredient WHERE name = '新鮮檸檬汁';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 15, 'ml', 3 FROM ingredient WHERE name = '果糖';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 0, '適量', 4 FROM ingredient WHERE name = '熱開水';

-- 馬頸
INSERT INTO recipe (product_id, preparation_notes) SELECT id, '直接注入法 / 高飛球杯，螺旋狀檸檬皮裝飾' FROM product WHERE name_zh = '馬頸' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 45, 'ml', 1 FROM ingredient WHERE name = '白蘭地';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 0, '適量', 2 FROM ingredient WHERE name = '薑汁汽水';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 1, 'dash', 3 FROM ingredient WHERE name = '安格式苦精';

-- 白蘭地巴克
INSERT INTO recipe (product_id, preparation_notes) SELECT id, '直接注入法 / 古典杯' FROM product WHERE name_zh = '白蘭地巴克' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 30, 'ml', 1 FROM ingredient WHERE name = '白蘭地';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 0, '適量', 2 FROM ingredient WHERE name = '薑汁汽水';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 0, '少許', 3 FROM ingredient WHERE name = '新鮮檸檬汁';

-- 白蘭地蘇打
INSERT INTO recipe (product_id, preparation_notes) SELECT id, '直接注入法 / 高飛球杯，檸檬片裝飾' FROM product WHERE name_zh = '白蘭地蘇打' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 30, 'ml', 1 FROM ingredient WHERE name = '白蘭地';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 0, '適量', 2 FROM ingredient WHERE name = '蘇打水';

-- 白蘭地亞歷山大
INSERT INTO recipe (product_id, preparation_notes) SELECT id, '搖盪法 / 雞尾酒杯，荳蔻粉裝飾' FROM product WHERE name_zh = '白蘭地亞歷山大' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 20, 'ml', 1 FROM ingredient WHERE name = '白蘭地';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 20, 'ml', 2 FROM ingredient WHERE name = '深可可香甜酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 20, 'ml', 3 FROM ingredient WHERE name = '奶精';

-- 東印度大樓
INSERT INTO recipe (product_id, preparation_notes) SELECT id, '搖盪法 / 雞尾酒杯' FROM product WHERE name_zh = '東印度大樓' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 52, 'ml', 1 FROM ingredient WHERE name = '白蘭地';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 7.5, 'ml', 2 FROM ingredient WHERE name = '白色蘭姆酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 7.5, 'ml', 3 FROM ingredient WHERE name = '香澄干邑白蘭地';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 90, 'ml', 4 FROM ingredient WHERE name = '鳳梨汁';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 1, 'dash', 5 FROM ingredient WHERE name = '安格式苦精';

-- 床第之間
INSERT INTO recipe (product_id, preparation_notes) SELECT id, '搖盪法 / 雞尾酒杯' FROM product WHERE name_zh = '床第之間' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 30, 'ml', 1 FROM ingredient WHERE name = '白蘭地';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 30, 'ml', 2 FROM ingredient WHERE name = '白色蘭姆酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 30, 'ml', 3 FROM ingredient WHERE name = '君度橙皮香甜酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 15, 'ml', 4 FROM ingredient WHERE name = '新鮮檸檬汁';

-- 傑克蘿絲
INSERT INTO recipe (product_id, preparation_notes) SELECT id, '搖盪法 / 雞尾酒杯' FROM product WHERE name_zh = '傑克蘿絲' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 45, 'ml', 1 FROM ingredient WHERE name = '蘋果白蘭地';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 15, 'ml', 2 FROM ingredient WHERE name = '紅石榴糖漿';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 15, 'ml', 3 FROM ingredient WHERE name = '新鮮檸檬汁';

-- 尼古拉斯
INSERT INTO recipe (product_id, preparation_notes) SELECT id, '直接注入法 / Shot 杯，檸檬片放杯口，糖撒其上，先飲再吃' FROM product WHERE name_zh = '尼古拉斯' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 30, 'ml', 1 FROM ingredient WHERE name = '干邑白蘭地';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 8, 'g', 2 FROM ingredient WHERE name = '糖';

-- 醉漢
INSERT INTO recipe (product_id, preparation_notes) SELECT id, '搖盪法 / 雞尾酒杯' FROM product WHERE name_zh = '醉漢' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 45, 'ml', 1 FROM ingredient WHERE name = '白蘭地';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 20, 'ml', 2 FROM ingredient WHERE name = '白薄荷香甜酒';

-- 蛋酒
INSERT INTO recipe (product_id, preparation_notes) SELECT id, '搖盪法 / 高飛球杯，荳蔻粉裝飾' FROM product WHERE name_zh = '蛋酒' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 30, 'ml', 1 FROM ingredient WHERE name = '白蘭地';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 15, 'ml', 2 FROM ingredient WHERE name = '白色蘭姆酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 120, 'ml', 3 FROM ingredient WHERE name = '鮮奶';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 15, 'ml', 4 FROM ingredient WHERE name = '果糖';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 1, '顆', 5 FROM ingredient WHERE name = '蛋黃';

-- 莫希多
INSERT INTO recipe (product_id, preparation_notes) SELECT id, '壓榨法 / 高飛球杯，薄荷枝裝飾' FROM product WHERE name_zh = '莫希多' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 45, 'ml', 1 FROM ingredient WHERE name = '白色蘭姆酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 15, 'ml', 2 FROM ingredient WHERE name = '新鮮檸檬汁';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 12, '片', 3 FROM ingredient WHERE name = '新鮮薄荷葉';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 8, 'g', 4 FROM ingredient WHERE name = '糖';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 0, '適量', 5 FROM ingredient WHERE name = '蘇打水';

-- 拓荒者賓治
INSERT INTO recipe (product_id, preparation_notes) SELECT id, '搖盪法 / 可林杯' FROM product WHERE name_zh = '拓荒者賓治' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 45, 'ml', 1 FROM ingredient WHERE name = '深色蘭姆酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 15, 'ml', 2 FROM ingredient WHERE name = '新鮮檸檬汁';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 10, 'ml', 3 FROM ingredient WHERE name = '紅石榴糖漿';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 0, '適量', 4 FROM ingredient WHERE name = '蘇打水';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 1, 'dash', 5 FROM ingredient WHERE name = '安格式苦精';

-- 邁泰
INSERT INTO recipe (product_id, preparation_notes) SELECT id, '壓榨 + 漂浮法 / 古典杯' FROM product WHERE name_zh = '邁泰' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 30, 'ml', 1 FROM ingredient WHERE name = '白色蘭姆酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 30, 'ml', 2 FROM ingredient WHERE name = '深色蘭姆酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 15, 'ml', 3 FROM ingredient WHERE name = '柑橘香甜酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 10, 'ml', 4 FROM ingredient WHERE name = '果糖';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 10, 'ml', 5 FROM ingredient WHERE name = '新鮮檸檬汁';

-- 螺絲起子
INSERT INTO recipe (product_id, preparation_notes) SELECT id, '攪拌法 / 高飛球杯，柳橙片裝飾' FROM product WHERE name_zh = '螺絲起子' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 50, 'ml', 1 FROM ingredient WHERE name = '伏特加';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 100, 'ml', 2 FROM ingredient WHERE name = '新鮮柳橙汁';

-- 灰狗／鹹狗
INSERT INTO recipe (product_id, preparation_notes) SELECT id, '攪拌法 / 高飛球杯，鹹狗版加鹽口杯' FROM product WHERE name_zh = '灰狗／鹹狗' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 50, 'ml', 1 FROM ingredient WHERE name = '伏特加';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 100, 'ml', 2 FROM ingredient WHERE name = '葡萄柚汁';

-- 環遊世界
INSERT INTO recipe (product_id, preparation_notes) SELECT id, '直接注入法 / 高腳水杯' FROM product WHERE name_zh = '環遊世界' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 10, 'ml', 1 FROM ingredient WHERE name = '琴酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 10, 'ml', 2 FROM ingredient WHERE name = '伏特加';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 10, 'ml', 3 FROM ingredient WHERE name = '白色蘭姆酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 10, 'ml', 4 FROM ingredient WHERE name = '龍舌蘭';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 10, 'ml', 5 FROM ingredient WHERE name = '白蘭地';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 10, 'ml', 6 FROM ingredient WHERE name = '波本威士忌';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 10, 'ml', 7 FROM ingredient WHERE name = 'Triple Sec';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 0, '適量', 8 FROM ingredient WHERE name = '新鮮柳橙汁';

-- 長島冰茶
INSERT INTO recipe (product_id, preparation_notes) SELECT id, '直接注入法 / 可林杯' FROM product WHERE name_zh = '長島冰茶' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 15, 'ml', 1 FROM ingredient WHERE name = '琴酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 15, 'ml', 2 FROM ingredient WHERE name = '伏特加';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 15, 'ml', 3 FROM ingredient WHERE name = '白色蘭姆酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 15, 'ml', 4 FROM ingredient WHERE name = '龍舌蘭';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 15, 'ml', 5 FROM ingredient WHERE name = 'Triple Sec';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 15, 'ml', 6 FROM ingredient WHERE name = '新鮮檸檬汁';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 10, 'ml', 7 FROM ingredient WHERE name = '果糖';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order) SELECT @rid, id, 0, '適量', 8 FROM ingredient WHERE name = '可樂';
