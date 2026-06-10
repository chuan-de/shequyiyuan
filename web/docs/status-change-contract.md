# 状态变更请求契约（`PATCH /{module}/{id}/status`）

为避免前后端在状态变更请求体上继续分叉，前端统一使用：

```json
{ "targetStatus": "<MODULE_STATUS_ENUM>", "reason": "<optional>" }
```

> `reason` 目前仅 `family-doctors` 模块 DTO 预留（可选）。

## 后端 `*StatusChangeRequest` DTO 现状

所有已迁移模块均采用 `targetStatus` 字段，字段类型是各模块自己的枚举：

- `doctor`: `DoctorStatusChangeRequest(targetStatus: DoctorStatus)`
- `familyDoctor`: `FamilyDoctorStatusChangeRequest(targetStatus: FamilyDoctorStatus, reason?: String)`
- `visit`: `VisitStatusChangeRequest(targetStatus: VisitStatus)`
- `medication`: `MedicationStatusChangeRequest(targetStatus: MedicationStatus)`
- `config`: `SystemConfigStatusChangeRequest(targetStatus: ConfigStatus)`
- `medicalRecord`: `BingliStatusChangeRequest(targetStatus: BingliStatus)`
- `familyDoctorContract`: `ContractStatusChangeRequest(targetStatus: ContractStatus)`

## 前端统一映射规则（`enabled -> targetStatus`）

`web/lib/api.ts#changeEntityStatus()` 统一按模块路由映射：

- `/api/v1/doctors`: `true -> ACTIVE`, `false -> INACTIVE`
- `/api/v1/family-doctors`: `true -> ACTIVE`, `false -> SUSPENDED`
- `/api/v1/visits`: `true -> COMPLETED`, `false -> CANCELLED`
- `/api/v1/medications`: `true -> ENABLED`, `false -> DISABLED`
- `/api/v1/configs`: `true -> ENABLED`, `false -> DISABLED`
- `/api/v1/medical-records`: `true -> ACTIVE`, `false -> ARCHIVED`
- `/api/v1/family-doctor-contracts`: `true -> ACTIVE`, `false -> TERMINATED`

## 类型约束

`web/lib/api-contract.ts` 已新增：

- `StatusManagedRoute`
- 各模块状态联合类型（如 `DoctorStatus`、`VisitStatus` 等）
- `EntityStatusByRoute`
- `StatusChangeRequest<R>`

新增模块时要求：

1. 后端 `*StatusChangeRequest` 必须使用 `targetStatus`。
2. 在 `api-contract.ts` 增加该模块状态枚举联合类型。
3. 在 `api.ts` 的 `statusChangeByRoute` 补齐映射。
4. 更新本文档，记录映射语义。
