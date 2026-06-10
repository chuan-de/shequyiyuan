-- 业务链串联：医生归属科室、就诊关联接诊医生、病历关联就诊。
ALTER TABLE doctor_profile
    ADD COLUMN IF NOT EXISTS department_id BIGINT REFERENCES department(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_doctor_department ON doctor_profile(department_id);

ALTER TABLE visit_record
    ADD COLUMN IF NOT EXISTS doctor_id BIGINT REFERENCES doctor_profile(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_visit_doctor ON visit_record(doctor_id);

ALTER TABLE medical_record
    ADD COLUMN IF NOT EXISTS visit_id BIGINT REFERENCES visit_record(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_medical_record_visit ON medical_record(visit_id);
