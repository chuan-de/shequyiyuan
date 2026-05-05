create table if not exists file_metadata (
    id bigserial primary key,
    business_type varchar(64) not null,
    business_id varchar(128),
    uploader_id bigint not null references app_user(id),
    original_filename varchar(255) not null,
    mime_type varchar(128) not null,
    file_size bigint not null,
    file_hash varchar(128),
    storage_path varchar(512) not null unique,
    status varchar(32) not null,
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now()
);

create index if not exists idx_file_metadata_business on file_metadata(business_type, business_id);
create index if not exists idx_file_metadata_uploader on file_metadata(uploader_id);
create index if not exists idx_file_metadata_hash on file_metadata(file_hash);
