-- 健康档案（jiuankangdangan）模块下线：自由文本档案的职责已由
-- 患者档案扩展字段（V47：过敏史/既往病史等）+ 慢病随访（V49：结构化指标）承接。
DROP TABLE IF EXISTS health_record;

-- 权限及 legacy 映射清理（mapping 表对 permission_code 有外键，先删映射）。
DELETE FROM app_legacy_permission_mapping
 WHERE permission_code IN (
    SELECT permission_code FROM app_permission
     WHERE permission_code LIKE 'health-records:%' OR permission_code LIKE 'jiuankangdangan:%');

DELETE FROM app_role_permission
 WHERE permission_id IN (
    SELECT id FROM app_permission
     WHERE permission_code LIKE 'health-records:%' OR permission_code LIKE 'jiuankangdangan:%');

DELETE FROM app_permission
 WHERE permission_code LIKE 'health-records:%' OR permission_code LIKE 'jiuankangdangan:%';

-- 字典「档案类型」随模块一并下线。
DELETE FROM dictionary_item WHERE dict_code = 'jiuankangdangan_types';

-- AI 摄取死信中残留的健康档案条目。
DELETE FROM ai_dead_letter WHERE source_type = 'health_record';
