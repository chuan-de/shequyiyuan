CREATE TABLE IF NOT EXISTS dictionary_item (
    id BIGSERIAL PRIMARY KEY,
    dict_code VARCHAR(100) NOT NULL,
    dict_name VARCHAR(100) NOT NULL,
    item_code VARCHAR(100) NOT NULL,
    item_name VARCHAR(100) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_dictionary_item_dict_code
    ON dictionary_item (dict_code);

CREATE INDEX IF NOT EXISTS idx_dictionary_item_enabled
    ON dictionary_item (enabled);

INSERT INTO dictionary_item (dict_code, dict_name, item_code, item_name, sort_order, enabled)
VALUES
    ('sex_types', '性别类型', '1', '男', 1, TRUE),
    ('sex_types', '性别类型', '2', '女', 2, TRUE),
    ('keshi_types', '科室类型', 'neike', '内科', 1, TRUE),
    ('keshi_types', '科室类型', 'waike', '外科', 2, TRUE),
    ('keshi_types', '科室类型', 'erbihouke', '耳鼻喉科', 3, TRUE),
    ('jiuankangdangan_types', '健康档案类型', 'normal', '普通档案', 1, TRUE),
    ('jiuankangdangan_types', '健康档案类型', 'chronic', '慢病档案', 2, TRUE),
    ('jiuankangdangan_types', '健康档案类型', 'elderly', '老年档案', 3, TRUE)
ON CONFLICT DO NOTHING;
