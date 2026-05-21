CREATE TABLE IF NOT EXISTS bingli_record (
    id                  BIGSERIAL PRIMARY KEY,
    yisheng_id          BIGINT,
    yisheng_uuid_number VARCHAR(100),
    yisheng_name        VARCHAR(100),
    yisheng_phone       VARCHAR(50),
    yisheng_id_number   VARCHAR(50),
    yisheng_email       VARCHAR(200),
    yonghu_id           BIGINT,
    yonghu_name         VARCHAR(100),
    yonghu_phone        VARCHAR(50),
    yonghu_id_number    VARCHAR(50),
    yonghu_email        VARCHAR(200),
    bingli_uuid_number  VARCHAR(100),
    bingli_name         VARCHAR(200),
    bingli_bingqing     TEXT,
    jianchaxiangmu      TEXT,
    jianchajieguo       TEXT,
    status              VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    version             BIGINT      NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_bingli_record_status  ON bingli_record (status);
CREATE INDEX IF NOT EXISTS idx_bingli_record_yonghu  ON bingli_record (yonghu_id);
CREATE INDEX IF NOT EXISTS idx_bingli_record_yisheng ON bingli_record (yisheng_id);
