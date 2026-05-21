CREATE TABLE IF NOT EXISTS jiuankangdangan_record (
    id                      BIGSERIAL PRIMARY KEY,
    yonghu_id               BIGINT,
    yonghu_name             VARCHAR(100),
    yonghu_phone            VARCHAR(50),
    yonghu_id_number        VARCHAR(50),
    yonghu_email            VARCHAR(200),
    jiuankangdangan_name    VARCHAR(200),
    jiuankangdangan_qita    TEXT,
    jiuankangdangan_types   INTEGER,
    insert_time             TIMESTAMPTZ,
    jiuankangdangan_content TEXT,
    status                  VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    version                 BIGINT      NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_jiuankangdangan_status ON jiuankangdangan_record (status);
CREATE INDEX IF NOT EXISTS idx_jiuankangdangan_yonghu ON jiuankangdangan_record (yonghu_id);
