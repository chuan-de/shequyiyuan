# shequyiyuan workspace

This repository is now organized as a two-app workspace:

- `server/`: backend code (legacy code and Spring Boot 3 rewrite)
- `web/`: Next.js frontend scaffold

## Layout

- `server/legacy/`: original Spring Boot + Vue2 bundled project moved from root
- `server/backend-rewrite/`: Spring Boot 3 rewrite project (`com.hospital`)
- `web/`: independent Next.js app for frontend rewrite

## 本地启动

1. 启动 PostgreSQL：

```bash
docker compose up -d postgres
```

2. 启动后端（Spring Boot）：

```bash
cd server/backend-rewrite && mvn spring-boot:run
```

3. 启动前端（Next.js）：

```bash
cd web && npm install && npm run dev
```

### 前端环境变量示例

在 `web/.env.local` 中配置：

```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

### 接口验证命令

1. 健康检查：

```bash
curl http://localhost:8080/api/v1/health
```

2. 注册接口：

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Password123!",
    "name": "Test User"
  }'
```

3. 登录接口：

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Password123!"
  }'
```

4. 获取当前用户 `/api/v1/auth/me`（将 `<JWT_TOKEN>` 替换为登录接口返回的 token）：

```bash
curl http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

### 排错说明

- **403 / CORS 问题**：检查后端 CORS 白名单是否包含前端地址（如 `http://localhost:3000`），并确认前端 `NEXT_PUBLIC_API_BASE_URL` 指向正确后端地址。
- **Postgres 未启动**：执行 `docker compose ps` 确认 `postgres` 服务状态，必要时重新执行 `docker compose up -d postgres`。
- **Flyway migration 失败**：查看后端启动日志与数据库连接配置，确认数据库可连接且迁移脚本版本连续、未重复执行。
- **JWT secret 过短**：检查后端 JWT 配置，确保 secret 长度满足签名算法要求（建议至少 32 字节）。

## 团队前端规范（补充）

- `web/components/ui/` 与 `web/components/layout/` 组件文件统一使用 **小写 kebab-case** 文件名。
- **导入路径大小写必须与文件名完全一致**（包括别名路径 `@/components/...`）。
- 禁止在仓库中并存仅大小写不同的重复组件文件。

## Legacy 核心功能迁移对账方案

## 数据库中英文命名迁移实施策略

为降低一次性改库风险，数据库命名迁移建议分阶段执行，并与外部依赖联动治理：

1. **第一阶段：仅在代码层英文化**
   - Entity / DTO 字段先采用英文命名。
   - 通过 ORM 注解映射旧表名、旧列名，保持数据库物理结构不变。
   - 目标是在不改动线上库结构的前提下，先完成应用层可读性与规范化改造。

2. **第二阶段：通过 Flyway 进行物理重命名（按需）**
   - 新增 Flyway migration 脚本执行表名/列名重命名。
   - 必须同步提供对应回滚脚本，确保可回退。
   - 建议按模块分批次上线，避免大事务影响可用性。

3. **外部依赖影响评估**
   - 对报表系统、ETL/数据同步任务、运维脚本、BI 查询做全量排查。
   - 明确“直接依赖旧表旧列名”的对象清单与改造窗口。
   - 以灰度或双写/兼容视图方式降低联动断裂风险。

4. **重命名前置保障**
   - 每次执行数据库重命名前，先做数据快照备份。
   - 在预发/演练环境完成完整迁移演练（含回滚演练）。
   - 演练通过后再进入生产变更流程。

5. **文档同步更新**
   - 迁移完成后及时更新 ER 图。
   - 同步维护数据字典，标注旧字段到新字段映射与废弃时间点。

### 1) 行为对照清单（每个 legacy 核心功能都要有）

每个功能至少从以下四个维度记录“老系统行为”与“新系统目标行为”：

- **输入**：请求参数、字段格式、默认值、是否允许空值、分页与排序规则。
- **权限**：角色可见性、接口鉴权方式、越权访问返回码、数据范围限制。
- **输出**：字段结构、字段语义、状态码、错误码、排序稳定性、单位/格式（如时间与金额）。
- **边界条件**：空数据、极大数据量、重复提交、并发更新、异常参数、软删/失效数据。

建议以“功能 -> 场景 -> 预期行为”三层结构维护，便于后续自动化。

### 2) 抽样对账流程（老系统 vs 新系统同场景执行）

对每个业务域（如用户、医生、就诊、病历、字典）定义统一抽样步骤：

1. **确定样本池**：覆盖常规路径、异常路径、边界路径（建议 7:2:1）。
2. **固定前置条件**：同一时间窗口、同一数据快照、同一账号权限。
3. **双跑执行**：在老系统和新系统执行同一业务场景，记录请求与响应。
4. **结构化比对**：按字段级比对（忽略明确允许差异的字段，如 traceId、更新时间戳）。
5. **输出差异报告**：场景 ID、差异字段、差异值、影响范围、初步定级。

建议使用统一对账模板（CSV/JSON）沉淀，便于周度统计。

### 3) 不一致项分类规则

所有差异项必须归档到以下三类之一：

- **必须兼容**：影响核心业务正确性、财务/医疗口径、权限安全、关键流程可用性。
- **可优化变更**：不影响业务结论，但在展示格式、排序、提示文案上存在改进空间。
- **待业务确认**：存在历史灰度逻辑、文档缺失、口径争议，需产品/业务 owner 书面确认。

每个差异项要有：分类、责任人、截止日期、处理结论（修复/接受差异/延后）。

### 4) 对账结果沉淀为回归用例（纳入每周验收）

将“已确认场景 + 已确认预期”转成回归资产：

- **接口回归用例**：包含请求、预期响应、关键断言。
- **数据回归用例**：包含数据库快照校验、统计口径校验。
- **权限回归用例**：覆盖角色矩阵和越权场景。

执行机制建议：

- 每周固定验收窗口（例如每周三），执行全量高优先级 + 增量中低优先级回归。
- 对“本周新增差异”设置阻断门槛：必须兼容项未关闭不得发布。

### 5) 迁移看板（功能状态 + 差异状态 + 风险）

建议建立统一迁移看板，至少包含以下字段：

- **功能名称 / 模块**
- **功能状态**：未开始 / 对齐中 / 已完成 / 已发布
- **差异状态**：无差异 / 有差异处理中 / 待业务确认 / 已关闭
- **风险等级**：高 / 中 / 低（按业务影响 × 发生概率）
- **负责人**：研发、测试、业务 owner
- **目标日期** 与 **实际完成日期**

推荐在周会上按看板滚动更新：优先清理“高风险 + 必须兼容 + 临近上线”的差异项。
