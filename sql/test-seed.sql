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
--   docker exec -i pobar-mysql mysql --default-character-set=utf8mb4 \
--     -upobar -ppobar_pass pobar < sql/test-seed.sql
--   ⚠️ 務必加 --default-character-set=utf8mb4，否則中文（如「測試餐點」）
--      會存成亂碼。
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
-- 冪等策略：只在該桌名尚不存在時插入（不刪除，因桌位可能被 table_session_table
-- 外鍵引用，DELETE 會失敗）。桌位為靜態基礎資料，建一次即可。
INSERT INTO `bar_table` (`name`, `type`, `capacity`, `pos_x`, `pos_y`, `is_locked`, `is_active`)
SELECT * FROM (
  SELECT 'A1' n, 'REGULAR' t, 2 c, 0 x, 0 y, 0 l, 1 a UNION ALL
  SELECT 'A2', 'REGULAR',     2, 80,  0, 0, 1 UNION ALL
  SELECT 'A3', 'REGULAR',     4,  0, 80, 0, 1 UNION ALL
  SELECT 'A4', 'REGULAR',     4, 80, 80, 0, 1 UNION ALL
  SELECT 'B1', 'BAR_COUNTER', 1,  0,160, 0, 1 UNION ALL
  SELECT 'B2', 'BAR_COUNTER', 1, 40,160, 0, 1
) seed
WHERE NOT EXISTS (SELECT 1 FROM `bar_table` bt WHERE bt.`name` = seed.n);

-- ── FOOD 測試品項 ──────────────────────────────────────────
-- 菜單原本全為 DRINK（調酒），廚房出餐看板（?type=FOOD）沒有品項可測。
-- 種一個 FOOD 品項供廚房測試（吧台看板用既有 DRINK 品項即可）。
-- 以 name_zh 冪等：先刪同名再插入。
DELETE FROM `product` WHERE `name_zh` = '測試餐點';
INSERT INTO `product` (`category_id`, `name_zh`, `name_en`, `price`, `type`, `is_active`, `is_available`, `created_by`)
VALUES ((SELECT id FROM category LIMIT 1), '測試餐點', 'Test Dish', 200, 'FOOD', 1, 1, 'test-seed');
