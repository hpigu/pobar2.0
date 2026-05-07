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
INSERT INTO product (category_id, name_zh, name_en, price, type, is_active, is_available, created_by) VALUES
(
  (SELECT id FROM category WHERE name_zh = '琴酒調酒' LIMIT 1),
  '琴費士', 'Gin Fizz',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '琴酒調酒' LIMIT 1),
  '藍鳥', 'Blue Bird',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '琴酒調酒' LIMIT 1),
  '白色佳人', 'White Lady',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '琴酒調酒' LIMIT 1),
  '橘花', 'Orange Blossom',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '琴酒調酒' LIMIT 1),
  '新加坡司令', 'Singapore Sling',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '琴酒調酒' LIMIT 1),
  '琴蕾', 'Gimlet',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '琴酒調酒' LIMIT 1),
  '不甜馬丁尼', 'Dry Martini',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '琴酒調酒' LIMIT 1),
  '尼格尼羅', 'Negroni',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '琴酒調酒' LIMIT 1),
  '茶通寧', 'Tea Tonic',
  350, 'DRINK', 1, 1, 'admin'
);

-- ── 威士忌調酒（15 款）──────────────────────────────────────
INSERT INTO product (category_id, name_zh, name_en, price, type, is_active, is_available, created_by) VALUES
(
  (SELECT id FROM category WHERE name_zh = '威士忌調酒' LIMIT 1),
  '蘇格蘭蘇打', 'Scotch Soda',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '威士忌調酒' LIMIT 1),
  '加拿大七喜', 'Canadian 7-UP',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '威士忌調酒' LIMIT 1),
  '約翰可林', 'John Collins',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '威士忌調酒' LIMIT 1),
  '領航者可林', 'Captain Collins',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '威士忌調酒' LIMIT 1),
  '曼哈頓', 'Manhattan',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '威士忌調酒' LIMIT 1),
  '不甜曼哈頓', 'Dry Manhattan',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '威士忌調酒' LIMIT 1),
  '威士忌酸酒', 'Whisky Sour',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '威士忌調酒' LIMIT 1),
  '沉默的第三者', 'Silent Third',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '威士忌調酒' LIMIT 1),
  '教父', 'God Father',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '威士忌調酒' LIMIT 1),
  '鏽釘子', 'Rusty Nail',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '威士忌調酒' LIMIT 1),
  '古典酒', 'Old Fashioned',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '威士忌調酒' LIMIT 1),
  '花花公子', 'Boulevardier',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '威士忌調酒' LIMIT 1),
  '蘋果曼哈頓', 'Apple Manhattan',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '威士忌調酒' LIMIT 1),
  '愛爾蘭汽車炸彈', 'Irish Car Bomb',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '威士忌調酒' LIMIT 1),
  '老夥伴', 'Old Pal',
  350, 'DRINK', 1, 1, 'admin'
);

-- ── 白蘭地調酒（11 款）──────────────────────────────────────
INSERT INTO product (category_id, name_zh, name_en, price, type, is_active, is_available, created_by) VALUES
(
  (SELECT id FROM category WHERE name_zh = '白蘭地調酒' LIMIT 1),
  '熱托地', 'Hot Toddy',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '白蘭地調酒' LIMIT 1),
  '馬頸', 'Horse''s Neck',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '白蘭地調酒' LIMIT 1),
  '白蘭地巴克', 'Brandy Back',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '白蘭地調酒' LIMIT 1),
  '白蘭地蘇打', 'Brandy Soda',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '白蘭地調酒' LIMIT 1),
  '白蘭地亞歷山大', 'Alexander',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '白蘭地調酒' LIMIT 1),
  '東印度大樓', 'East India House',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '白蘭地調酒' LIMIT 1),
  '床第之間', 'Between the Sheets',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '白蘭地調酒' LIMIT 1),
  '傑克蘿絲', 'Jack Rose',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '白蘭地調酒' LIMIT 1),
  '尼古拉斯', 'Nikolaschka',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '白蘭地調酒' LIMIT 1),
  '醉漢', 'Stingery',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '白蘭地調酒' LIMIT 1),
  '蛋酒', 'Egg Nog',
  350, 'DRINK', 1, 1, 'admin'
);

-- ── 蘭姆酒調酒（3 款）──────────────────────────────────────
INSERT INTO product (category_id, name_zh, name_en, price, type, is_active, is_available, created_by) VALUES
(
  (SELECT id FROM category WHERE name_zh = '蘭姆酒調酒' LIMIT 1),
  '莫希多', 'Mojito',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '蘭姆酒調酒' LIMIT 1),
  '拓荒者賓治', 'Planter''s Punch',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '蘭姆酒調酒' LIMIT 1),
  '邁泰', 'Mai Tai',
  350, 'DRINK', 1, 1, 'admin'
);

-- ── 伏特加調酒（2 款）──────────────────────────────────────
INSERT INTO product (category_id, name_zh, name_en, price, type, is_active, is_available, created_by) VALUES
(
  (SELECT id FROM category WHERE name_zh = '伏特加調酒' LIMIT 1),
  '螺絲起子', 'Screwdriver',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '伏特加調酒' LIMIT 1),
  '灰狗／鹹狗', 'Greyhound / Salty Dog',
  350, 'DRINK', 1, 1, 'admin'
);

-- ── 綜合調酒（2 款）────────────────────────────────────────
INSERT INTO product (category_id, name_zh, name_en, price, type, is_active, is_available, created_by) VALUES
(
  (SELECT id FROM category WHERE name_zh = '綜合調酒' LIMIT 1),
  '環遊世界', 'Around the World',
  350, 'DRINK', 1, 1, 'admin'
),
(
  (SELECT id FROM category WHERE name_zh = '綜合調酒' LIMIT 1),
  '長島冰茶', 'Long Island Iced Tea',
  350, 'DRINK', 1, 1, 'admin'
);

-- 調酒無客製化維度，顧客以備註欄溝通特殊需求即可。
