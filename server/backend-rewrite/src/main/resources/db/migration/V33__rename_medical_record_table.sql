-- Rename bingli_record to medical_record and columns to English
ALTER TABLE bingli_record RENAME TO medical_record;
ALTER TABLE medical_record RENAME COLUMN yisheng_id TO doctor_id;
ALTER TABLE medical_record RENAME COLUMN yisheng_uuid_number TO doctor_uuid_number;
ALTER TABLE medical_record RENAME COLUMN yisheng_name TO doctor_name;
ALTER TABLE medical_record RENAME COLUMN yisheng_phone TO doctor_phone;
ALTER TABLE medical_record RENAME COLUMN yisheng_id_number TO doctor_id_number;
ALTER TABLE medical_record RENAME COLUMN yisheng_email TO doctor_email;
ALTER TABLE medical_record RENAME COLUMN yonghu_id TO patient_id;
ALTER TABLE medical_record RENAME COLUMN yonghu_name TO patient_name;
ALTER TABLE medical_record RENAME COLUMN yonghu_phone TO patient_phone;
ALTER TABLE medical_record RENAME COLUMN yonghu_id_number TO patient_id_number;
ALTER TABLE medical_record RENAME COLUMN yonghu_email TO patient_email;
ALTER TABLE medical_record RENAME COLUMN bingli_uuid_number TO case_number;
ALTER TABLE medical_record RENAME COLUMN bingli_name TO case_name;
ALTER TABLE medical_record RENAME COLUMN bingli_bingqing TO condition_desc;
ALTER TABLE medical_record RENAME COLUMN jianchaxiangmu TO exam_items;
ALTER TABLE medical_record RENAME COLUMN jianchajieguo TO exam_results;
