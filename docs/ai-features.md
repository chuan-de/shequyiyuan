# AI 功能使用文档

本文档面向**部署运维**和**最终用户**，覆盖 AI 模块的部署、配置、使用与排查。技术实施细节见 [ai-features-plan.md](./ai-features-plan.md)。

---

## 一、能力概览

| 功能 | 入口 | 谁能用 |
|------|------|------|
| 病历 AI 识别 | 病历编辑页 → 附件旁"AI 识别"按钮 | 医生、管理员 |
| 患者私域 AI 问询 | 患者管理 → 患者详情页 → 右侧"AI 问询"侧栏 | 医生、患者本人、管理员 |
| 社区 AI 问诊 | 左侧导航 → "社区 AI 问诊" | 患者、医生、管理员 |

所有 AI 功能均使用**火山引擎方舟（Doubao）**：
- 视觉识别：`doubao-seed-2.0-lite`
- 文本嵌入：`doubao-embedding-text-240715`
- 聊天：`doubao-seed-2.0-lite`

---

## 二、部署前置

### 数据库镜像
**必须**使用 `pgvector/pgvector:pg16`（已预装 pgvector 扩展）。`docker-compose.yml` 已配置；自建环境请确认。

### 环境变量
在所有运行环境（开发/测试/生产）设置：

```bash
export HOSPITAL_AI_API_KEY=ark-xxxxxxxx
```

申请：火山引擎方舟控制台 → API Key 管理。

**严禁**把 key 写到 `application.yml` 或 commit 到仓库。

### 功能开关
`application.yml` 默认全开。如需关闭：

```yaml
hospital:
  ai:
    enabled: false                  # 完全禁用（不装配任何 AI bean）
    features:
      vision: false                 # 仅关病历识别
      patient-rag: false            # 仅关 RAG 问询
      consult: false                # 仅关社区问诊
```

灰度场景：可在测试环境关掉某些 feature 单独验证。

---

## 三、限流与配额

`application.yml` 中：

```yaml
hospital:
  ai:
    rate-limit:
      per-user-qpm: 10              # 每用户每分钟最多 10 次调用
      daily-token-budget: 100000    # 每用户每天最多 10 万 token
```

超限返回 HTTP **429** + 提示。审计表 `ai_audit_log` 的 `status = rate_limited` 行表示被限流。

调整生产值时按真实负载校准。

---

## 四、监控指标

通过 `/actuator/prometheus` 暴露：

| 指标 | 类型 | 标签 | 说明 |
|------|------|------|------|
| `hospital_ai_calls_total` | Counter | feature, model, status | 每次 AI 调用 +1，status=success/failed/rate_limited |
| `hospital_ai_latency_seconds` | Timer | feature, model | 成功调用的端到端耗时 |
| `hospital_ai_tokens_total` | Counter | feature, model, direction(in/out) | token 消耗，用于成本核算 |

**建议告警**：
- `rate(hospital_ai_calls_total{status="failed"}[5m]) > 0.1` — 失败率超 10%
- `histogram_quantile(0.95, hospital_ai_latency_seconds_bucket) > 30` — P95 延迟 > 30s
- 月度 token 用量看板（按 feature/model 切分）

---

## 五、隐私与合规

### 患者授权（Feature 2）
患者 RAG 首次使用前**必须**获得患者授权（弹同意书 → 同意 → `patient_profile.ai_consent_at` 落库）。未授权时接口返回 HTTP **412** + `code: AI_CONSENT_REQUIRED`，前端引导授权流程。

如需撤回授权：当前版本仅支持联系管理员手动清空 `ai_consent_at`，后续版本将提供患者自助撤回。

### 审计表
`ai_audit_log` 记录每次 AI 调用（user_id、feature、model、prompt 摘要 500 字、response 摘要 500 字、tokens、延迟、状态、trace_id）。**建议保留 ≥ 6 个月**。

### 数据出境
所有调用走火山引擎方舟（境内），不涉及跨境。如需替换为自部署模型，仅需替换 `AiCallTemplate` 的 `base-url` 与适配实现，业务代码无需改动。

### 合规声明
社区 AI 问诊页每次会话开头展示"AI 仅供参考，不能替代专业医疗诊断"折叠条。**该文案上线前请过法务**。

---

## 六、用户使用指南

### 病历 AI 识别
1. 进入「病历管理」→ 新建或编辑某条病历
2. 在附件区上传病历图片（JPEG/PNG/GIF/WebP/PDF，≤ 10 MB）
3. 点击图片旁的「AI 识别」按钮
4. 等待约 10 秒，弹出右侧建议值面板
5. 勾选要采纳的字段 → 点「填入表单」
6. 按需手动调整 → 保存

### 患者 AI 问询
1. 进入「患者管理」→ 点击某患者「AI 问询」操作（或进入详情页）
2. **首次使用**：弹出同意书，确认获得患者授权后点「同意」
3. 在右侧聊天面板提问，如：
   - "他最近一次诊断是什么？"
   - "他有什么药物过敏史？"
   - "最近 3 次就诊都是什么科室？"
4. AI 回答下方显示 `[#1] [#2]` 引用脚注，点击查看原片段

### 社区 AI 问诊
1. 点击左侧导航「社区 AI 问诊」
2. 点「新对话」或选择已有 session
3. 输入症状描述（如 "我最近老头疼"），按 Enter 发送（Shift+Enter 换行）
4. AI 流式返回建议
5. 可继续追问（多轮上下文自动维护）
6. 左侧可重命名、删除会话

**注意**：
- 提问与健康无关的话题（如天气、股票、写代码）会被礼貌拒绝
- 单次提问不超过 2000 字
- AI 答案仅供参考，**不能替代专业诊疗**

---

## 七、运维排查

| 现象 | 可能原因 | 处理 |
|------|---------|------|
| 启动报 `HOSPITAL_AI_API_KEY` 未设置 | 环境变量缺失 | 设置环境变量后重启；或临时设 `hospital.ai.enabled=false` |
| 调用返回 429 | 超 QPM 或 token 预算 | 查 `ai_audit_log` 的 `status=rate_limited` 行；调高 `rate-limit.*` |
| 调用返回 412 | 未授权 | 让患者完成同意书流程 |
| 调用返回 500 + 上游错误 | 火山引擎方舟侧 5xx/网络 | 查 `ai_audit_log.error_msg`；已自动重试 3 次仍失败 |
| 嵌入队列堆积 | 异步事件处理慢 | 查 `ai_dead_letter` 表；用 `POST /api/v1/ai/admin/backfill?dryRun=true` 预估 |
| SSE 流式被截断 | Nginx 缓冲 | 确认上游已设 `proxy_buffering off` + 后端 `X-Accel-Buffering: no` 已生效 |
| 启动时 V40 报 `extension "vector" does not exist` | 用了非 pgvector 镜像 | 切到 `pgvector/pgvector:pg16` |

### 历史数据回填
启用 RAG 后，历史病历/档案/就诊默认**不会**自动嵌入。需触发回填：

```bash
# 预估（不实际写库）
curl -X POST 'http://host/api/v1/ai/admin/backfill?dryRun=true' \
  -H "Authorization: Bearer <admin-token>"

# 真实回填
curl -X POST 'http://host/api/v1/ai/admin/backfill?dryRun=false' \
  -H "Authorization: Bearer <admin-token>"
```

幂等，重跑安全。批次间限速 200ms，大库可能要数小时。

---

## 八、已知限制与后续计划

| 项 | 现状 | 后续 |
|---|---|---|
| 流式审计粒度 | 总延迟 + 总 tokens | 加首字节延迟、流式中断率 |
| Guardrail 词表 | 静态文件 | 支持热加载 + 管理后台编辑 |
| 患者本人 RAG 入口 | 暂未提供（医生端可代问） | 增加患者端门户 |
| 集成测试 | 需 Docker，本地 CI 未跑 | 接入 GitHub Actions Linux runner |
| Token 估算 | 中文按 1:1 粗估 | 引入 tokenizer 精确计算 |
| 跨设备会话同步 | 不支持（按需求） | — |
| 病历回填 cron | 无（仅手动触发） | 按需添加定时任务 |

---

## 九、相关 Flyway 版本

| 版本 | 内容 |
|------|------|
| V37 | AI 权限码 + 角色分配 |
| V38 | `ai_audit_log` 表 |
| V39 | `medical_record.ai_extracted` + `ai_extraction_history` |
| V40 | pgvector 扩展 |
| V41 | `patient_knowledge_chunk` + HNSW 索引 |
| V42 | `patient_profile.ai_consent_at` + `ai_dead_letter` |
| V43 | `ai_consult_session` + `ai_consult_message` |

---

## 十、参考链接

- [实施计划与设计](./ai-features-plan.md)
- [迁移状态总表](./migration-status.md)
- 火山引擎方舟 API 文档：https://www.volcengine.com/docs/82379
