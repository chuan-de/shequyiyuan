"""Generate db-schema.md from the running Postgres (shequyiyuan-postgres-1)."""
import subprocess, sys, collections

DOCKER = ["docker", "exec", "shequyiyuan-postgres-1",
          "psql", "-U", "hospital", "-d", "hospital", "-t", "-A", "-F|", "-c"]

GROUPS = [
    ("认证 / RBAC", "用户、角色、权限分配（V1–V14）",
     ["app_user", "app_role", "app_user_role", "app_permission",
      "app_role_permission", "app_legacy_permission_mapping"]),
    ("业务主体", "患者、医生、前台、家庭医生、科室",
     ["patient_profile", "doctor_profile", "family_doctor_profile",
      "reception_profile", "department"]),
    ("药品", "药品台账 + 库存流水",
     ["medication", "medication_inventory_log"]),
    ("业务记录", "就诊 / 病历 / 健康档案",
     ["visit_record", "medical_record", "health_record"]),
    ("字典 / 系统配置", "数据字典 + 配置项",
     ["dictionary_item", "dictionary_operation_log", "system_config"]),
    ("文件 / 媒体", "通用文件元数据 + 照片 BYTEA",
     ["file_metadata", "photo"]),
    ("AI 模块（V37–V45）", "审计 / 病历识别历史 / RAG 同意 / 死信队列 / AI 问诊会话",
     ["ai_audit_log", "ai_extraction_history", "ai_dead_letter",
      "ai_consult_session", "ai_consult_message"]),
]

TABLE_COMMENTS = {
    "app_user": "登录账号（含 admin / 患者 / 医生等所有角色用户）。",
    "app_role": "角色字典：ADMIN / USER / PATIENT / RECEPTION / DOCTOR / FAMILY_DOCTOR。",
    "app_user_role": "用户—角色多对多关联。",
    "app_permission": "细粒度权限码（如 `ai:vision`、`patients:write`）。",
    "app_role_permission": "角色—权限多对多关联。",
    "app_legacy_permission_mapping": "legacy 菜单/接口 → 新权限码的兼容映射表。",
    "patient_profile": "患者档案。`ai_consent_at` 为 RAG 授权时间戳。",
    "doctor_profile": "医生档案，含工号、身份证等敏感字段。",
    "family_doctor_profile": "家庭医生档案。",
    "reception_profile": "前台档案。",
    "department": "科室定义（独立于字典）。",
    "medication": "药品台账。`code` 自动生成 `YP+yyyyMMddHHmmss+3位`。",
    "medication_inventory_log": "药品库存变动流水。",
    "visit_record": "就诊记录。`visit_number` 自动生成 `JZ+...`。",
    "medical_record": "病历记录。`case_number` 自动生成 `BL+...`；`prescription_items` / `attachments` 是 JSONB；`ai_extracted` 存 Vision OCR 抽取的结构化字段。",
    "health_record": "健康档案。",
    "dictionary_item": "数据字典条目。",
    "dictionary_operation_log": "字典变更审计日志。",
    "system_config": "系统级配置 key-value。",
    "file_metadata": "通用文件元数据（按 business_type 关联业务）。",
    "photo": "照片二进制（BYTEA）+ 元数据。",
    "ai_audit_log": "AI 调用审计：feature / model / tokens / 延迟 / 状态 / trace_id。",
    "ai_extraction_history": "病历 AI 识别历史（每次 OCR 一行）。",
    "ai_dead_letter": "嵌入 / 抽取 / 聊天失败的死信队列。",
    "ai_consult_session": "社区 AI 问诊会话。",
    "ai_consult_message": "社区 AI 问诊单条消息。",
}


def run(sql):
    return subprocess.check_output(DOCKER + [sql], text=True)


def fetch_columns():
    sql = ("SELECT c.table_name, c.column_name, c.data_type, "
           "c.character_maximum_length, c.numeric_precision, c.numeric_scale, "
           "c.is_nullable, c.column_default "
           "FROM information_schema.columns c "
           "WHERE c.table_schema='public' AND c.table_name NOT IN ('flyway_schema_history') "
           "ORDER BY c.table_name, c.ordinal_position;")
    cols = collections.defaultdict(list)
    for line in run(sql).strip().splitlines():
        t, col, dt, clen, nprec, nscale, nullable, default = line.split("|")
        cols[t].append((col, dt, clen, nprec, nscale, nullable == "YES", default))
    return cols


def fetch_constraints():
    sql = ("SELECT tc.table_name, tc.constraint_type, kcu.column_name, "
           "COALESCE(ccu.table_name||'.'||ccu.column_name, '') "
           "FROM information_schema.table_constraints tc "
           "JOIN information_schema.key_column_usage kcu ON tc.constraint_name=kcu.constraint_name "
           "LEFT JOIN information_schema.constraint_column_usage ccu "
           "  ON tc.constraint_name=ccu.constraint_name AND tc.constraint_type='FOREIGN KEY' "
           "WHERE tc.table_schema='public' "
           "AND tc.constraint_type IN ('PRIMARY KEY','FOREIGN KEY','UNIQUE') "
           "ORDER BY tc.table_name, tc.constraint_type, kcu.ordinal_position;")
    pks, fks, uqs = collections.defaultdict(set), collections.defaultdict(dict), collections.defaultdict(set)
    for line in run(sql).strip().splitlines():
        t, ctype, col, ref = line.split("|")
        if ctype == "PRIMARY KEY":
            pks[t].add(col)
        elif ctype == "FOREIGN KEY":
            fks[t][col] = ref
        else:
            uqs[t].add(col)
    return pks, fks, uqs


def fmt_type(dt, clen, nprec, nscale):
    m = {
        "bigint": "bigint", "integer": "integer", "boolean": "boolean",
        "text": "text", "jsonb": "jsonb", "bytea": "bytea", "uuid": "uuid",
        "numeric": "numeric",
        "timestamp with time zone": "timestamptz",
        "character varying": f"varchar({clen})" if clen else "varchar",
    }
    return m.get(dt, dt)


def fmt_default(d):
    if not d:
        return ""
    if d.startswith("nextval("):
        return "AUTO"
    if d == "now()":
        return "NOW()"
    return d.split("::")[0]


def attrs(table, col, nullable, pks, fks, uqs):
    out = []
    if col in pks[table]:
        out.append("PK")
    if col in fks[table]:
        out.append(f"FK→{fks[table][col]}")
    if col in uqs[table]:
        out.append("UQ")
    if not nullable and col not in pks[table]:
        out.append("NOT NULL")
    return ", ".join(out)


def main():
    cols = fetch_columns()
    pks, fks, uqs = fetch_constraints()

    lines = ["# 数据库表结构\n",
             f"> 自动生成于 `scripts/dump_schema.py`。基于当前 Postgres 实例（Flyway V1–V45）。\n",
             f"> 共 {sum(len(t) for _, _, t in GROUPS)} 张业务表（不含 `flyway_schema_history`）。\n",
             "",
             "## 目录\n"]
    for name, _, tables in GROUPS:
        lines.append(f"- [{name}](#{name.replace(' ', '-').replace('/', '').lower()})")
        for t in tables:
            lines.append(f"  - [`{t}`](#{t})")
    lines.append("\n---\n")

    for name, desc, tables in GROUPS:
        lines.append(f"\n## {name}\n")
        lines.append(f"> {desc}\n")
        for t in tables:
            if t not in cols:
                continue
            lines.append(f"\n### `{t}`")
            if t in TABLE_COMMENTS:
                lines.append(f"\n{TABLE_COMMENTS[t]}\n")
            lines.append("\n| 字段 | 类型 | 默认 | 约束 |")
            lines.append("|---|---|---|---|")
            for col, dt, clen, nprec, nscale, nullable, default in cols[t]:
                ty = fmt_type(dt, clen, nprec, nscale)
                a = attrs(t, col, nullable, pks, fks, uqs)
                d = fmt_default(default)
                lines.append(f"| `{col}` | {ty} | {d or '—'} | {a or '—'} |")
            lines.append("")

    out = "\n".join(lines).rstrip() + "\n"
    with open("db-schema.md", "w", encoding="utf-8") as f:
        f.write(out)
    print(f"wrote db-schema.md ({len(out)} chars, {len([l for l in lines if l.startswith('### ')])} tables)")


if __name__ == "__main__":
    sys.exit(main())
