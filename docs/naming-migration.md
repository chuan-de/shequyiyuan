# 命名迁移规范（拼音 → 英文）

> 目的：统一历史拼音命名到英文命名，降低跨团队沟通成本，并避免“看似直译但业务语义错误”的命名。

## 1. 英文命名风格（强制）

- **类名（Entity / Controller / Service / DTO）**：`PascalCase`
  - 示例：`VisitRecordService`、`HealthRecordController`
- **变量名 / 方法参数名**：`camelCase`
  - 示例：`visitDate`、`doctorId`
- **URL 路径 / 路由段**：`kebab-case`
  - 示例：`/health-records`、`/family-doctors`
- **数据库表名 / 字段名**：`snake_case`
  - 示例：`visit_record`、`doctor_id`

---

## 2. 拼音 → 英文映射总表（含业务语义）

> 说明：以下映射适用于实体、控制器、服务、DTO、路由、数据库字段。若同一词在不同上下文有歧义，优先采用“业务语义说明”列定义。

| 拼音 | 推荐英文 | 业务语义说明（防止直译错误） |
| --- | --- | --- |
| `jiuzhen` | `visit` | 指患者一次就医/接诊事件。默认用 **Visit**；仅在强调“临床接触事件”并需与住院 encounter 模型对齐时可用 `encounter`。 |
| `yisheng` | `doctor` | 医生主体，包含执业信息和账号信息。 |
| `yaopin` | `medication` | 药品目录与药品信息，优先用 `medication` 而不是过宽泛的 `drug`。 |
| `jiuankangdangan` | `healthRecord` | 居民/患者健康档案，强调长期档案属性。 |
| `jiatingyisheng` | `familyDoctor` | 家庭医生签约与服务关系，不等同于普通 doctor。 |
| `keshi` | `department` | 医院科室（内科、外科等）组织维度。 |
| `bingli` | `medicalRecord` | 病历文本与诊疗记录，不等同于健康档案。 |
| `guahao` | `registration` | 挂号/预约登记动作；若强调排班预约可扩展为 `appointmentRegistration`。 |
| `zhenduan` | `diagnosis` | 医生诊断结论，可含主诊断/次诊断。 |
| `chufang` | `prescription` | 处方单及处方明细。 |
| `jiancha` | `examination` | 检查项目（影像、检验等），非泛化“检查动作”时优先此词。 |
| `jianyan` | `labTest` | 实验室检验，和 `examination` 区分。 |
| `zhiliao` | `treatment` | 治疗过程、方案、执行记录。 |
| `shoufei` | `billing` | 收费与计费行为；财务结算上下文可用 `settlement`。 |
| `feiyong` | `fee` | 单项费用金额字段，不与总账 `billing` 混用。 |
| `qita` | `notes` | 备注/其他说明，避免语义空洞的 `other`。 |
| `leixing` / `types` | `type` | 业务分类枚举值。 |
| `zhuangtai` | `status` | 状态机状态（有效、作废、完成等）。 |
| `riqi` / `shijian` | `date` / `time` | 日期与时间分离建模；时间戳统一 `*_at`。 |

---

## 3. 分层命名落地规则

### 3.1 实体（Entity）
- 采用单数名词：`Visit`, `Doctor`, `HealthRecord`。
- 禁止直接使用拼音实体名，如 `Jiuzhen`, `Yisheng`。

### 3.2 控制器（Controller）
- 命名：`<Domain>Controller`，如 `VisitController`。
- 路由资源名用复数 kebab-case：`/visits`, `/health-records`。

### 3.3 服务（Service）
- 命名：`<Domain>Service`，如 `VisitService`。
- 领域动作优先语义动词：`createVisit`, `closeVisit`, `assignFamilyDoctor`。

### 3.4 DTO
- 命名：`<Domain><Action>Request/Response`，如 `VisitCreateRequest`。
- 字段禁止拼音：用 `doctorId`，不用 `yishengId`。

### 3.5 路由
- 禁止新增拼音路径：如 `/jiuzhen`, `/yaopin`。
- 新路径必须为英文语义复数：`/visits`, `/medications`。

### 3.6 数据库（表/字段）
- 表名 snake_case + 单数或复数保持库内一致（建议新表用复数）：`visits`, `health_records`。
- 外键统一：`<target>_id`，如 `doctor_id`。
- 时间字段统一：`created_at`, `updated_at`, `deleted_at`。

---

## 4. PR 必检项（强制）

后续所有 PR 必须包含以下检查，**任一不满足不得合并**：

- [ ] 无新增拼音命名（类名、变量、DTO、路由、表名、字段名）。
- [ ] 新增/修改领域词汇符合本规范映射表与业务语义。
- [ ] 命名风格符合：类名 PascalCase、变量 camelCase、路径 kebab-case、数据库 snake_case。
- [ ] 若涉及历史拼音兼容（如旧接口 `/jiuzhen`），PR 描述中给出英文别名方案与迁移计划。

建议在 CI 增加文本扫描（关键拼音词黑名单）与代码评审模板联动，防止拼音命名回流。
