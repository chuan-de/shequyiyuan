# Legacy 菜单到 Next.js 页面迁移矩阵

| legacy tableName | legacy route | legacy menu | next route | 业务域 |
|---|---|---|---|---|
| yaopin | /yaopin | 药品管理 | /medications | medications |
| jiatingyisheng | /jiatingyisheng | 家庭医生管理 | /family-doctors | family-doctors |
| jiuzhen | /jiuzhen | 就诊信息管理 | /visits | visits |
| bingli | /bingli | 病例信息管理 | /medical-records | medical-records |
| jiuankangdangan | /jiuankangdangan | 健康档案管理 | /health-records | health-records |
| yisheng | /yisheng | 医生管理 | /doctors | doctors |
| qiantai/config | /qiantai,/config | 前台/配置管理 | /configs | configs |
| dictionary* | /dictionary* | 基础数据管理 | /dictionaries | dictionaries |

来源：`server/legacy/src/main/resources/admin/admin/src/utils/menu.js` 与 `server/legacy/src/main/resources/admin/admin/src/router/router-static.js`.
