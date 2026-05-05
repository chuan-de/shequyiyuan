# API 路由迁移公告（拼音 -> 英文）

发布日期：2026-05-05

## 医生模块迁移
- 新路由：`/doctor/**`
- 旧路由：`/yisheng/**`（兼容期保留）
- 旧路径废弃时间：`2026-12-31`

## 兼容策略
- 过渡期内同时支持新旧路由，建议调用方尽快迁移到英文路由。
- 过渡期结束后将移除旧路由别名，只保留新路由。

## 命名迁移
已将医生模块核心类型由拼音命名迁移为英文命名：
- `YishengController` -> `DoctorController`
- `YishengService` -> `DoctorService`
- `YishengServiceImpl` -> `DoctorServiceImpl`
- `YishengEntity` -> `DoctorEntity`
- `YishengModel` -> `DoctorModel`
- `YishengVO` -> `DoctorVO`
- `YishengView` -> `DoctorView`
- `YishengDao` -> `DoctorDao`
