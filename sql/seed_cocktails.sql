-- ============================================================
-- Pobar 預設酒單資料
-- 來源：29_酒譜(43款)1100602更新.docx（共 42 款，Sangria 略）
-- 執行前請確認資料庫已執行 schema.sql
-- ============================================================

-- ────────────────────────────────────────────────────────────
-- 1. 分類（category）
-- ────────────────────────────────────────────────────────────
INSERT INTO category (name_zh, name_en, type, display_order, is_active) VALUES
  ('琴酒調酒',   'Gin Cocktails',     'DRINK', 1, 1),
  ('威士忌調酒', 'Whisky Cocktails',  'DRINK', 2, 1),
  ('白蘭地調酒', 'Brandy Cocktails',  'DRINK', 3, 1),
  ('蘭姆酒調酒', 'Rum Cocktails',     'DRINK', 4, 1),
  ('伏特加調酒', 'Vodka Cocktails',   'DRINK', 5, 1),
  ('綜合調酒',   'Mixed Cocktails',   'DRINK', 6, 1);

-- ────────────────────────────────────────────────────────────
-- 2. 品項（product）— 使用子查詢取 category_id，避免硬編 ID
-- ────────────────────────────────────────────────────────────

-- ── 琴酒調酒（9 款）──────────────────────────────────────────
INSERT INTO product (category_id, name_zh, name_en, description_zh, price, type, is_active, is_available, created_by) VALUES
(
  (SELECT id FROM category WHERE name_zh = '琴酒調酒' LIMIT 1),
  '琴費士', 'Gin Fizz',
  '琴酒 45ml・新鮮檸檬汁 30ml・果糖 15ml・蘇打水　搖盪法 / 高飛球杯',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '琴酒調酒' LIMIT 1),
  '藍鳥', 'Blue Bird',
  '琴酒 30ml・藍柑橘香甜酒 15ml・新鮮檸檬汁 15ml・杏仁糖漿 10ml　搖盪法 / 雞尾酒杯',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '琴酒調酒' LIMIT 1),
  '白色佳人', 'White Lady',
  '琴酒 40ml・君度橙皮香甜酒 20ml・新鮮檸檬汁 15ml　搖盪法 / 雞尾酒杯',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '琴酒調酒' LIMIT 1),
  '橘花', 'Orange Blossom',
  '琴酒 30ml・甜苦艾酒 15ml・新鮮柳橙汁 30ml　搖盪法 / 雞尾酒杯',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '琴酒調酒' LIMIT 1),
  '新加坡司令', 'Singapore Sling',
  '琴酒 30ml・櫻桃白蘭地 15ml・君度橙皮 10ml・班尼狄克丁 10ml・紅石榴糖漿 10ml・鳳梨汁 90ml・檸檬汁 30ml　搖盪法 / 可林杯',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '琴酒調酒' LIMIT 1),
  '琴蕾', 'Gimlet',
  '琴酒 45ml・萊姆汁 30ml・果糖 15ml　攪拌法 / 雞尾酒杯',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '琴酒調酒' LIMIT 1),
  '不甜馬丁尼', 'Dry Martini',
  '琴酒 45ml・不甜苦艾酒 15ml　攪拌法 / 馬丁尼杯　紅心橄欖裝飾',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '琴酒調酒' LIMIT 1),
  '尼格尼羅', 'Negroni',
  '琴酒 30ml・甜苦艾酒 30ml・金巴利酒 30ml　攪拌法 / 古典酒杯　柳橙皮裝飾',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '琴酒調酒' LIMIT 1),
  '茶通寧', 'Tea Tonic',
  '琴酒 30ml・通寧水 150ml・伯爵茶包　直接注入法 / 雞尾酒杯',
  350, 'DRINK', 1, 1, 'admin'
);

-- ── 威士忌調酒（15 款）──────────────────────────────────────
INSERT INTO product (category_id, name_zh, name_en, description_zh, price, type, is_active, is_available, created_by) VALUES
(
  (SELECT id FROM category WHERE name_zh = '威士忌調酒' LIMIT 1),
  '蘇格蘭蘇打', 'Scotch Soda',
  '蘇格蘭威士忌 30ml・蘇打水　直接注入法 / 高飛球杯　檸檬片裝飾',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '威士忌調酒' LIMIT 1),
  '加拿大七喜', 'Canadian 7-UP',
  '加拿大威士忌 30ml・七喜　直接注入法 / 高飛球杯　檸檬角裝飾',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '威士忌調酒' LIMIT 1),
  '約翰可林', 'John Collins',
  '波本威士忌 45ml・新鮮檸檬汁 30ml・果糖 15ml・蘇打水　直接注入法 / 可林杯',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '威士忌調酒' LIMIT 1),
  '領航者可林', 'Captain Collins',
  '加拿大威士忌 30ml・新鮮檸檬汁 30ml・果糖 10ml・蘇打水　搖盪法 / 可林杯',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '威士忌調酒' LIMIT 1),
  '曼哈頓', 'Manhattan',
  '波本威士忌 45ml・甜苦艾酒 15ml・安格式苦精　攪拌法 / 馬丁尼杯　櫻桃裝飾',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '威士忌調酒' LIMIT 1),
  '不甜曼哈頓', 'Dry Manhattan',
  '波本威士忌 45ml・不甜苦艾酒 15ml・安格式苦精　攪拌法 / 馬丁尼杯　檸檬皮裝飾',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '威士忌調酒' LIMIT 1),
  '威士忌酸酒', 'Whisky Sour',
  '波本威士忌 45ml・新鮮檸檬汁 30ml・果糖 30ml　搖盪法 / 酸酒杯',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '威士忌調酒' LIMIT 1),
  '沉默的第三者', 'Silent Third',
  '愛爾蘭調和威士忌 15ml・Triple Sec 20ml・新鮮檸檬汁 20ml　搖盪法 / 雞尾酒杯',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '威士忌調酒' LIMIT 1),
  '教父', 'God Father',
  '蘇格蘭調和威士忌 45ml・杏仁香甜酒 15ml　直接注入法 / 古典酒杯',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '威士忌調酒' LIMIT 1),
  '鏽釘子', 'Rusty Nail',
  '蘇格蘭調和威士忌 45ml・蜂蜜香甜酒(Drambuie) 30ml　攪拌法 / 雞尾酒杯',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '威士忌調酒' LIMIT 1),
  '古典酒', 'Old Fashioned',
  '波本威士忌 45ml・安格式苦精・糖包 8g・蘇打水少許　直接注入法 / 古典酒杯',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '威士忌調酒' LIMIT 1),
  '花花公子', 'Boulevardier',
  '波本威士忌 30ml・金巴利苦酒 30ml・甜苦艾酒 30ml　攪拌法 / 雞尾酒杯　柳橙皮裝飾',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '威士忌調酒' LIMIT 1),
  '蘋果曼哈頓', 'Apple Manhattan',
  '波本威士忌 30ml・青蘋果香甜酒 15ml・Triple Sec 15ml・甜苦艾酒 15ml　攪拌法 / 雞尾酒杯',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '威士忌調酒' LIMIT 1),
  '愛爾蘭汽車炸彈', 'Irish Car Bomb',
  '愛爾蘭威士忌 15ml・奶酒 15ml・健力士黑啤酒半瓶　冰鎮啤酒杯 + 烈酒杯',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '威士忌調酒' LIMIT 1),
  '老夥伴', 'Old Pal',
  '裸麥威士忌 30ml・金巴利苦酒 22ml・不甜香艾酒 22ml　攪拌法 / 古典酒杯',
  350, 'DRINK', 1, 1, 'admin'
);

-- ── 白蘭地調酒（11 款）──────────────────────────────────────
INSERT INTO product (category_id, name_zh, name_en, description_zh, price, type, is_active, is_available, created_by) VALUES
(
  (SELECT id FROM category WHERE name_zh = '白蘭地調酒' LIMIT 1),
  '熱托地', 'Hot Toddy',
  '白蘭地 45ml・新鮮檸檬汁 15ml・果糖 15ml・熱開水　直接注入法 / 托地杯　檸檬片 + 肉桂粉裝飾',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '白蘭地調酒' LIMIT 1),
  '馬頸', 'Horse''s Neck',
  '白蘭地 45ml・薑汁汽水・安格式苦精少許　直接注入法 / 高飛球杯　螺旋狀檸檬皮裝飾',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '白蘭地調酒' LIMIT 1),
  '白蘭地巴克', 'Brandy Back',
  '白蘭地 30ml・薑汁汽水・新鮮檸檬汁少許　直接注入法 / 古典杯',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '白蘭地調酒' LIMIT 1),
  '白蘭地蘇打', 'Brandy Soda',
  '白蘭地 30ml・蘇打水　直接注入法 / 高飛球杯　檸檬片裝飾',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '白蘭地調酒' LIMIT 1),
  '白蘭地亞歷山大', 'Alexander',
  '白蘭地 20ml・深可可香甜酒 20ml・奶精 20ml　搖盪法 / 雞尾酒杯　荳蔻粉裝飾',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '白蘭地調酒' LIMIT 1),
  '東印度大樓', 'East India House',
  '白蘭地 52ml・白色蘭姆酒 7.5ml・香澄干邑白蘭地 7.5ml・鳳梨汁 90ml・安格式苦精少許　搖盪法 / 雞尾酒杯',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '白蘭地調酒' LIMIT 1),
  '床第之間', 'Between the Sheets',
  '白蘭地 30ml・白色蘭姆酒 30ml・君度橙皮香甜酒 30ml・新鮮檸檬汁 15ml　搖盪法 / 雞尾酒杯',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '白蘭地調酒' LIMIT 1),
  '傑克蘿絲', 'Jack Rose',
  '蘋果白蘭地 45ml・紅石榴糖漿 15ml・新鮮檸檬汁 15ml　搖盪法 / 雞尾酒杯',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '白蘭地調酒' LIMIT 1),
  '尼古拉斯', 'Nikolaschka',
  '干邑白蘭地 30ml・檸檬片・糖 8g　直接注入法 / Shot 杯',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '白蘭地調酒' LIMIT 1),
  '醉漢', 'Stingery',
  '白蘭地 45ml・白薄荷香甜酒(Crème De Menthe) 20ml　搖盪法 / 雞尾酒杯',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '白蘭地調酒' LIMIT 1),
  '蛋酒', 'Egg Nog',
  '白蘭地 30ml・白色蘭姆酒 15ml・鮮奶 120ml・果糖 15ml・蛋黃 1 顆　搖盪法 / 高飛球杯　荳蔻粉裝飾',
  350, 'DRINK', 1, 1, 'admin'
);

-- ── 蘭姆酒調酒（3 款）──────────────────────────────────────
INSERT INTO product (category_id, name_zh, name_en, description_zh, price, type, is_active, is_available, created_by) VALUES
(
  (SELECT id FROM category WHERE name_zh = '蘭姆酒調酒' LIMIT 1),
  '莫希多', 'Mojito',
  '白色蘭姆酒 45ml・新鮮檸檬汁 15ml・新鮮薄荷葉 12 片・糖 8g・蘇打水　壓榨法 / 高飛球杯　薄荷枝裝飾',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '蘭姆酒調酒' LIMIT 1),
  '拓荒者賓治', 'Planter''s Punch',
  '深色蘭姆酒 45ml・新鮮檸檬汁 15ml・紅石榴糖漿 10ml・蘇打水・安格式苦精少許　搖盪法 / 可林杯',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '蘭姆酒調酒' LIMIT 1),
  '邁泰', 'Mai Tai',
  '白色蘭姆酒 30ml・深色蘭姆酒 30ml（漂浮）・柑橘香甜酒 15ml・果糖 10ml・新鮮檸檬汁 10ml　壓榨 + 漂浮法 / 古典杯',
  350, 'DRINK', 1, 1, 'admin'
);

-- ── 伏特加調酒（2 款）──────────────────────────────────────
INSERT INTO product (category_id, name_zh, name_en, description_zh, price, type, is_active, is_available, created_by) VALUES
(
  (SELECT id FROM category WHERE name_zh = '伏特加調酒' LIMIT 1),
  '螺絲起子', 'Screwdriver',
  '伏特加 50ml・新鮮柳橙汁 100ml　攪拌法 / 高飛球杯　柳橙片裝飾',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '伏特加調酒' LIMIT 1),
  '灰狗／鹹狗', 'Greyhound / Salty Dog',
  '伏特加 50ml・葡萄柚汁 100ml　攪拌法 / 高飛球杯　（鹹狗版：鹽口杯）',
  350, 'DRINK', 1, 1, 'admin'
);

-- ── 綜合調酒（2 款）────────────────────────────────────────
INSERT INTO product (category_id, name_zh, name_en, description_zh, price, type, is_active, is_available, created_by) VALUES
(
  (SELECT id FROM category WHERE name_zh = '綜合調酒' LIMIT 1),
  '環遊世界', 'Around the World',
  '琴酒・伏特加・蘭姆酒・龍舌蘭・白蘭地・威士忌・Triple Sec 各 10ml・柳橙汁加滿　直接注入法 / 高腳水杯',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '綜合調酒' LIMIT 1),
  '長島冰茶', 'Long Island Iced Tea',
  '琴酒・伏特加・白色蘭姆酒・龍舌蘭・Triple Sec 各 15ml・新鮮檸檬汁 15ml・果糖 10ml・可樂　直接注入法 / 可林杯',
  350, 'DRINK', 1, 1, 'admin'
);

-- 調酒無客製化維度，顧客以備註欄溝通特殊需求即可。

