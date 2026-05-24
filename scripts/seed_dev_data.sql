-- ============================================================================
-- 开发环境演示数据种子
--
-- 包含：科室 12 个，患者 12 个，医生 6 个，家庭医生 4 个。
-- 所有账号统一密码：123456（BCrypt cost=10）
-- 幂等：所有 INSERT 均带 ON CONFLICT DO NOTHING，可重复运行。
--
-- 运行：
--   docker exec -i shequyiyuan-postgres-1 \
--     psql -U hospital -d hospital < scripts/seed_dev_data.sql
-- ============================================================================

BEGIN;

-- ----------------------------------------------------------------------------
-- 1. 科室
-- ----------------------------------------------------------------------------

INSERT INTO department (dept_name, description, head_person, phone, dept_types) VALUES
  ('全科医学科', '常见病、多发病初诊与转诊', '王志强', '0512-66001001', 1),
  ('内科',       '呼吸、消化、心血管常见病诊治', '张丽华', '0512-66001002', 1),
  ('外科',       '常规外伤、疝气、阑尾炎等',     '李伟',   '0512-66001003', 1),
  ('妇产科',     '妇科常见病、孕期保健',         '赵敏',   '0512-66001004', 1),
  ('儿科',       '0-14 岁儿童常见病诊疗',         '陈军',   '0512-66001005', 1),
  ('中医科',     '中医辨证、针灸、推拿',         '刘静',   '0512-66001006', 2),
  ('口腔科',     '龋齿、牙周、洁牙',             '周琳',   '0512-66001007', 1),
  ('眼科',       '常见眼病、视力检查',           '吴明',   '0512-66001008', 1),
  ('耳鼻喉科',   '鼻炎、咽炎、中耳炎',           '孙建',   '0512-66001009', 1),
  ('皮肤科',     '湿疹、皮炎、过敏诊治',         '黄菁',   '0512-66001010', 1),
  ('急诊科',     '24 小时急诊处置',              '徐健',   '0512-66001011', 3),
  ('康复科',     '术后康复、慢病管理',           '杨敏',   '0512-66001012', 2)
ON CONFLICT (dept_name) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 2. 患者（12 名）—— app_user + patient_profile + app_user_role
-- 密码统一 123456
-- ----------------------------------------------------------------------------

WITH new_users AS (
  INSERT INTO app_user (username, password_hash, enabled) VALUES
    ('patient01', '$2b$10$G3CjtOG5F1ZA4WVFz0ek0ONGFiuIbne1nfhz5t9pj6YM7YxcD1In6', true),
    ('patient02', '$2b$10$G3CjtOG5F1ZA4WVFz0ek0ONGFiuIbne1nfhz5t9pj6YM7YxcD1In6', true),
    ('patient03', '$2b$10$G3CjtOG5F1ZA4WVFz0ek0ONGFiuIbne1nfhz5t9pj6YM7YxcD1In6', true),
    ('patient04', '$2b$10$G3CjtOG5F1ZA4WVFz0ek0ONGFiuIbne1nfhz5t9pj6YM7YxcD1In6', true),
    ('patient05', '$2b$10$G3CjtOG5F1ZA4WVFz0ek0ONGFiuIbne1nfhz5t9pj6YM7YxcD1In6', true),
    ('patient06', '$2b$10$G3CjtOG5F1ZA4WVFz0ek0ONGFiuIbne1nfhz5t9pj6YM7YxcD1In6', true),
    ('patient07', '$2b$10$G3CjtOG5F1ZA4WVFz0ek0ONGFiuIbne1nfhz5t9pj6YM7YxcD1In6', true),
    ('patient08', '$2b$10$G3CjtOG5F1ZA4WVFz0ek0ONGFiuIbne1nfhz5t9pj6YM7YxcD1In6', true),
    ('patient09', '$2b$10$G3CjtOG5F1ZA4WVFz0ek0ONGFiuIbne1nfhz5t9pj6YM7YxcD1In6', true),
    ('patient10', '$2b$10$G3CjtOG5F1ZA4WVFz0ek0ONGFiuIbne1nfhz5t9pj6YM7YxcD1In6', true),
    ('patient11', '$2b$10$G3CjtOG5F1ZA4WVFz0ek0ONGFiuIbne1nfhz5t9pj6YM7YxcD1In6', true),
    ('patient12', '$2b$10$G3CjtOG5F1ZA4WVFz0ek0ONGFiuIbne1nfhz5t9pj6YM7YxcD1In6', true)
  ON CONFLICT (username) DO NOTHING
  RETURNING id, username
)
INSERT INTO app_user_role (user_id, role_id)
SELECT nu.id, r.id
FROM new_users nu CROSS JOIN app_role r
WHERE r.role_code = 'PATIENT'
ON CONFLICT DO NOTHING;

-- patient_profile —— 用 username 反查 user_id，1 vs N 写法
INSERT INTO patient_profile (user_id, full_name, sex_types, phone, id_number, email)
SELECT u.id, v.full_name, v.sex_types, v.phone, v.id_number, v.email
FROM (VALUES
  ('patient01', '张三',     1, '13800100001', '110101199001010001', 'zhangsan@example.com'),
  ('patient02', '李四',     1, '13800100002', '110101199001020002', 'lisi@example.com'),
  ('patient03', '王五',     1, '13800100003', '110101199001030003', 'wangwu@example.com'),
  ('patient04', '赵六',     2, '13800100004', '110101199001040004', 'zhaoliu@example.com'),
  ('patient05', '刘七',     1, '13800100005', '110101199001050005', 'liuqi@example.com'),
  ('patient06', '陈八',     2, '13800100006', '110101199001060006', 'chenba@example.com'),
  ('patient07', '杨九',     1, '13800100007', '110101199001070007', 'yangjiu@example.com'),
  ('patient08', '周十',     2, '13800100008', '110101199001080008', 'zhoushi@example.com'),
  ('patient09', '吴小芳',   2, '13800100009', '110101199001090009', 'wuxf@example.com'),
  ('patient10', '徐建国',   1, '13800100010', '110101199001100010', 'xujg@example.com'),
  ('patient11', '孙美玲',   2, '13800100011', '110101199001110011', 'sunml@example.com'),
  ('patient12', '黄文斌',   1, '13800100012', '110101199001120012', 'huangwb@example.com')
) AS v(username, full_name, sex_types, phone, id_number, email)
JOIN app_user u ON u.username = v.username
ON CONFLICT (user_id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 3. 医生（6 名）
-- ----------------------------------------------------------------------------

WITH new_users AS (
  INSERT INTO app_user (username, password_hash, enabled) VALUES
    ('doctor_wang',  '$2b$10$G3CjtOG5F1ZA4WVFz0ek0ONGFiuIbne1nfhz5t9pj6YM7YxcD1In6', true),
    ('doctor_zhang', '$2b$10$G3CjtOG5F1ZA4WVFz0ek0ONGFiuIbne1nfhz5t9pj6YM7YxcD1In6', true),
    ('doctor_li',    '$2b$10$G3CjtOG5F1ZA4WVFz0ek0ONGFiuIbne1nfhz5t9pj6YM7YxcD1In6', true),
    ('doctor_zhao',  '$2b$10$G3CjtOG5F1ZA4WVFz0ek0ONGFiuIbne1nfhz5t9pj6YM7YxcD1In6', true),
    ('doctor_chen',  '$2b$10$G3CjtOG5F1ZA4WVFz0ek0ONGFiuIbne1nfhz5t9pj6YM7YxcD1In6', true),
    ('doctor_liu',   '$2b$10$G3CjtOG5F1ZA4WVFz0ek0ONGFiuIbne1nfhz5t9pj6YM7YxcD1In6', true)
  ON CONFLICT (username) DO NOTHING
  RETURNING id, username
)
INSERT INTO app_user_role (user_id, role_id)
SELECT nu.id, r.id
FROM new_users nu CROSS JOIN app_role r
WHERE r.role_code = 'DOCTOR'
ON CONFLICT DO NOTHING;

INSERT INTO doctor_profile (user_id, uuid_number, full_name, sex_types, phone, id_number, email)
SELECT u.id, v.uuid_number, v.full_name, v.sex_types, v.phone, v.id_number, v.email
FROM (VALUES
  ('doctor_wang',  'YS001', '王医生',  1, '13800200001', '110101198001010001', 'wang@hospital.example'),
  ('doctor_zhang', 'YS002', '张医生',  2, '13800200002', '110101198002020002', 'zhang@hospital.example'),
  ('doctor_li',    'YS003', '李医生',  1, '13800200003', '110101198003030003', 'li@hospital.example'),
  ('doctor_zhao',  'YS004', '赵医生',  2, '13800200004', '110101198004040004', 'zhao@hospital.example'),
  ('doctor_chen',  'YS005', '陈医生',  1, '13800200005', '110101198005050005', 'chen@hospital.example'),
  ('doctor_liu',   'YS006', '刘医生',  2, '13800200006', '110101198006060006', 'liu@hospital.example')
) AS v(username, uuid_number, full_name, sex_types, phone, id_number, email)
JOIN app_user u ON u.username = v.username
ON CONFLICT (user_id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 4. 家庭医生（4 名）
-- ----------------------------------------------------------------------------

WITH new_users AS (
  INSERT INTO app_user (username, password_hash, enabled) VALUES
    ('fdoctor_wu',    '$2b$10$G3CjtOG5F1ZA4WVFz0ek0ONGFiuIbne1nfhz5t9pj6YM7YxcD1In6', true),
    ('fdoctor_zhou',  '$2b$10$G3CjtOG5F1ZA4WVFz0ek0ONGFiuIbne1nfhz5t9pj6YM7YxcD1In6', true),
    ('fdoctor_sun',   '$2b$10$G3CjtOG5F1ZA4WVFz0ek0ONGFiuIbne1nfhz5t9pj6YM7YxcD1In6', true),
    ('fdoctor_huang', '$2b$10$G3CjtOG5F1ZA4WVFz0ek0ONGFiuIbne1nfhz5t9pj6YM7YxcD1In6', true)
  ON CONFLICT (username) DO NOTHING
  RETURNING id, username
)
INSERT INTO app_user_role (user_id, role_id)
SELECT nu.id, r.id
FROM new_users nu CROSS JOIN app_role r
WHERE r.role_code = 'FAMILY_DOCTOR'
ON CONFLICT DO NOTHING;

INSERT INTO family_doctor_profile (user_id, full_name, sex_types, phone, email)
SELECT u.id, v.full_name, v.sex_types, v.phone, v.email
FROM (VALUES
  ('fdoctor_wu',    '吴医生',  1, '13800300001', 'wu@hospital.example'),
  ('fdoctor_zhou',  '周医生',  2, '13800300002', 'zhou@hospital.example'),
  ('fdoctor_sun',   '孙医生',  1, '13800300003', 'sun@hospital.example'),
  ('fdoctor_huang', '黄医生',  2, '13800300004', 'huang@hospital.example')
) AS v(username, full_name, sex_types, phone, email)
JOIN app_user u ON u.username = v.username
ON CONFLICT (user_id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 5. 药品（20 种常用药）
-- code 用 'YP-DEMO-NNN' 前缀，与自动生成的 'YP+yyyyMMddHHmmss' 区分开。
-- medication.code 无 UNIQUE 约束，用 WHERE NOT EXISTS 实现幂等。
-- ----------------------------------------------------------------------------

INSERT INTO medication (code, name, price, stock, main_effect, side_effect, detail)
SELECT v.code, v.name, v.price, v.stock, v.main_effect, v.side_effect, v.detail
FROM (VALUES
  -- 抗感染 / 抗生素
  ('YP-DEMO-001', '阿莫西林胶囊',        12.80, 500, '广谱青霉素类抗菌药，用于敏感菌感染', '皮疹、腹泻、过敏反应', '0.25g × 24 粒/盒，口服一次 0.5g 一日 3-4 次'),
  ('YP-DEMO-002', '头孢克肟分散片',      28.50, 300, '第三代头孢菌素，用于呼吸道、泌尿道感染', '胃肠道反应、皮疹', '50mg × 12 片/盒，成人 100mg 一日 2 次'),
  ('YP-DEMO-003', '阿奇霉素干混悬剂',    32.00, 200, '大环内酯类抗生素，儿童呼吸道感染常用', '腹泻、恶心、转氨酶升高', '0.1g × 6 袋/盒，体重 < 15kg 慎用'),
  ('YP-DEMO-004', '左氧氟沙星片',        18.60, 250, '喹诺酮类抗菌药，泌尿系统感染首选',     '光敏感、肌腱炎、18 岁以下禁用', '0.1g × 12 片/盒'),
  -- 解热镇痛
  ('YP-DEMO-005', '布洛芬缓释胶囊',       9.50, 800, '非甾体抗炎药，发热、头痛、关节痛',     '胃肠道刺激、肾损伤',           '0.3g × 20 粒/盒，一次 1 粒一日 2 次'),
  ('YP-DEMO-006', '对乙酰氨基酚片',       6.20, 1000, '退热镇痛，儿童成人通用',              '过量致肝损伤',                 '0.5g × 20 片/盒'),
  -- 抗过敏
  ('YP-DEMO-007', '氯雷他定片',          15.30, 400, '第二代抗组胺，过敏性鼻炎、荨麻疹',     '少数嗜睡、口干',               '10mg × 6 片/盒，每日 1 片'),
  -- 消化系统
  ('YP-DEMO-008', '蒙脱石散',             8.40, 600, '吸附性止泻，急慢性腹泻',              '便秘',                         '3g × 10 袋/盒，温水冲服'),
  ('YP-DEMO-009', '奥美拉唑肠溶胶囊',    22.00, 350, '质子泵抑制剂，胃溃疡、反流性食管炎',   '头痛、皮疹',                   '20mg × 14 粒/盒，晨起空腹服'),
  -- 心血管 / 高血压 / 糖尿病
  ('YP-DEMO-010', '阿司匹林肠溶片',       7.80, 700, '抗血小板聚集，心脑血管疾病二级预防',   '出血风险、胃肠道反应',         '100mg × 30 片/盒，每日 1 片'),
  ('YP-DEMO-011', '硝酸甘油片',           4.50, 200, '冠心病心绞痛急救用',                  '头痛、低血压',                 '0.5mg × 100 片/瓶，舌下含服'),
  ('YP-DEMO-012', '美托洛尔缓释片',      36.80, 280, 'β1 受体阻滞剂，高血压、心绞痛',        '心动过缓、乏力',               '47.5mg × 7 片/盒'),
  ('YP-DEMO-013', '缬沙坦胶囊',          42.00, 320, 'ARB 类降压药，高血压、心衰',           '头晕、高钾血症',               '80mg × 7 粒/盒'),
  ('YP-DEMO-014', '二甲双胍片',          14.20, 450, '2 型糖尿病一线用药',                  '胃肠道反应、乳酸酸中毒（罕见）','0.5g × 30 片/盒，餐中或餐后服'),
  -- 中药制剂
  ('YP-DEMO-015', '蒲地蓝消炎口服液',    25.00, 400, '清热解毒，咽炎、扁桃体炎',            '过敏者慎用',                   '10ml × 6 支/盒'),
  ('YP-DEMO-016', '复方丹参滴丸',        58.00, 220, '活血化瘀，冠心病心绞痛',              '少数胃部不适',                 '180 粒/瓶，每次 10 粒'),
  ('YP-DEMO-017', '板蓝根颗粒',           8.80, 800, '清热解毒，感冒初起咽痛',              '过敏体质慎用',                 '10g × 20 袋/盒'),
  -- 维生素 / 营养
  ('YP-DEMO-018', '维生素 C 片',          5.50, 1200, '坏血病、增强免疫、辅助治疗',          '大剂量致结石',                 '100mg × 100 片/瓶'),
  ('YP-DEMO-019', '维生素 B 族片',       12.00, 600, '神经系统营养，慢性疲劳、口角炎',      '尿液偏黄属正常',               '100 片/瓶'),
  ('YP-DEMO-020', '钙尔奇 D 片',         48.00, 380, '补钙 + 维生素 D，骨质疏松、孕妇',     '便秘、肾结石（罕见）',         '60 片/瓶')
) AS v(code, name, price, stock, main_effect, side_effect, detail)
WHERE NOT EXISTS (SELECT 1 FROM medication WHERE medication.code = v.code);

COMMIT;

-- ----------------------------------------------------------------------------
-- 6. 验证
-- ----------------------------------------------------------------------------

SELECT '科室' AS entity, count(*) AS rows FROM department
UNION ALL SELECT '患者', count(*) FROM patient_profile
UNION ALL SELECT '医生', count(*) FROM doctor_profile
UNION ALL SELECT '家庭医生', count(*) FROM family_doctor_profile
UNION ALL SELECT '药品', count(*) FROM medication;
