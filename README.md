# shequyiyuan workspace

## 开发入口说明（当前迭代）

> **唯一后端主线根目录：`server/backend-rewrite/`**
>
> 当前迭代后端开发、联调、缺陷修复与业务功能提交，统一在 `server/backend-rewrite/` 进行。

- **后端主线目录**：`server/backend-rewrite/`
- **前端主线目录**：`web/`
- **非主线目录**：`backend-rewrite/`（历史副本）、`server/legacy/`（遗留系统）

### 启动方式

1. 启动 PostgreSQL：

```bash
docker compose up -d postgres
```

2. 启动后端（主线）：

```bash
cd server/backend-rewrite && mvn spring-boot:run
```

3. 启动前端（主线）：

```bash
cd web && npm install && npm run dev
```

### 前端环境变量示例

在 `web/.env.local` 中配置：

```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

## 目录状态说明

- `server/backend-rewrite/`：✅ 当前唯一后端主线
- `web/`：✅ 当前前端主线
- `backend-rewrite/`：⚠️ deprecated（仅归档参考，不接收业务变更）
- `server/legacy/`：⚠️ legacy/archived（仅归档参考，不接收业务变更）

## 提交范围守卫（主线目录限制）

已增加路径校验脚本与 CI：

- 脚本：`scripts/check-mainline-paths.sh`
- CI：`.github/workflows/mainline-path-guard.yml`

规则：当提交包含 `backend-rewrite/` 或 `server/legacy/` 下的业务文件时将直接失败，避免业务成果继续分散到非主线目录。

## 并行任务归档核对（按主线目录）

为避免成果分散，本轮已完成归档核对并统一到主线目录约束：

1. 后端入口统一确认为 `server/backend-rewrite/`。
2. 根 README 明确了前后端主目录与启动方式。
3. 非主线目录已加醒目标识（deprecated / legacy / archived）。
4. 已添加 CI 路径守卫，限制业务变更落在非主线目录。
5. 并行任务结果已在本节与 CI 规则中完成“按主线目录”的再归档确认。
