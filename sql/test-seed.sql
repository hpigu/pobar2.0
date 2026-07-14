-- ─────────────────────────────────────────────────────────────
-- 測試專用種子帳號（僅供本機 / E2E 自動化測試使用，切勿用於正式環境）
--
-- 五種角色各一個固定帳號，密碼統一為：Test1234!
-- must_change_password = 0：跳過首次強制改密，方便自動化直接登入
-- is_active = 1：啟用狀態
--
-- 密碼為 BCrypt hash（strength 10），與後端 BCryptPasswordEncoder 相容。
-- 重新產生 hash：於後端容器內執行
--   new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("Test1234!")
--
-- 匯入方式：
--   docker exec -i pobar-mysql mysql -upobar -ppobar_pass pobar < sql/test-seed.sql
-- ─────────────────────────────────────────────────────────────

-- ── 員工帳號 ─────────────────────────────────────────────
INSERT INTO `user` (`account`, `password`, `role`, `is_active`, `must_change_password`)
VALUES
  ('test_admin',     '$2y$10$K9BI7t3FZ8xd3xeBs7S1o.wcg9hA.c0bmk3gYNxnPxhvYb9uZ6RGW', 'ADMIN',     1, 0),
  ('test_manager',   '$2y$10$wbtgo.JkxLxVRYRqroVDPeTEM91VvbZET595VB7E4or64yyFxw2iu', 'MANAGER',   1, 0),
  ('test_waiter',    '$2y$10$OeVbl4kCdqWg/4OBhU.y6udCCbLpHsoRElwauJKmSFSr/ZHajGh5O', 'WAITER',    1, 0),
  ('test_bartender', '$2y$10$Z7TfYbvXT8ge0CFIjBJG4edjWLJruBsTYHi4vpXmLiebOOv32sDHS', 'BARTENDER', 1, 0),
  ('test_kitchen',   '$2y$10$nsx.JyhQQcSbT73xkCiNW.Wk8Vb7rNLyy0Kj4DVX0dSqTwkPP/y..', 'KITCHEN',   1, 0)
AS new
ON DUPLICATE KEY UPDATE
  `password`             = new.`password`,
  `role`                 = new.`role`,
  `is_active`            = new.`is_active`,
  `must_change_password` = new.`must_change_password`;

-- ── 桌位（一般桌 ×4 + 吧台 ×2）─────────────────────────────
-- 顧客點餐流程需先由員工開桌產生 QR token，故測試前需有可用桌位。
-- bar_table.name 無 UNIQUE 約束，故先刪同名測試桌位再插入以確保可重複執行。
-- 注意：只刪 A1~A4/B1~B2 這組測試桌名，不影響其他既有桌位。
DELETE FROM `bar_table` WHERE `name` IN ('A1','A2','A3','A4','B1','B2');
INSERT INTO `bar_table` (`name`, `type`, `capacity`, `pos_x`, `pos_y`, `is_locked`, `is_active`)
VALUES
  ('A1', 'REGULAR',     2,  0,  0, 0, 1),
  ('A2', 'REGULAR',     2, 80,  0, 0, 1),
  ('A3', 'REGULAR',     4,  0, 80, 0, 1),
  ('A4', 'REGULAR',     4, 80, 80, 0, 1),
  ('B1', 'BAR_COUNTER', 1,  0,160, 0, 1),
  ('B2', 'BAR_COUNTER', 1, 40,160, 0, 1);
