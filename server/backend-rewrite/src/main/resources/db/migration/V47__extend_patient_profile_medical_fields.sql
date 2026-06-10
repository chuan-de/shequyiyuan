-- 患者档案补充医疗相关基础信息：出生日期、住址、过敏史、既往病史、紧急联系人。
ALTER TABLE patient_profile
    ADD COLUMN IF NOT EXISTS birth_date              DATE,
    ADD COLUMN IF NOT EXISTS address                 VARCHAR(255),
    ADD COLUMN IF NOT EXISTS allergies               TEXT,
    ADD COLUMN IF NOT EXISTS medical_history         TEXT,
    ADD COLUMN IF NOT EXISTS emergency_contact_name  VARCHAR(100),
    ADD COLUMN IF NOT EXISTS emergency_contact_phone VARCHAR(20);
