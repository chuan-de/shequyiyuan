-- Rename jiuankangdangan_record to health_record and columns to English
ALTER TABLE jiuankangdangan_record RENAME TO health_record;
ALTER TABLE health_record RENAME COLUMN yonghu_id TO patient_id;
ALTER TABLE health_record RENAME COLUMN yonghu_name TO patient_name;
ALTER TABLE health_record RENAME COLUMN yonghu_phone TO patient_phone;
ALTER TABLE health_record RENAME COLUMN yonghu_id_number TO patient_id_number;
ALTER TABLE health_record RENAME COLUMN yonghu_email TO patient_email;
ALTER TABLE health_record RENAME COLUMN jiuankangdangan_name TO title;
ALTER TABLE health_record RENAME COLUMN jiuankangdangan_qita TO other_members;
ALTER TABLE health_record RENAME COLUMN jiuankangdangan_types TO unit_types;
ALTER TABLE health_record RENAME COLUMN jiuankangdangan_content TO content;
ALTER TABLE health_record RENAME COLUMN insert_time TO recorded_at;
