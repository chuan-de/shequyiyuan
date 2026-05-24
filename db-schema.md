# 数据库表结构

> 自动生成于 `scripts/dump_schema.py`。基于当前 Postgres 实例（Flyway V1–V45）。

> 共 26 张业务表（不含 `flyway_schema_history`）。


## 目录

- [认证 / RBAC](#认证--rbac)
  - [`app_user`](#app_user)
  - [`app_role`](#app_role)
  - [`app_user_role`](#app_user_role)
  - [`app_permission`](#app_permission)
  - [`app_role_permission`](#app_role_permission)
  - [`app_legacy_permission_mapping`](#app_legacy_permission_mapping)
- [业务主体](#业务主体)
  - [`patient_profile`](#patient_profile)
  - [`doctor_profile`](#doctor_profile)
  - [`family_doctor_profile`](#family_doctor_profile)
  - [`reception_profile`](#reception_profile)
  - [`department`](#department)
- [药品](#药品)
  - [`medication`](#medication)
  - [`medication_inventory_log`](#medication_inventory_log)
- [业务记录](#业务记录)
  - [`visit_record`](#visit_record)
  - [`medical_record`](#medical_record)
  - [`health_record`](#health_record)
- [字典 / 系统配置](#字典--系统配置)
  - [`dictionary_item`](#dictionary_item)
  - [`dictionary_operation_log`](#dictionary_operation_log)
  - [`system_config`](#system_config)
- [文件 / 媒体](#文件--媒体)
  - [`file_metadata`](#file_metadata)
  - [`photo`](#photo)
- [AI 模块（V37–V45）](#ai-模块（v37–v45）)
  - [`ai_audit_log`](#ai_audit_log)
  - [`ai_extraction_history`](#ai_extraction_history)
  - [`ai_dead_letter`](#ai_dead_letter)
  - [`ai_consult_session`](#ai_consult_session)
  - [`ai_consult_message`](#ai_consult_message)

---


## 认证 / RBAC

> 用户、角色、权限分配（V1–V14）


### `app_user`

登录账号（含 admin / 患者 / 医生等所有角色用户）。


| 字段 | 类型 | 默认 | 约束 |
|---|---|---|---|
| `id` | bigint | AUTO | PK |
| `username` | varchar(100) | — | UQ, NOT NULL |
| `password_hash` | varchar(255) | — | NOT NULL |
| `enabled` | boolean | true | NOT NULL |
| `created_at` | timestamptz | NOW() | NOT NULL |


### `app_role`

角色字典：ADMIN / USER / PATIENT / RECEPTION / DOCTOR / FAMILY_DOCTOR。


| 字段 | 类型 | 默认 | 约束 |
|---|---|---|---|
| `id` | bigint | AUTO | PK |
| `role_code` | varchar(50) | — | UQ, NOT NULL |
| `role_name` | varchar(100) | — | NOT NULL |


### `app_user_role`

用户—角色多对多关联。


| 字段 | 类型 | 默认 | 约束 |
|---|---|---|---|
| `user_id` | bigint | — | PK, FK→app_user.id |
| `role_id` | bigint | — | PK, FK→app_role.id |


### `app_permission`

细粒度权限码（如 `ai:vision`、`patients:write`）。


| 字段 | 类型 | 默认 | 约束 |
|---|---|---|---|
| `id` | bigint | AUTO | PK |
| `permission_code` | varchar(100) | — | UQ, NOT NULL |
| `permission_name` | varchar(200) | — | NOT NULL |
| `resource_code` | varchar(50) | 'unknown' | NOT NULL |
| `action_code` | varchar(50) | 'unknown' | NOT NULL |


### `app_role_permission`

角色—权限多对多关联。


| 字段 | 类型 | 默认 | 约束 |
|---|---|---|---|
| `role_id` | bigint | — | PK, FK→app_role.id |
| `permission_id` | bigint | — | PK, FK→app_permission.id |


### `app_legacy_permission_mapping`

legacy 菜单/接口 → 新权限码的兼容映射表。


| 字段 | 类型 | 默认 | 约束 |
|---|---|---|---|
| `id` | bigint | AUTO | PK |
| `legacy_module` | varchar(100) | — | NOT NULL |
| `menu_name` | varchar(100) | — | NOT NULL |
| `api_pattern` | varchar(200) | — | NOT NULL |
| `permission_code` | varchar(100) | — | FK→app_permission.permission_code, NOT NULL |
| `role_code` | varchar(50) | — | FK→app_role.role_code, NOT NULL |


## 业务主体

> 患者、医生、前台、家庭医生、科室


### `patient_profile`

患者档案。`ai_consent_at` 为 RAG 授权时间戳。


| 字段 | 类型 | 默认 | 约束 |
|---|---|---|---|
| `id` | bigint | AUTO | PK |
| `user_id` | bigint | — | FK→app_user.id, UQ, NOT NULL |
| `full_name` | varchar(100) | — | NOT NULL |
| `photo_url` | varchar(255) | — | — |
| `sex_types` | integer | — | — |
| `phone` | varchar(20) | — | UQ |
| `id_number` | varchar(30) | — | UQ |
| `email` | varchar(100) | — | — |
| `created_at` | timestamptz | NOW() | NOT NULL |
| `ai_consent_at` | timestamptz | — | — |


### `doctor_profile`

医生档案，含工号、身份证等敏感字段。


| 字段 | 类型 | 默认 | 约束 |
|---|---|---|---|
| `id` | bigint | AUTO | PK |
| `user_id` | bigint | — | FK→app_user.id, UQ, NOT NULL |
| `uuid_number` | varchar(50) | — | UQ |
| `full_name` | varchar(100) | — | NOT NULL |
| `photo_url` | varchar(255) | — | — |
| `sex_types` | integer | — | — |
| `phone` | varchar(20) | — | UQ |
| `id_number` | varchar(30) | — | UQ |
| `email` | varchar(100) | — | — |
| `created_at` | timestamptz | NOW() | NOT NULL |


### `family_doctor_profile`

家庭医生档案。


| 字段 | 类型 | 默认 | 约束 |
|---|---|---|---|
| `id` | bigint | AUTO | PK |
| `user_id` | bigint | — | FK→app_user.id, UQ, NOT NULL |
| `full_name` | varchar(100) | — | NOT NULL |
| `photo_url` | varchar(255) | — | — |
| `sex_types` | integer | — | — |
| `phone` | varchar(20) | — | UQ |
| `email` | varchar(100) | — | — |
| `created_at` | timestamptz | NOW() | NOT NULL |


### `reception_profile`

前台档案。


| 字段 | 类型 | 默认 | 约束 |
|---|---|---|---|
| `id` | bigint | AUTO | PK |
| `user_id` | bigint | — | FK→app_user.id, UQ, NOT NULL |
| `uuid_number` | varchar(50) | — | UQ |
| `full_name` | varchar(100) | — | NOT NULL |
| `photo_url` | varchar(255) | — | — |
| `sex_types` | integer | — | — |
| `phone` | varchar(20) | — | UQ |
| `email` | varchar(100) | — | — |
| `created_at` | timestamptz | NOW() | NOT NULL |


### `department`

科室定义（独立于字典）。


| 字段 | 类型 | 默认 | 约束 |
|---|---|---|---|
| `id` | bigint | AUTO | PK |
| `dept_name` | varchar(100) | — | UQ, NOT NULL |
| `description` | text | — | — |
| `head_person` | varchar(100) | — | — |
| `phone` | varchar(20) | — | — |
| `dept_types` | integer | — | — |
| `created_at` | timestamptz | NOW() | NOT NULL |
| `updated_at` | timestamptz | NOW() | NOT NULL |


## 药品

> 药品台账 + 库存流水


### `medication`

药品台账。`code` 自动生成 `YP+yyyyMMddHHmmss+3位`。


| 字段 | 类型 | 默认 | 约束 |
|---|---|---|---|
| `id` | bigint | AUTO | PK |
| `code` | varchar(100) | — | NOT NULL |
| `name` | varchar(100) | — | NOT NULL |
| `status` | varchar(32) | 'ENABLED' | NOT NULL |
| `version` | bigint | 0 | NOT NULL |
| `created_at` | timestamptz | NOW() | NOT NULL |
| `updated_at` | timestamptz | NOW() | NOT NULL |
| `price` | numeric | 0 | NOT NULL |
| `stock` | integer | 0 | NOT NULL |
| `main_effect` | text | — | — |
| `side_effect` | text | — | — |
| `detail` | text | — | — |


### `medication_inventory_log`

药品库存变动流水。


| 字段 | 类型 | 默认 | 约束 |
|---|---|---|---|
| `id` | bigint | AUTO | PK |
| `medication_id` | bigint | — | FK→medication.id, NOT NULL |
| `delta` | integer | — | NOT NULL |
| `stock_after` | integer | — | NOT NULL |
| `reason` | varchar(255) | — | — |
| `operator` | varchar(100) | — | — |
| `created_at` | timestamptz | NOW() | NOT NULL |


## 业务记录

> 就诊 / 病历 / 健康档案


### `visit_record`

就诊记录。`visit_number` 自动生成 `JZ+...`。


| 字段 | 类型 | 默认 | 约束 |
|---|---|---|---|
| `id` | bigint | AUTO | PK |
| `patient_id` | bigint | — | FK→patient_profile.id, NOT NULL |
| `visit_number` | varchar(50) | — | UQ, NOT NULL |
| `fee` | numeric | — | — |
| `keshi_types` | integer | — | — |
| `visit_date` | timestamptz | NOW() | NOT NULL |
| `registration_notes` | text | — | — |
| `visit_content` | text | — | — |
| `created_at` | timestamptz | NOW() | NOT NULL |


### `medical_record`

病历记录。`case_number` 自动生成 `BL+...`；`prescription_items` / `attachments` 是 JSONB；`ai_extracted` 存 Vision OCR 抽取的结构化字段。


| 字段 | 类型 | 默认 | 约束 |
|---|---|---|---|
| `id` | bigint | AUTO | PK |
| `doctor_id` | bigint | — | — |
| `doctor_uuid_number` | varchar(100) | — | — |
| `doctor_name` | varchar(100) | — | — |
| `doctor_phone` | varchar(50) | — | — |
| `doctor_id_number` | varchar(50) | — | — |
| `doctor_email` | varchar(200) | — | — |
| `patient_id` | bigint | — | — |
| `patient_name` | varchar(100) | — | — |
| `patient_phone` | varchar(50) | — | — |
| `patient_id_number` | varchar(50) | — | — |
| `patient_email` | varchar(200) | — | — |
| `case_number` | varchar(100) | — | — |
| `case_name` | varchar(200) | — | — |
| `condition_desc` | text | — | — |
| `exam_items` | text | — | — |
| `exam_results` | text | — | — |
| `status` | varchar(32) | 'DRAFT' | NOT NULL |
| `version` | bigint | 0 | NOT NULL |
| `created_at` | timestamptz | NOW() | NOT NULL |
| `updated_at` | timestamptz | NOW() | NOT NULL |
| `prescription_items` | jsonb | — | — |
| `attachments` | jsonb | — | — |
| `record_date` | timestamptz | — | — |
| `ai_extracted` | jsonb | — | — |


### `health_record`

健康档案。


| 字段 | 类型 | 默认 | 约束 |
|---|---|---|---|
| `id` | bigint | AUTO | PK |
| `patient_id` | bigint | — | — |
| `patient_name` | varchar(100) | — | — |
| `patient_phone` | varchar(50) | — | — |
| `patient_id_number` | varchar(50) | — | — |
| `patient_email` | varchar(200) | — | — |
| `title` | varchar(200) | — | — |
| `other_members` | text | — | — |
| `unit_types` | integer | — | — |
| `recorded_at` | timestamptz | — | — |
| `content` | text | — | — |
| `status` | varchar(32) | 'DRAFT' | NOT NULL |
| `version` | bigint | 0 | NOT NULL |
| `created_at` | timestamptz | NOW() | NOT NULL |
| `updated_at` | timestamptz | NOW() | NOT NULL |


## 字典 / 系统配置

> 数据字典 + 配置项


### `dictionary_item`

数据字典条目。


| 字段 | 类型 | 默认 | 约束 |
|---|---|---|---|
| `id` | bigint | AUTO | PK |
| `dict_code` | varchar(100) | — | UQ, NOT NULL |
| `dict_name` | varchar(100) | — | NOT NULL |
| `item_code` | varchar(100) | — | UQ, NOT NULL |
| `item_name` | varchar(100) | — | NOT NULL |
| `sort_order` | integer | 0 | NOT NULL |
| `enabled` | boolean | true | NOT NULL |
| `created_at` | timestamptz | NOW() | NOT NULL |
| `updated_at` | timestamptz | NOW() | NOT NULL |


### `dictionary_operation_log`

字典变更审计日志。


| 字段 | 类型 | 默认 | 约束 |
|---|---|---|---|
| `id` | bigint | AUTO | PK |
| `dictionary_item_id` | bigint | — | — |
| `operation_type` | varchar(20) | — | NOT NULL |
| `operator` | varchar(100) | — | NOT NULL |
| `before_value` | text | — | — |
| `after_value` | text | — | — |
| `operated_at` | timestamptz | NOW() | NOT NULL |


### `system_config`

系统级配置 key-value。


| 字段 | 类型 | 默认 | 约束 |
|---|---|---|---|
| `id` | bigint | AUTO | PK |
| `config_key` | varchar(200) | — | UQ, NOT NULL |
| `config_value` | text | — | NOT NULL |
| `status` | varchar(32) | 'ENABLED' | NOT NULL |
| `version` | bigint | 0 | NOT NULL |
| `created_at` | timestamptz | NOW() | NOT NULL |
| `updated_at` | timestamptz | NOW() | NOT NULL |


## 文件 / 媒体

> 通用文件元数据 + 照片 BYTEA


### `file_metadata`

通用文件元数据（按 business_type 关联业务）。


| 字段 | 类型 | 默认 | 约束 |
|---|---|---|---|
| `id` | bigint | AUTO | PK |
| `business_type` | varchar(64) | — | NOT NULL |
| `business_id` | varchar(128) | — | — |
| `uploader_id` | bigint | — | FK→app_user.id, NOT NULL |
| `original_filename` | varchar(255) | — | NOT NULL |
| `mime_type` | varchar(128) | — | NOT NULL |
| `file_size` | bigint | — | NOT NULL |
| `file_hash` | varchar(128) | — | — |
| `storage_path` | varchar(512) | — | UQ, NOT NULL |
| `status` | varchar(32) | — | NOT NULL |
| `created_at` | timestamptz | NOW() | NOT NULL |
| `updated_at` | timestamptz | NOW() | NOT NULL |


### `photo`

照片二进制（BYTEA）+ 元数据。


| 字段 | 类型 | 默认 | 约束 |
|---|---|---|---|
| `id` | uuid | gen_random_uuid() | PK |
| `content_type` | varchar(100) | — | NOT NULL |
| `original_filename` | varchar(255) | — | — |
| `size_bytes` | bigint | — | NOT NULL |
| `data` | bytea | — | NOT NULL |
| `uploader_id` | bigint | — | FK→app_user.id |
| `created_at` | timestamptz | NOW() | NOT NULL |


## AI 模块（V37–V45）

> 审计 / 病历识别历史 / RAG 同意 / 死信队列 / AI 问诊会话


### `ai_audit_log`

AI 调用审计：feature / model / tokens / 延迟 / 状态 / trace_id。


| 字段 | 类型 | 默认 | 约束 |
|---|---|---|---|
| `id` | bigint | AUTO | PK |
| `user_id` | bigint | — | — |
| `feature` | varchar(32) | — | NOT NULL |
| `model` | varchar(64) | — | — |
| `prompt_excerpt` | text | — | — |
| `response_excerpt` | text | — | — |
| `tokens_in` | integer | — | — |
| `tokens_out` | integer | — | — |
| `latency_ms` | integer | — | — |
| `status` | varchar(16) | — | NOT NULL |
| `error_msg` | text | — | — |
| `trace_id` | varchar(64) | — | — |
| `created_at` | timestamptz | NOW() | NOT NULL |


### `ai_extraction_history`

病历 AI 识别历史（每次 OCR 一行）。


| 字段 | 类型 | 默认 | 约束 |
|---|---|---|---|
| `id` | bigint | AUTO | PK |
| `source_type` | varchar(32) | — | NOT NULL |
| `source_id` | bigint | — | — |
| `photo_id` | uuid | — | — |
| `operator_id` | bigint | — | — |
| `model` | varchar(64) | — | NOT NULL |
| `raw_json` | jsonb | — | NOT NULL |
| `confidence` | numeric | — | — |
| `tokens_in` | integer | — | — |
| `tokens_out` | integer | — | — |
| `latency_ms` | integer | — | — |
| `status` | varchar(16) | — | NOT NULL |
| `error_msg` | text | — | — |
| `created_at` | timestamptz | NOW() | NOT NULL |


### `ai_dead_letter`

嵌入 / 抽取 / 聊天失败的死信队列。


| 字段 | 类型 | 默认 | 约束 |
|---|---|---|---|
| `id` | bigint | AUTO | PK |
| `kind` | varchar(32) | — | NOT NULL |
| `source_type` | varchar(32) | — | — |
| `source_id` | bigint | — | — |
| `patient_id` | bigint | — | — |
| `payload` | jsonb | — | NOT NULL |
| `error_msg` | text | — | — |
| `retry_count` | integer | 0 | NOT NULL |
| `last_attempt_at` | timestamptz | NOW() | NOT NULL |
| `resolved_at` | timestamptz | — | — |
| `created_at` | timestamptz | NOW() | NOT NULL |


### `ai_consult_session`

社区 AI 问诊会话。


| 字段 | 类型 | 默认 | 约束 |
|---|---|---|---|
| `id` | bigint | AUTO | PK |
| `user_id` | bigint | — | FK→app_user.id, NOT NULL |
| `title` | varchar(200) | — | — |
| `created_at` | timestamptz | NOW() | NOT NULL |
| `updated_at` | timestamptz | NOW() | NOT NULL |


### `ai_consult_message`

社区 AI 问诊单条消息。


| 字段 | 类型 | 默认 | 约束 |
|---|---|---|---|
| `id` | bigint | AUTO | PK |
| `session_id` | bigint | — | FK→ai_consult_session.id, NOT NULL |
| `role` | varchar(16) | — | NOT NULL |
| `content` | text | — | NOT NULL |
| `tokens_in` | integer | — | — |
| `tokens_out` | integer | — | — |
| `model` | varchar(64) | — | — |
| `status` | varchar(16) | 'completed' | NOT NULL |
| `error_msg` | text | — | — |
| `created_at` | timestamptz | NOW() | NOT NULL |
