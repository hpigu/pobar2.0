-- ============================================================
-- Pobar 食材與酒譜種子資料
-- 依據 29_酒譜(43款)1100602更新.docx
-- 執行順序：schema.sql → seed_cocktails.sql → seed_ingredients.sql
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 重跑前先清除酒譜資料（ingredient 有 UNIQUE KEY 可安全 INSERT IGNORE，不需清）
DELETE FROM recipe_ingredient;
DELETE FROM recipe;

-- ────────────────────────────────────────────────────────────
-- 1. 食材（ingredient）— INSERT IGNORE 避免重複名稱錯誤
-- ────────────────────────────────────────────────────────────
INSERT IGNORE INTO ingredient (name, unit, is_available) VALUES
-- 基酒
('琴酒',          'ml', 1),
('蘇格蘭威士忌',  'ml', 1),
('加拿大威士忌',  'ml', 1),
('波本威士忌',    'ml', 1),
('愛爾蘭調和威士忌', 'ml', 1),
('愛爾蘭威士忌',  'ml', 1),
('蘇格蘭調和威士忌', 'ml', 1),
('裸麥威士忌',    'ml', 1),
('白蘭地',        'ml', 1),
('蘋果白蘭地',    'ml', 1),
('干邑白蘭地',    'ml', 1),
('香澄干邑白蘭地','ml', 1),
('白色蘭姆酒',    'ml', 1),
('深色蘭姆酒',    'ml', 1),
('伏特加',        'ml', 1),
('龍舌蘭',        'ml', 1),
-- 利口酒 / 香甜酒
('藍柑橘香甜酒',  'ml', 1),
('君度橙皮香甜酒','ml', 1),
('杏仁糖漿',      'ml', 1),
('甜苦艾酒',      'ml', 1),
('不甜苦艾酒',    'ml', 1),
('金巴利苦酒',    'ml', 1),
('班尼狄克丁',    'ml', 1),
('紅石榴糖漿',    'ml', 1),
('Triple Sec',    'ml', 1),
('杏仁香甜酒',    'ml', 1),
('蜂蜜香甜酒',    'ml', 1),
('青蘋果香甜酒',  'ml', 1),
('奶酒',          'ml', 1),
('深可可香甜酒',  'ml', 1),
('白薄荷香甜酒',  'ml', 1),
('柑橘香甜酒',    'ml', 1),
('櫻桃白蘭地',    'ml', 1),
-- 果汁 / 汽水 / 其他飲料
('新鮮檸檬汁',    'ml', 1),
('新鮮柳橙汁',    'ml', 1),
('鳳梨汁',        'ml', 1),
('葡萄柚汁',      'ml', 1),
('萊姆汁',        'ml', 1),
('薑汁汽水',      'ml', 1),
('蘇打水',        'ml', 1),
('通寧水',        'ml', 1),
('七喜',          'ml', 1),
('可樂',          'ml', 1),
('健力士黑啤酒',  'ml', 1),
('熱開水',        'ml', 1),
-- 調味料
('果糖',          'ml', 1),
('安格式苦精',    'dash', 1),
('糖',            'g',  1),
-- 其他食材
('新鮮薄荷葉',    '片', 1),
('鮮奶',          'ml', 1),
('蛋黃',          '顆', 1),
('奶精',          'ml', 1),
('伯爵茶包',      '包', 1);

-- ────────────────────────────────────────────────────────────
-- 2. 酒譜與食材聯動
-- 每款調酒：先 INSERT recipe，SET @rid = LAST_INSERT_ID()，
-- 再用 @rid 插入所有 recipe_ingredient（避免 LAST_INSERT_ID 被覆蓋）
-- ────────────────────────────────────────────────────────────

-- ── 琴費士 ─────────────────────────────────────────────────
INSERT INTO recipe (product_id, preparation_notes)
SELECT id, '搖盪法 / 高飛球杯' FROM product WHERE name_zh = '琴費士' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 45, 'ml', 1 FROM ingredient WHERE name = '琴酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 30, 'ml', 2 FROM ingredient WHERE name = '新鮮檸檬汁';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 15, 'ml', 3 FROM ingredient WHERE name = '果糖';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 0, '適量', 4 FROM ingredient WHERE name = '蘇打水';

-- ── 藍鳥 ───────────────────────────────────────────────────
INSERT INTO recipe (product_id, preparation_notes)
SELECT id, '搖盪法 / 雞尾酒杯' FROM product WHERE name_zh = '藍鳥' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 30, 'ml', 1 FROM ingredient WHERE name = '琴酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 15, 'ml', 2 FROM ingredient WHERE name = '藍柑橘香甜酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 15, 'ml', 3 FROM ingredient WHERE name = '新鮮檸檬汁';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 10, 'ml', 4 FROM ingredient WHERE name = '杏仁糖漿';

-- ── 白色佳人 ───────────────────────────────────────────────
INSERT INTO recipe (product_id, preparation_notes)
SELECT id, '搖盪法 / 雞尾酒杯' FROM product WHERE name_zh = '白色佳人' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 40, 'ml', 1 FROM ingredient WHERE name = '琴酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 20, 'ml', 2 FROM ingredient WHERE name = '君度橙皮香甜酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 15, 'ml', 3 FROM ingredient WHERE name = '新鮮檸檬汁';

-- ── 橘花 ───────────────────────────────────────────────────
INSERT INTO recipe (product_id, preparation_notes)
SELECT id, '搖盪法 / 雞尾酒杯' FROM product WHERE name_zh = '橘花' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 30, 'ml', 1 FROM ingredient WHERE name = '琴酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 15, 'ml', 2 FROM ingredient WHERE name = '甜苦艾酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 30, 'ml', 3 FROM ingredient WHERE name = '新鮮柳橙汁';

-- ── 新加坡司令 ─────────────────────────────────────────────
INSERT INTO recipe (product_id, preparation_notes)
SELECT id, '搖盪法 / 可林杯' FROM product WHERE name_zh = '新加坡司令' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 30, 'ml', 1 FROM ingredient WHERE name = '琴酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 15, 'ml', 2 FROM ingredient WHERE name = '櫻桃白蘭地';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 10, 'ml', 3 FROM ingredient WHERE name = '君度橙皮香甜酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 10, 'ml', 4 FROM ingredient WHERE name = '班尼狄克丁';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 10, 'ml', 5 FROM ingredient WHERE name = '紅石榴糖漿';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 90, 'ml', 6 FROM ingredient WHERE name = '鳳梨汁';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 30, 'ml', 7 FROM ingredient WHERE name = '新鮮檸檬汁';

-- ── 琴蕾 ───────────────────────────────────────────────────
INSERT INTO recipe (product_id, preparation_notes)
SELECT id, '攪拌法 / 雞尾酒杯' FROM product WHERE name_zh = '琴蕾' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 45, 'ml', 1 FROM ingredient WHERE name = '琴酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 30, 'ml', 2 FROM ingredient WHERE name = '萊姆汁';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 15, 'ml', 3 FROM ingredient WHERE name = '果糖';

-- ── 不甜馬丁尼 ─────────────────────────────────────────────
INSERT INTO recipe (product_id, preparation_notes)
SELECT id, '攪拌法 / 馬丁尼杯，紅心橄欖裝飾' FROM product WHERE name_zh = '不甜馬丁尼' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 45, 'ml', 1 FROM ingredient WHERE name = '琴酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 15, 'ml', 2 FROM ingredient WHERE name = '不甜苦艾酒';

-- ── 尼格尼羅 ───────────────────────────────────────────────
INSERT INTO recipe (product_id, preparation_notes)
SELECT id, '攪拌法 / 古典酒杯，柳橙皮裝飾' FROM product WHERE name_zh = '尼格尼羅' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 30, 'ml', 1 FROM ingredient WHERE name = '琴酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 30, 'ml', 2 FROM ingredient WHERE name = '甜苦艾酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 30, 'ml', 3 FROM ingredient WHERE name = '金巴利苦酒';

-- ── 茶通寧 ─────────────────────────────────────────────────
INSERT INTO recipe (product_id, preparation_notes)
SELECT id, '直接注入法 / 雞尾酒杯' FROM product WHERE name_zh = '茶通寧' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 30, 'ml', 1 FROM ingredient WHERE name = '琴酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 150, 'ml', 2 FROM ingredient WHERE name = '通寧水';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 1, '包', 3 FROM ingredient WHERE name = '伯爵茶包';

-- ── 蘇格蘭蘇打 ─────────────────────────────────────────────
INSERT INTO recipe (product_id, preparation_notes)
SELECT id, '直接注入法 / 高飛球杯，檸檬片裝飾' FROM product WHERE name_zh = '蘇格蘭蘇打' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 30, 'ml', 1 FROM ingredient WHERE name = '蘇格蘭威士忌';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 0, '適量', 2 FROM ingredient WHERE name = '蘇打水';

-- ── 加拿大七喜 ─────────────────────────────────────────────
INSERT INTO recipe (product_id, preparation_notes)
SELECT id, '直接注入法 / 高飛球杯，檸檬角裝飾' FROM product WHERE name_zh = '加拿大七喜' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 30, 'ml', 1 FROM ingredient WHERE name = '加拿大威士忌';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 0, '適量', 2 FROM ingredient WHERE name = '七喜';

-- ── 約翰可林 ───────────────────────────────────────────────
INSERT INTO recipe (product_id, preparation_notes)
SELECT id, '直接注入法 / 可林杯' FROM product WHERE name_zh = '約翰可林' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 45, 'ml', 1 FROM ingredient WHERE name = '波本威士忌';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 30, 'ml', 2 FROM ingredient WHERE name = '新鮮檸檬汁';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 15, 'ml', 3 FROM ingredient WHERE name = '果糖';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 0, '適量', 4 FROM ingredient WHERE name = '蘇打水';

-- ── 領航者可林 ─────────────────────────────────────────────
INSERT INTO recipe (product_id, preparation_notes)
SELECT id, '搖盪法 / 可林杯' FROM product WHERE name_zh = '領航者可林' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 30, 'ml', 1 FROM ingredient WHERE name = '加拿大威士忌';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 30, 'ml', 2 FROM ingredient WHERE name = '新鮮檸檬汁';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 10, 'ml', 3 FROM ingredient WHERE name = '果糖';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 0, '適量', 4 FROM ingredient WHERE name = '蘇打水';

-- ── 曼哈頓 ─────────────────────────────────────────────────
INSERT INTO recipe (product_id, preparation_notes)
SELECT id, '攪拌法 / 馬丁尼杯，櫻桃裝飾' FROM product WHERE name_zh = '曼哈頓' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 45, 'ml', 1 FROM ingredient WHERE name = '波本威士忌';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 15, 'ml', 2 FROM ingredient WHERE name = '甜苦艾酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 2, 'dash', 3 FROM ingredient WHERE name = '安格式苦精';

-- ── 不甜曼哈頓 ─────────────────────────────────────────────
INSERT INTO recipe (product_id, preparation_notes)
SELECT id, '攪拌法 / 馬丁尼杯，檸檬皮裝飾' FROM product WHERE name_zh = '不甜曼哈頓' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 45, 'ml', 1 FROM ingredient WHERE name = '波本威士忌';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 15, 'ml', 2 FROM ingredient WHERE name = '不甜苦艾酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 2, 'dash', 3 FROM ingredient WHERE name = '安格式苦精';

-- ── 威士忌酸酒 ─────────────────────────────────────────────
INSERT INTO recipe (product_id, preparation_notes)
SELECT id, '搖盪法 / 酸酒杯' FROM product WHERE name_zh = '威士忌酸酒' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 45, 'ml', 1 FROM ingredient WHERE name = '波本威士忌';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 30, 'ml', 2 FROM ingredient WHERE name = '新鮮檸檬汁';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 30, 'ml', 3 FROM ingredient WHERE name = '果糖';

-- ── 沉默的第三者 ───────────────────────────────────────────
INSERT INTO recipe (product_id, preparation_notes)
SELECT id, '搖盪法 / 雞尾酒杯' FROM product WHERE name_zh = '沉默的第三者' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 15, 'ml', 1 FROM ingredient WHERE name = '愛爾蘭調和威士忌';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 20, 'ml', 2 FROM ingredient WHERE name = 'Triple Sec';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 20, 'ml', 3 FROM ingredient WHERE name = '新鮮檸檬汁';

-- ── 教父 ───────────────────────────────────────────────────
INSERT INTO recipe (product_id, preparation_notes)
SELECT id, '直接注入法 / 古典酒杯' FROM product WHERE name_zh = '教父' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 45, 'ml', 1 FROM ingredient WHERE name = '蘇格蘭調和威士忌';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 15, 'ml', 2 FROM ingredient WHERE name = '杏仁香甜酒';

-- ── 鏽釘子 ─────────────────────────────────────────────────
INSERT INTO recipe (product_id, preparation_notes)
SELECT id, '攪拌法 / 雞尾酒杯' FROM product WHERE name_zh = '鏽釘子' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 45, 'ml', 1 FROM ingredient WHERE name = '蘇格蘭調和威士忌';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 30, 'ml', 2 FROM ingredient WHERE name = '蜂蜜香甜酒';

-- ── 古典酒 ─────────────────────────────────────────────────
INSERT INTO recipe (product_id, preparation_notes)
SELECT id, '直接注入法 / 古典酒杯' FROM product WHERE name_zh = '古典酒' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 45, 'ml', 1 FROM ingredient WHERE name = '波本威士忌';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 2, 'dash', 2 FROM ingredient WHERE name = '安格式苦精';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 8, 'g', 3 FROM ingredient WHERE name = '糖';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 0, '少許', 4 FROM ingredient WHERE name = '蘇打水';

-- ── 花花公子 ───────────────────────────────────────────────
INSERT INTO recipe (product_id, preparation_notes)
SELECT id, '攪拌法 / 雞尾酒杯，柳橙皮裝飾' FROM product WHERE name_zh = '花花公子' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 30, 'ml', 1 FROM ingredient WHERE name = '波本威士忌';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 30, 'ml', 2 FROM ingredient WHERE name = '金巴利苦酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 30, 'ml', 3 FROM ingredient WHERE name = '甜苦艾酒';

-- ── 蘋果曼哈頓 ─────────────────────────────────────────────
INSERT INTO recipe (product_id, preparation_notes)
SELECT id, '攪拌法 / 雞尾酒杯' FROM product WHERE name_zh = '蘋果曼哈頓' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 30, 'ml', 1 FROM ingredient WHERE name = '波本威士忌';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 15, 'ml', 2 FROM ingredient WHERE name = '青蘋果香甜酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 15, 'ml', 3 FROM ingredient WHERE name = 'Triple Sec';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 15, 'ml', 4 FROM ingredient WHERE name = '甜苦艾酒';

-- ── 愛爾蘭汽車炸彈 ────────────────────────────────────────
INSERT INTO recipe (product_id, preparation_notes)
SELECT id, '冰鎮啤酒杯 + 烈酒杯，炸彈式飲法' FROM product WHERE name_zh = '愛爾蘭汽車炸彈' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 15, 'ml', 1 FROM ingredient WHERE name = '愛爾蘭威士忌';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 15, 'ml', 2 FROM ingredient WHERE name = '奶酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 250, 'ml', 3 FROM ingredient WHERE name = '健力士黑啤酒';

-- ── 老夥伴 ─────────────────────────────────────────────────
INSERT INTO recipe (product_id, preparation_notes)
SELECT id, '攪拌法 / 古典酒杯' FROM product WHERE name_zh = '老夥伴' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 30, 'ml', 1 FROM ingredient WHERE name = '裸麥威士忌';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 22, 'ml', 2 FROM ingredient WHERE name = '金巴利苦酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 22, 'ml', 3 FROM ingredient WHERE name = '不甜苦艾酒';

-- ── 熱托地 ─────────────────────────────────────────────────
INSERT INTO recipe (product_id, preparation_notes)
SELECT id, '直接注入法 / 托地杯，檸檬片 + 肉桂粉裝飾' FROM product WHERE name_zh = '熱托地' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 45, 'ml', 1 FROM ingredient WHERE name = '白蘭地';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 15, 'ml', 2 FROM ingredient WHERE name = '新鮮檸檬汁';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 15, 'ml', 3 FROM ingredient WHERE name = '果糖';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 0, '適量', 4 FROM ingredient WHERE name = '熱開水';

-- ── 馬頸 ───────────────────────────────────────────────────
INSERT INTO recipe (product_id, preparation_notes)
SELECT id, '直接注入法 / 高飛球杯，螺旋狀檸檬皮裝飾' FROM product WHERE name_zh = '馬頸' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 45, 'ml', 1 FROM ingredient WHERE name = '白蘭地';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 0, '適量', 2 FROM ingredient WHERE name = '薑汁汽水';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 1, 'dash', 3 FROM ingredient WHERE name = '安格式苦精';

-- ── 白蘭地巴克 ─────────────────────────────────────────────
INSERT INTO recipe (product_id, preparation_notes)
SELECT id, '直接注入法 / 古典杯' FROM product WHERE name_zh = '白蘭地巴克' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 30, 'ml', 1 FROM ingredient WHERE name = '白蘭地';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 0, '適量', 2 FROM ingredient WHERE name = '薑汁汽水';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 0, '少許', 3 FROM ingredient WHERE name = '新鮮檸檬汁';

-- ── 白蘭地蘇打 ─────────────────────────────────────────────
INSERT INTO recipe (product_id, preparation_notes)
SELECT id, '直接注入法 / 高飛球杯，檸檬片裝飾' FROM product WHERE name_zh = '白蘭地蘇打' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 30, 'ml', 1 FROM ingredient WHERE name = '白蘭地';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 0, '適量', 2 FROM ingredient WHERE name = '蘇打水';

-- ── 白蘭地亞歷山大 ────────────────────────────────────────
INSERT INTO recipe (product_id, preparation_notes)
SELECT id, '搖盪法 / 雞尾酒杯，荳蔻粉裝飾' FROM product WHERE name_zh = '白蘭地亞歷山大' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 20, 'ml', 1 FROM ingredient WHERE name = '白蘭地';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 20, 'ml', 2 FROM ingredient WHERE name = '深可可香甜酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 20, 'ml', 3 FROM ingredient WHERE name = '奶精';

-- ── 東印度大樓 ─────────────────────────────────────────────
INSERT INTO recipe (product_id, preparation_notes)
SELECT id, '搖盪法 / 雞尾酒杯' FROM product WHERE name_zh = '東印度大樓' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 52, 'ml', 1 FROM ingredient WHERE name = '白蘭地';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 7.5, 'ml', 2 FROM ingredient WHERE name = '白色蘭姆酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 7.5, 'ml', 3 FROM ingredient WHERE name = '香澄干邑白蘭地';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 90, 'ml', 4 FROM ingredient WHERE name = '鳳梨汁';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 1, 'dash', 5 FROM ingredient WHERE name = '安格式苦精';

-- ── 床第之間 ───────────────────────────────────────────────
INSERT INTO recipe (product_id, preparation_notes)
SELECT id, '搖盪法 / 雞尾酒杯' FROM product WHERE name_zh = '床第之間' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 30, 'ml', 1 FROM ingredient WHERE name = '白蘭地';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 30, 'ml', 2 FROM ingredient WHERE name = '白色蘭姆酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 30, 'ml', 3 FROM ingredient WHERE name = '君度橙皮香甜酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 15, 'ml', 4 FROM ingredient WHERE name = '新鮮檸檬汁';

-- ── 傑克蘿絲 ───────────────────────────────────────────────
INSERT INTO recipe (product_id, preparation_notes)
SELECT id, '搖盪法 / 雞尾酒杯' FROM product WHERE name_zh = '傑克蘿絲' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 45, 'ml', 1 FROM ingredient WHERE name = '蘋果白蘭地';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 15, 'ml', 2 FROM ingredient WHERE name = '紅石榴糖漿';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 15, 'ml', 3 FROM ingredient WHERE name = '新鮮檸檬汁';

-- ── 尼古拉斯 ───────────────────────────────────────────────
INSERT INTO recipe (product_id, preparation_notes)
SELECT id, '直接注入法 / Shot 杯，檸檬片放杯口，糖撒其上，先飲再吃' FROM product WHERE name_zh = '尼古拉斯' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 30, 'ml', 1 FROM ingredient WHERE name = '干邑白蘭地';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 8, 'g', 2 FROM ingredient WHERE name = '糖';

-- ── 醉漢 ───────────────────────────────────────────────────
INSERT INTO recipe (product_id, preparation_notes)
SELECT id, '搖盪法 / 雞尾酒杯' FROM product WHERE name_zh = '醉漢' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 45, 'ml', 1 FROM ingredient WHERE name = '白蘭地';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 20, 'ml', 2 FROM ingredient WHERE name = '白薄荷香甜酒';

-- ── 蛋酒 ───────────────────────────────────────────────────
INSERT INTO recipe (product_id, preparation_notes)
SELECT id, '搖盪法 / 高飛球杯，荳蔻粉裝飾' FROM product WHERE name_zh = '蛋酒' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 30, 'ml', 1 FROM ingredient WHERE name = '白蘭地';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 15, 'ml', 2 FROM ingredient WHERE name = '白色蘭姆酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 120, 'ml', 3 FROM ingredient WHERE name = '鮮奶';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 15, 'ml', 4 FROM ingredient WHERE name = '果糖';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 1, '顆', 5 FROM ingredient WHERE name = '蛋黃';

-- ── 莫希多 ─────────────────────────────────────────────────
INSERT INTO recipe (product_id, preparation_notes)
SELECT id, '壓榨法 / 高飛球杯，薄荷枝裝飾' FROM product WHERE name_zh = '莫希多' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 45, 'ml', 1 FROM ingredient WHERE name = '白色蘭姆酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 15, 'ml', 2 FROM ingredient WHERE name = '新鮮檸檬汁';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 12, '片', 3 FROM ingredient WHERE name = '新鮮薄荷葉';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 8, 'g', 4 FROM ingredient WHERE name = '糖';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 0, '適量', 5 FROM ingredient WHERE name = '蘇打水';

-- ── 拓荒者賓治 ─────────────────────────────────────────────
INSERT INTO recipe (product_id, preparation_notes)
SELECT id, '搖盪法 / 可林杯' FROM product WHERE name_zh = '拓荒者賓治' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 45, 'ml', 1 FROM ingredient WHERE name = '深色蘭姆酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 15, 'ml', 2 FROM ingredient WHERE name = '新鮮檸檬汁';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 10, 'ml', 3 FROM ingredient WHERE name = '紅石榴糖漿';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 0, '適量', 4 FROM ingredient WHERE name = '蘇打水';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 1, 'dash', 5 FROM ingredient WHERE name = '安格式苦精';

-- ── 邁泰 ───────────────────────────────────────────────────
INSERT INTO recipe (product_id, preparation_notes)
SELECT id, '壓榨 + 漂浮法 / 古典杯' FROM product WHERE name_zh = '邁泰' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 30, 'ml', 1 FROM ingredient WHERE name = '白色蘭姆酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 30, 'ml', 2 FROM ingredient WHERE name = '深色蘭姆酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 15, 'ml', 3 FROM ingredient WHERE name = '柑橘香甜酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 10, 'ml', 4 FROM ingredient WHERE name = '果糖';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 10, 'ml', 5 FROM ingredient WHERE name = '新鮮檸檬汁';

-- ── 螺絲起子 ───────────────────────────────────────────────
INSERT INTO recipe (product_id, preparation_notes)
SELECT id, '攪拌法 / 高飛球杯，柳橙片裝飾' FROM product WHERE name_zh = '螺絲起子' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 50, 'ml', 1 FROM ingredient WHERE name = '伏特加';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 100, 'ml', 2 FROM ingredient WHERE name = '新鮮柳橙汁';

-- ── 灰狗／鹹狗 ─────────────────────────────────────────────
INSERT INTO recipe (product_id, preparation_notes)
SELECT id, '攪拌法 / 高飛球杯，鹹狗版加鹽口杯' FROM product WHERE name_zh = '灰狗／鹹狗' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 50, 'ml', 1 FROM ingredient WHERE name = '伏特加';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 100, 'ml', 2 FROM ingredient WHERE name = '葡萄柚汁';

-- ── 環遊世界 ───────────────────────────────────────────────
INSERT INTO recipe (product_id, preparation_notes)
SELECT id, '直接注入法 / 高腳水杯' FROM product WHERE name_zh = '環遊世界' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 10, 'ml', 1 FROM ingredient WHERE name = '琴酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 10, 'ml', 2 FROM ingredient WHERE name = '伏特加';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 10, 'ml', 3 FROM ingredient WHERE name = '白色蘭姆酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 10, 'ml', 4 FROM ingredient WHERE name = '龍舌蘭';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 10, 'ml', 5 FROM ingredient WHERE name = '白蘭地';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 10, 'ml', 6 FROM ingredient WHERE name = '波本威士忌';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 10, 'ml', 7 FROM ingredient WHERE name = 'Triple Sec';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 0, '適量', 8 FROM ingredient WHERE name = '新鮮柳橙汁';

-- ── 長島冰茶 ───────────────────────────────────────────────
INSERT INTO recipe (product_id, preparation_notes)
SELECT id, '直接注入法 / 可林杯' FROM product WHERE name_zh = '長島冰茶' LIMIT 1;
SET @rid = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 15, 'ml', 1 FROM ingredient WHERE name = '琴酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 15, 'ml', 2 FROM ingredient WHERE name = '伏特加';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 15, 'ml', 3 FROM ingredient WHERE name = '白色蘭姆酒';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 15, 'ml', 4 FROM ingredient WHERE name = '龍舌蘭';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 15, 'ml', 5 FROM ingredient WHERE name = 'Triple Sec';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 15, 'ml', 6 FROM ingredient WHERE name = '新鮮檸檬汁';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 10, 'ml', 7 FROM ingredient WHERE name = '果糖';
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, quantity, unit, display_order)
SELECT @rid, id, 0, '適量', 8 FROM ingredient WHERE name = '可樂';

SET FOREIGN_KEY_CHECKS = 1;
