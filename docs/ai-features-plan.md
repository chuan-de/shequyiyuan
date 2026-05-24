# AI 功能集成方案 — 实施计划

> 状态：✅ 已立项，待实施
> 模型供应商：火山引擎方舟（Doubao）
> 端点：`https://ark.cn-beijing.volces.com/api/v3`（OpenAI 兼容）

---

## 一、决策快照

| # | 议题 | 决定 |
|---|---|---|
| 1 | AI 问诊对象 | 患者 + 医生都用 |
| 2 | 联网搜索 | 不开 |
| 3 | 会话跨设备 | 不强制（仍存后端便于审计） |
| 4 | 导出对话到病历 | 不做 |
| 5 | 多语言 | 仅中文 |
| 6 | 图片来源 | PC + 手机拍照都支持 |
| 7 | 实施顺序 | Feature 1 → 2 → 3 |
| 8 | MVP 范围 | 三个全做齐 |
| 9 | 域外问题 | guardrail 婉拒（不调 LLM） |
| 10 | 特殊病种 | 不屏蔽 |

---

## 二、三大功能概述

### Feature 1：病历 AI 识别（Vision OCR）
- 触发：医生在病历表单点 "AI 识别" 按钮
- 模型：`doubao-seed-2.0-lite`（已验证：660KB 病历图 10s 出结构化 JSON）
- 调用：同步（用户能等）
- 输出：字段建议值，医生审阅后勾选填入

### Feature 2：患者私域 RAG
- 触发：医生/患者在患者详情页 "AI 问询" 侧栏问问题
- 数据：患者历次 medical_record / health_record / visit 切片向量化
- 隐私：`WHERE patient_id = ?` 强过滤；首次使用需患者授权
- 输出：答案 + 引用脚注（可点回原病历）

### Feature 3：社区 AI 问诊（聊天页）
- 路由：`/ai-consult`
- 多轮对话 + SSE 流式
- guardrail：域外问题婉拒
- 会话存后端但不跨设备同步

---

## 三、模型 / API 选型

| 用途 | 模型 ID | 备注 |
|------|---------|------|
| 视觉识别 | `doubao-seed-2.0-lite` | Phase 1 + 测试已验证 |
| 文本嵌入 | `doubao-embedding-text-240715` | Phase 2 |
| 聊天 | `doubao-seed-2.0-lite` | Phase 3 默认；后续可切 pro |

**注意**：火山方舟要求 `temperature` 固定为 1，调用时不要传别的值。

---

## 四、技术栈

- **向量库**：pgvector（PostgreSQL 扩展，需要切到 `pgvector/pgvector:pg16` 镜像）
- **框架**：Spring AI 1.0.x
  - `spring-ai-starter-model-openai`（用 OpenAI 协议调火山）
  - `spring-ai-starter-vector-store-pgvector`
- **限流**：Bucket4j
- **流式**：Spring WebFlux SSE

---

## 五、跨切面基础约定

### 权限码（V37 引入）
| 权限码 | 名称 | 角色分配 |
|---|---|---|
| `ai:vision` | AI 病历识别 | DOCTOR + ADMIN |
| `ai:patient-rag` | 患者 AI 问询 | DOCTOR + PATIENT + ADMIN |
| `ai:consult` | 社区 AI 问诊 | PATIENT + DOCTOR + ADMIN |
| `ai:admin` | AI 审计与配额管理 | ADMIN |

### 配置（`application.yml`）
```yaml
hospital:
  ai:
    enabled: true
    provider: doubao
    base-url: https://ark.cn-beijing.volces.com/api/v3
    api-key: ${HOSPITAL_AI_API_KEY:}
    chat-model: doubao-seed-2.0-lite
    embedding-model: doubao-embedding-text-240715
    vision-model: doubao-seed-2.0-lite
    timeout-seconds: 60
    features:
      vision: true
      patient-rag: true
      consult: true
    rate-limit:
      per-user-qpm: 10
      daily-token-budget: 100000
```

API key 从环境变量 `HOSPITAL_AI_API_KEY` 读，**不入仓库**。本地开发可临时写到 `application-local.yml`（已在 .gitignore 中通过 `application-local.yml` 规则排除——若未排除请补上）。

### 审计表 `ai_audit_log`
| 列 | 类型 | 说明 |
|---|---|---|
| id | BIGSERIAL PK | |
| user_id | BIGINT | 调用者 |
| feature | VARCHAR(32) | vision / patient-rag / consult |
| model | VARCHAR(64) | |
| prompt_excerpt | TEXT | 截断到 500 字 |
| response_excerpt | TEXT | 截断到 500 字 |
| tokens_in | INTEGER | |
| tokens_out | INTEGER | |
| latency_ms | INTEGER | |
| status | VARCHAR(16) | success / failed / rate_limited |
| error_msg | TEXT | nullable |
| trace_id | VARCHAR(64) | 链路追踪 |
| created_at | TIMESTAMPTZ NOT NULL DEFAULT NOW() | |

### API 路由约定
全部前缀 `/api/v1/ai/*`：
- `POST /api/v1/ai/vision/parse-medical-record`
- `POST /api/v1/ai/patient/{patientId}/ask`
- `POST /api/v1/ai/consult/sessions`
- `GET  /api/v1/ai/consult/sessions`
- `GET  /api/v1/ai/consult/sessions/{id}`
- `DELETE /api/v1/ai/consult/sessions/{id}`
- `POST /api/v1/ai/consult/sessions/{id}/messages`（SSE）

### Flyway 版本预留
| 版本 | 内容 | 所属 Phase |
|------|------|-----------|
| V37 | AI 权限码 + 角色分配 | Phase 0 |
| V38 | `ai_audit_log` 表 | Phase 0 |
| V39 | `medical_record.ai_extracted` JSONB + `ai_extraction_history` 表 | Phase 1 |
| V40 | `CREATE EXTENSION vector` | Phase 2 |
| V41 | `patient_knowledge_chunk` 表 + HNSW 索引 | Phase 2 |
| V42 | `patient_profile.ai_consent_at` + `ai_dead_letter` 表 | Phase 2 |
| V43 | `ai_consult_session` + `ai_consult_message` 表 | Phase 3 |

**严格按上述版本号使用，不得跳号或复用。**

---

## 六、Phase 分解与子任务

### Phase 0 — 跨切面基础设施（≈ 1.5-2 天）
> 必须先完成，所有后续 Phase 依赖

| ID | 任务 | 关键产物 |
|----|------|---------|
| 0.1 | pom 增加依赖 | Spring AI BOM + openai-starter + pgvector-starter + bucket4j |
| 0.2 | `application.yml` AI 配置块 + 环境变量 `HOSPITAL_AI_API_KEY` | `hospital.ai.*` |
| 0.3 | `com.hospital.ai` 模块骨架 + `AiProperties` 配置绑定 | |
| 0.4 | `AiCallTemplate`（统一 Doubao OpenAI 兼容客户端）| 一处管：超时/重试/429 退避/temperature=1 |
| 0.5 | Flyway V37 — 新权限码 + 角色分配 | |
| 0.6 | Flyway V38 — `ai_audit_log` 表 | |
| 0.7 | `AiAuditInterceptor` 切面 | 围绕 `AiCallTemplate` 自动落库 |
| 0.8 | Bucket4j 限流（每用户 QPM + 每日 token 预算）| |

**完成定义**：可以从单元测试调一次 `AiCallTemplate.chat(model="doubao-seed-2.0-lite", messages=[...])` 拿到响应，`ai_audit_log` 落一条记录。

### Phase 1 — 病历 AI 识别（≈ 2 天）

| ID | 任务 |
|----|------|
| 1.1 | Flyway V39 — `medical_record.ai_extracted JSONB` + `ai_extraction_history` 表 |
| 1.2 | `AiVisionService` 接口 + `DoubaoVisionService` 实现（入参 base64/photoId，出参 raw JSON + tokens） |
| 1.3 | `MedicalRecordExtractor`（Doubao JSON → `MedicalRecordFields` DTO，字段校验） |
| 1.4 | `AiVisionController` — `POST /api/v1/ai/vision/parse-medical-record`，权限 `ai:vision` |
| 1.5 | 前端 `lib/api.ts` + `lib/api-contract.ts` 类型 |
| 1.6 | 病历表单附件区"AI 识别"按钮 + 建议值审阅侧栏 |
| 1.7 | 集成测试（mock Doubao 客户端） |

**完成定义**：医生在病历编辑页选择附件 → 点 AI 识别 → 10s 内返回建议值 → 勾选填入 → 保存。

### Phase 2 — 患者私域 RAG（≈ 3-4 天）

| ID | 任务 |
|----|------|
| 2.1 | docker-compose 切换 PG 镜像到 `pgvector/pgvector:pg16` + Flyway V40 `CREATE EXTENSION vector` |
| 2.2 | Flyway V41 — `patient_knowledge_chunk` 表（含 1024 维向量）+ HNSW 索引 + `patient_id` 复合索引 |
| 2.3 | Flyway V42 — `patient_profile.ai_consent_at` + `ai_dead_letter` 表 |
| 2.4 | Spring AI `PgVectorStore` + `EmbeddingModel`（指向 Doubao）配置 |
| 2.5 | `MedicalRecordChunker` — 按字段切片（主诉/诊断/医嘱/检查各一 chunk） |
| 2.6 | `KnowledgeIngestionService` + 事件驱动（保存事件 → 异步嵌入 → 写 chunk；失败入 dead letter） |
| 2.7 | 历史数据回填 Job（一次性 + 幂等 + 限速） |
| 2.8 | `PatientRagService`（Retrieval + ChatClient，强制 patient_id 过滤，返回 answer + cited_chunks） |
| 2.9 | `PatientRagController` — `POST /api/v1/ai/patient/{patientId}/ask`，权限 + 行级校验（患者只能问自己） |
| 2.10 | 前端患者详情页 "AI 问询" 侧栏 + 引用气泡可点回原病历 |
| 2.11 | 患者授权同意书弹窗 + 落库 `ai_consent_at` |

**完成定义**：医生进入患者详情 → AI 问询侧栏 → "他最近一次诊断是什么" → 带引用脚注的答案。

### Phase 3 — 社区 AI 问诊（≈ 2-3 天）

| ID | 任务 |
|----|------|
| 3.1 | Flyway V43 — `ai_consult_session` + `ai_consult_message` 表 |
| 3.2 | `AiConsultService` 会话+消息 CRUD |
| 3.3 | 多轮上下文管理（滑窗截断超 32K，保留 system + 最近 N 轮） |
| 3.4 | Guardrail（域外问题/恶意输入 → 友好婉拒模板，不调 LLM） |
| 3.5 | SSE 流式端点 `POST /api/v1/ai/consult/sessions/{id}/messages` |
| 3.6 | Controller + DTO + 权限 `ai:consult`（患者只见自己 session） |
| 3.7 | 前端 `/ai-consult` 路由 + 页面骨架（左 session 列表 + 右消息流） |
| 3.8 | 前端 SSE 流式渲染 + typing 效果 |
| 3.9 | 合规声明组件（每个新会话开头展示"AI 仅供参考"折叠条） |
| 3.10 | 侧边栏增加 "社区 AI 问诊" 入口（按 `ai:consult` 权限显示） |

**完成定义**：登录用户进入 `/ai-consult` → 输入症状 → 流式返回建议 → 多轮追问 → 会话保留可查。

### Phase 4 — 收尾（≈ 1 天）

| ID | 任务 |
|----|------|
| 4.1 | 三个 Feature 的集成测试（VCR 录制） |
| 4.2 | Micrometer 指标：`ai_calls_total`、`ai_latency_seconds`、`ai_tokens_total` |
| 4.3 | 更新 `docs/migration-status.md` + 加 `docs/ai-features.md` 用户文档 |
| 4.4 | README / CLAUDE.md 增加 AI 模块说明 + 环境变量要求 |

---

## 七、PR 划分

| PR | 内容 | 可独立合并 |
|----|------|----------|
| PR-1 | Phase 0（AI 基础设施）| ✅ |
| PR-2 | Phase 1（Vision OCR） | ✅ 依赖 PR-1 |
| PR-3 | Phase 2.1-2.7（RAG 数据层） | ✅ 依赖 PR-1 |
| PR-4 | Phase 2.8-2.11（RAG 查询 + UI） | ✅ 依赖 PR-3 |
| PR-5 | Phase 3（AI 问诊） | ✅ 依赖 PR-1 |
| PR-6 | Phase 4（收尾） | ✅ 依赖前面所有 |

---

## 八、风险与缓解

| 风险 | 缓解 |
|------|------|
| 现有 `postgres:16` 镜像没装 pgvector | Phase 2.1 切到 `pgvector/pgvector:pg16` 镜像 |
| Doubao agentPlan 限流（429） | Phase 0 限流 + 重试已覆盖；生产用商用端点 `/api/v3` |
| SSE 被 Nginx 缓冲 | Phase 3.5 设置 `X-Accel-Buffering: no` |
| 历史病历 backfill 耗光配额 | Phase 2.7 加批次大小 + 限速 |
| 患者授权流程影响现有页面 | Phase 2.11 用拦截器懒触发 |
| 医疗数据出境合规 | 火山方舟在境内；保留切换自部署模型的抽象层 |

---

## 九、上线前检查清单

- [ ] `HOSPITAL_AI_API_KEY` 环境变量在所有部署环境配置
- [ ] `key` 文件已在 `.gitignore` 且从未 commit
- [ ] AI 审计日志保留策略：≥ 6 个月
- [ ] 患者授权同意书文案经法务确认
- [ ] AI 回答合规声明（"仅供参考"）在所有出口可见
- [ ] 限流配额按生产流量校准
- [ ] 监控告警接入（高错误率 / 高 latency / 配额超支）
