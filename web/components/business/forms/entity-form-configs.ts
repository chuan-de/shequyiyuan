import { EntityPageConfig } from '@/components/business/entity-management-page';
import { API_ROUTES } from '@/lib/api-contract';

export const patientsPageConfig: EntityPageConfig = {
  title: '患者管理',
  route: API_ROUTES.patients,
  permissionPrefix: 'patients',
  columns: [
    { key: 'username', title: '账号' },
    { key: 'fullName', title: '姓名' },
    { key: 'phone', title: '手机号' },
    { key: 'idNumber', title: '身份证号' }
  ],
  formFields: [
    { key: 'username', label: '账号', required: true, placeholder: '请输入登录账号', defaultValue: '' },
    { key: 'password', label: '密码', required: true, placeholder: '至少6位', defaultValue: '' },
    { key: 'fullName', label: '姓名', required: true, placeholder: '请输入姓名', defaultValue: '' },
    { key: 'phone', label: '手机号', placeholder: '请输入手机号', defaultValue: '' },
    { key: 'idNumber', label: '身份证号', placeholder: '请输入身份证号', defaultValue: '' },
    { key: 'email', label: '邮箱', placeholder: '请输入邮箱', defaultValue: '' }
  ],
  statusField: 'enabled',
  createPayload: (form) => ({
    username: form.username,
    password: form.password,
    fullName: form.fullName,
    ...(form.phone ? { phone: form.phone } : {}),
    ...(form.idNumber ? { idNumber: form.idNumber } : {}),
    ...(form.email ? { email: form.email } : {})
  }),
  updatePayload: (form) => ({
    fullName: form.fullName,
    ...(form.phone ? { phone: form.phone } : {}),
    ...(form.idNumber ? { idNumber: form.idNumber } : {}),
    ...(form.email ? { email: form.email } : {})
  })
};

export const receptionsPageConfig: EntityPageConfig = {
  title: '前台管理',
  route: API_ROUTES.receptions,
  permissionPrefix: 'receptions',
  columns: [
    { key: 'username', title: '账号' },
    { key: 'fullName', title: '姓名' },
    { key: 'uuidNumber', title: '工号' },
    { key: 'phone', title: '手机号' }
  ],
  formFields: [
    { key: 'username', label: '账号', required: true, placeholder: '请输入登录账号', defaultValue: '' },
    { key: 'password', label: '密码', required: true, placeholder: '至少6位', defaultValue: '' },
    { key: 'fullName', label: '姓名', required: true, placeholder: '请输入姓名', defaultValue: '' },
    { key: 'uuidNumber', label: '工号', placeholder: '请输入工号', defaultValue: '' },
    { key: 'phone', label: '手机号', placeholder: '请输入手机号', defaultValue: '' },
    { key: 'email', label: '邮箱', placeholder: '请输入邮箱', defaultValue: '' }
  ],
  statusField: 'enabled',
  createPayload: (form) => ({
    username: form.username,
    password: form.password,
    fullName: form.fullName,
    ...(form.uuidNumber ? { uuidNumber: form.uuidNumber } : {}),
    ...(form.phone ? { phone: form.phone } : {}),
    ...(form.email ? { email: form.email } : {})
  }),
  updatePayload: (form) => ({
    fullName: form.fullName,
    ...(form.uuidNumber ? { uuidNumber: form.uuidNumber } : {}),
    ...(form.phone ? { phone: form.phone } : {}),
    ...(form.email ? { email: form.email } : {})
  })
};

export const medicationsPageConfig: EntityPageConfig = {
  title: 'medications',
  route: API_ROUTES.medications,
  permissionPrefix: 'medications',
  columns: [{ key: 'code', title: '编码' }, { key: 'name', title: '名称' }],
  formFields: [
    { key: 'code', label: '编码', required: true, placeholder: '请输入编码', defaultValue: '' },
    { key: 'name', label: '名称', required: true, placeholder: '请输入名称', defaultValue: '' }
  ],
  statusField: 'enabled',
  createPayload: (form) => ({ code: form.code, name: form.name }),
  updatePayload: (form) => ({ code: form.code, name: form.name })
};

export const doctorPageConfig: EntityPageConfig = {
  title: 'doctors',
  route: API_ROUTES.doctors,
  permissionPrefix: 'doctors',
  columns: [{ key: 'name', title: '医生姓名' }, { key: 'department', title: '科室' }],
  formFields: [
    { key: 'name', label: '姓名', required: true, placeholder: '请输入姓名', defaultValue: '' },
    { key: 'department', label: '科室', required: true, placeholder: '请输入科室', defaultValue: '' }
  ],
  statusField: 'enabled',
  createPayload: (form) => ({ name: form.name, department: form.department }),
  updatePayload: (form) => ({ name: form.name, department: form.department })
};

export const visitPageConfig: EntityPageConfig = {
  title: 'visits',
  route: API_ROUTES.visits,
  permissionPrefix: 'visits',
  columns: [{ key: 'patientName', title: '就诊人' }, { key: 'doctorName', title: '接诊医生' }],
  formFields: [
    { key: 'patientName', label: '就诊人姓名', required: true, placeholder: '请输入就诊人姓名', defaultValue: '' },
    { key: 'doctorName', label: '接诊医生', required: true, placeholder: '请输入接诊医生', defaultValue: '' }
  ],
  statusField: 'status',
  createPayload: (form) => ({ patientName: form.patientName, doctorName: form.doctorName }),
  updatePayload: (form) => ({ patientName: form.patientName, doctorName: form.doctorName })
};

export const familyDoctorsPageConfig: EntityPageConfig = {
  title: 'family-doctors',
  route: API_ROUTES.familyDoctors,
  permissionPrefix: 'family-doctors',
  columns: [{ key: 'residentId', title: '居民ID' }, { key: 'doctorId', title: '医生ID' }, { key: 'servicePackage', title: '服务包' }],
  formFields: [
    { key: 'residentId', label: '居民ID', required: true, placeholder: '请输入居民用户ID', defaultValue: '' },
    { key: 'doctorId', label: '医生ID', required: true, placeholder: '请输入医生ID', defaultValue: '' },
    { key: 'servicePackage', label: '服务包', placeholder: '请输入服务包名称', defaultValue: '' },
    { key: 'validTo', label: '有效期至', placeholder: 'YYYY-MM-DD', defaultValue: '' }
  ],
  statusField: 'enabled',
  createPayload: (form) => ({
    residentId: Number(form.residentId),
    doctorId: Number(form.doctorId),
    ...(form.servicePackage ? { servicePackage: form.servicePackage } : {}),
    ...(form.validTo ? { validTo: form.validTo } : {})
  }),
  updatePayload: (form) => ({
    residentId: Number(form.residentId),
    doctorId: Number(form.doctorId),
    ...(form.servicePackage ? { servicePackage: form.servicePackage } : {}),
    ...(form.validTo ? { validTo: form.validTo } : {})
  })
};

export const configsPageConfig: EntityPageConfig = {
  title: 'configs',
  route: API_ROUTES.configs,
  permissionPrefix: 'configs',
  columns: [{ key: 'configKey', title: '配置键' }, { key: 'configValue', title: '配置值' }],
  formFields: [
    { key: 'configKey', label: '配置键', required: true, placeholder: '请输入配置键', defaultValue: '' },
    { key: 'configValue', label: '配置值', required: true, placeholder: '请输入配置值', defaultValue: '' }
  ],
  statusField: 'enabled',
  createPayload: (form) => ({ configKey: form.configKey, configValue: form.configValue }),
  updatePayload: (form) => ({ configKey: form.configKey, configValue: form.configValue })
};

export const medicalRecordsPageConfig: EntityPageConfig = {
  title: 'medical-records',
  route: API_ROUTES.medicalRecords,
  permissionPrefix: 'medical-records',
  columns: [{ key: 'bingliName', title: '病历名称' }, { key: 'bingliUuidNumber', title: '编号' }],
  formFields: [
    { key: 'yishengId', label: '医生ID', required: true, placeholder: '请输入医生ID', defaultValue: '' },
    { key: 'yonghuId', label: '患者ID', required: true, placeholder: '请输入患者ID', defaultValue: '' },
    { key: 'bingliUuidNumber', label: '病历编号', required: true, placeholder: '请输入病历编号', defaultValue: '' },
    { key: 'bingliName', label: '病历名称', required: true, placeholder: '请输入病历名称', defaultValue: '' },
    { key: 'bingliBingqing', label: '病情描述', required: true, placeholder: '请输入病情描述', defaultValue: '' },
    { key: 'jianchaxiangmu', label: '检查项目', required: true, placeholder: '请输入检查项目', defaultValue: '' },
    { key: 'jianchajieguo', label: '检查结果', required: true, placeholder: '请输入检查结果', defaultValue: '' }
  ],
  statusField: 'enabled',
  createPayload: (form) => ({
    yishengId: Number(form.yishengId),
    yonghuId: Number(form.yonghuId),
    bingliUuidNumber: form.bingliUuidNumber,
    bingliName: form.bingliName,
    bingliBingqing: form.bingliBingqing,
    jianchaxiangmu: form.jianchaxiangmu,
    jianchajieguo: form.jianchajieguo
  }),
  updatePayload: (form) => ({
    yishengId: Number(form.yishengId),
    yonghuId: Number(form.yonghuId),
    bingliUuidNumber: form.bingliUuidNumber,
    bingliName: form.bingliName,
    bingliBingqing: form.bingliBingqing,
    jianchaxiangmu: form.jianchaxiangmu,
    jianchajieguo: form.jianchajieguo
  })
};

export const healthRecordsPageConfig: EntityPageConfig = {
  title: 'health-records',
  route: API_ROUTES.healthRecords,
  permissionPrefix: 'health-records',
  columns: [{ key: 'jiuankangdanganName', title: '档案名称' }, { key: 'yonghuId', title: '患者ID' }],
  formFields: [
    { key: 'yonghuId', label: '患者ID', required: true, placeholder: '请输入患者ID', defaultValue: '' },
    { key: 'jiuankangdanganName', label: '档案名称', required: true, placeholder: '请输入档案名称', defaultValue: '' },
    { key: 'jiuankangdanganQita', label: '其他信息', required: true, placeholder: '请输入其他信息', defaultValue: '' },
    { key: 'jiuankangdanganTypes', label: '档案类型', required: true, placeholder: '请输入类型(数字)', defaultValue: '1' },
    { key: 'insertTime', label: '记录时间', required: true, placeholder: 'YYYY-MM-DDTHH:mm:ss', defaultValue: '' },
    { key: 'jiuankangdanganContent', label: '档案内容', required: true, placeholder: '请输入档案内容', defaultValue: '' }
  ],
  statusField: 'enabled',
  createPayload: (form) => ({
    yonghuId: Number(form.yonghuId),
    jiuankangdanganName: form.jiuankangdanganName,
    jiuankangdanganQita: form.jiuankangdanganQita,
    jiuankangdanganTypes: Number(form.jiuankangdanganTypes),
    insertTime: form.insertTime,
    jiuankangdanganContent: form.jiuankangdanganContent
  }),
  updatePayload: (form) => ({
    yonghuId: Number(form.yonghuId),
    jiuankangdanganName: form.jiuankangdanganName,
    jiuankangdanganQita: form.jiuankangdanganQita,
    jiuankangdanganTypes: Number(form.jiuankangdanganTypes),
    insertTime: form.insertTime,
    jiuankangdanganContent: form.jiuankangdanganContent
  })
};
