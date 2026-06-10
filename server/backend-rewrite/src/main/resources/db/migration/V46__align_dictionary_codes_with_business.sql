-- V46: 字典数据与业务字段对齐。
--
-- 背景：业务表中的字典字段均为 INTEGER（department.dept_types、
-- health_record.unit_types、*.sex_types），但 V3 种入的字典项 item_code
-- 是拼音/英文字符串（neike / normal …），两边对不上，导致字典从未被业务消费。
--
-- 本脚本：
--   1. 删除 keshi_types 字典 —— 就诊记录的「科室」由 department 表联动
--      （前端下拉来自科室管理，新增科室自动生效），不再走字典。
--   2. jiuankangdangan_types（健康档案类型）item_code 改为数字码 1/2/3，
--      与 health_record.unit_types (INTEGER) 对齐。
--   3. 新增 dept_types 字典（科室类别），对应 department.dept_types 取值
--      （与 scripts/seed_dev_data.sql 的科室种子一致：1 临床 / 2 中医康复 / 3 急诊）。
--   sex_types 已是数字码（1 男 / 2 女），无需调整。

-- 1. 清理从未被消费的 keshi_types 字典（就诊科室改由 department 表提供）
DELETE FROM dictionary_item WHERE dict_code = 'keshi_types';

-- 2. jiuankangdangan_types：旧英文码 → 数字码
UPDATE dictionary_item SET item_code = '1' WHERE dict_code = 'jiuankangdangan_types' AND item_code = 'normal';
UPDATE dictionary_item SET item_code = '2' WHERE dict_code = 'jiuankangdangan_types' AND item_code = 'chronic';
UPDATE dictionary_item SET item_code = '3' WHERE dict_code = 'jiuankangdangan_types' AND item_code = 'elderly';

-- 3. dept_types：科室类别（department.dept_types 消费）
INSERT INTO dictionary_item (dict_code, dict_name, item_code, item_name, sort_order, enabled) VALUES
    ('dept_types', '科室类别', '1', '临床科室',   1, TRUE),
    ('dept_types', '科室类别', '2', '中医与康复', 2, TRUE),
    ('dept_types', '科室类别', '3', '急诊急救',   3, TRUE)
ON CONFLICT (dict_code, item_code) DO NOTHING;
