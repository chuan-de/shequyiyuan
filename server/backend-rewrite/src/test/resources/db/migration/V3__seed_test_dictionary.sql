CREATE TABLE IF NOT EXISTS dictionary (
    id BIGSERIAL PRIMARY KEY,
    dict_code VARCHAR(64) NOT NULL UNIQUE,
    dict_name VARCHAR(128) NOT NULL
);

CREATE TABLE IF NOT EXISTS dictionary_item (
    id BIGSERIAL PRIMARY KEY,
    dict_id BIGINT NOT NULL REFERENCES dictionary(id) ON DELETE CASCADE,
    item_code VARCHAR(64) NOT NULL,
    item_name VARCHAR(128) NOT NULL,
    UNIQUE(dict_id, item_code)
);

INSERT INTO dictionary (dict_code, dict_name)
VALUES ('GENDER', 'Gender')
ON CONFLICT (dict_code) DO NOTHING;

INSERT INTO dictionary_item (dict_id, item_code, item_name)
SELECT d.id, v.item_code, v.item_name
FROM dictionary d
JOIN (
    VALUES ('M', 'Male'), ('F', 'Female')
) AS v(item_code, item_name) ON TRUE
WHERE d.dict_code = 'GENDER'
ON CONFLICT (dict_id, item_code) DO NOTHING;
