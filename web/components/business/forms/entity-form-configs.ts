import { ActionHelpers, EntityPageConfig } from '@/components/business/entity-management-page';
import { API_ROUTES } from '@/lib/api-contract';
import { EntityRecord } from '@/lib/api-contract';

function passwordValidator(v: string): string | null {
  return v.length < 6 ? '密码至少 6 位' : null;
}

function positiveIntValidator(v: string): string | null {
  const n = Number(v);
  if (!v || isNaN(n) || !Number.isInteger(n) || n <= 0) return '请输入正整数';
  return null;
}

async function resetPasswordAction(
  row: EntityRecord,
  helpers: ActionHelpers,
  apiPath: string,
): Promise<void> {
  const pwd = await helpers.prompt({
    title: '重置密码', label: '新密码', type: 'password',
    placeholder: '至少 6 位', confirmText: '确认重置', validate: passwordValidator,
  });
  if (pwd === null) return;
  try {
    const res = await fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL}${apiPath}/${row.id}/reset-password`, {
      method: 'PATCH',
      headers: { Authorization: `Bearer ${helpers.token}`, 'Content-Type': 'application/json' },
      body: JSON.stringify({ newPassword: pwd }),
    });
    if (!res.ok) throw new Error('重置失败');
    helpers.showToast('密码已重置');
  } catch (e) { helpers.showToast((e as Error).message, 'error'); }
}

async function adjustInventoryAction(
  row: EntityRecord,
  helpers: ActionHelpers,
  direction: 'add' | 'reduce',
): Promise<void> {
  const qty = await helpers.prompt({
    title: direction === 'add' ? '增加库存' : '减少库存',
    label: '数量',
    type: 'number',
    placeholder: '请输入正整数',
    confirmText: '确认',
    validate: positiveIntValidator,
  });
  if (qty === null) return;
  const delta = direction === 'add' ? Number(qty) : -Number(qty);
  try {
    const res = await fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL}/api/v1/medications/${row.id}/inventory`, {
      method: 'PATCH',
      headers: { Authorization: `Bearer ${helpers.token}`, 'Content-Type': 'application/json' },
      body: JSON.stringify({ delta }),
    });
    if (!res.ok) throw new Error('调整失败');
    helpers.showToast(direction === 'add' ? '库存已增加' : '库存已减少');
    helpers.reload();
  } catch (e) { helpers.showToast((e as Error).message, 'error'); }
}

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
    { key: 'password', label: '密码', required: true, type: 'password', placeholder: '至少6位', defaultValue: '' },
    { key: 'fullName', label: '姓名', required: true, placeholder: '请输入姓名', defaultValue: '' },
    { key: 'phone', label: '手机号', placeholder: '请输入手机号', defaultValue: '' },
    { key: 'idNumber', label: '身份证号', placeholder: '请输入身份证号', defaultValue: '' },
    { key: 'email', label: '邮箱', placeholder: '请输入邮箱', defaultValue: '' }
  ],
  statusField: 'enabled',
  rowActions: [
    {
      key: 'resetPwd',
      label: '重置密码',
      permission: 'patients:reset-password',
      onClick: (row, helpers) => resetPasswordAction(row, helpers, '/api/v1/patients'),
    },
    {
      // Entry point into the AI-augmented patient detail page (Phase 2.10).
      // Permission gating: only users with ai:patient-rag see the action; the
      // detail page double-checks before rendering the AI panel.
      key: 'aiAsk',
      label: 'AI 问询',
      permission: 'ai:patient-rag',
      onClick: (row) => {
        if (typeof window !== 'undefined') {
          window.location.assign(`/patients/${row.id}`);
        }
      },
    },
  ],
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
    { key: 'password', label: '密码', required: true, type: 'password', placeholder: '至少6位', defaultValue: '' },
    { key: 'fullName', label: '姓名', required: true, placeholder: '请输入姓名', defaultValue: '' },
    { key: 'uuidNumber', label: '工号', placeholder: '请输入工号', defaultValue: '' },
    { key: 'phone', label: '手机号', placeholder: '请输入手机号', defaultValue: '' },
    { key: 'email', label: '邮箱', placeholder: '请输入邮箱', defaultValue: '' }
  ],
  statusField: 'enabled',
  rowActions: [
    {
      key: 'resetPwd',
      label: '重置密码',
      permission: 'receptions:reset-password',
      onClick: (row, helpers) => resetPasswordAction(row, helpers, '/api/v1/receptions'),
    },
  ],
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
  title: '药品管理',
  route: API_ROUTES.medications,
  permissionPrefix: 'medications',
  columns: [
    { key: 'code', title: '药品编号' },
    { key: 'name', title: '药品名称' },
    { key: 'price', title: '药品价格', type: 'currency' },
    { key: 'stock', title: '药品库存' },
    { key: 'mainEffect', title: '主要药效' },
    { key: 'sideEffect', title: '副作用' },
  ],
  formFields: [
    { key: 'code', label: '药品编号', required: true, defaultValue: '' },
    { key: 'name', label: '药品名称', required: true, defaultValue: '' },
    { key: 'price', label: '药品价格', type: 'number', defaultValue: '0' },
    { key: 'stock', label: '药品库存', type: 'number', defaultValue: '0' },
    { key: 'mainEffect', label: '主要药效', type: 'textarea', defaultValue: '' },
    { key: 'sideEffect', label: '副作用', type: 'textarea', defaultValue: '' },
    { key: 'detail', label: '药品详情', type: 'textarea', defaultValue: '' },
  ],
  rowActions: [
    {
      key: 'addStock',
      label: '增加库存',
      permission: 'medications:inventory',
      variant: 'primary',
      onClick: (row, helpers) => adjustInventoryAction(row, helpers, 'add'),
    },
    {
      key: 'reduceStock',
      label: '减少库存',
      permission: 'medications:inventory',
      variant: 'danger',
      onClick: (row, helpers) => adjustInventoryAction(row, helpers, 'reduce'),
    },
  ],
  labelMap: { code: '药品编号', name: '药品名称', price: '药品价格', stock: '药品库存', mainEffect: '主要药效', sideEffect: '副作用', detail: '药品详情', enabled: '状态', createdAt: '创建时间' },
  createPayload: (form) => ({ code: form.code, name: form.name, price: form.price ? Number(form.price) : 0, stock: form.stock ? Number(form.stock) : 0, mainEffect: form.mainEffect || undefined, sideEffect: form.sideEffect || undefined, detail: form.detail || undefined }),
  updatePayload: (form) => ({ code: form.code, name: form.name, price: form.price ? Number(form.price) : 0, mainEffect: form.mainEffect || undefined, sideEffect: form.sideEffect || undefined, detail: form.detail || undefined }),
};

export const doctorPageConfig: EntityPageConfig = {
  title: '医生管理',
  route: API_ROUTES.doctors,
  permissionPrefix: 'doctors',
  columns: [
    { key: 'uuidNumber', title: '工号' },
    { key: 'username', title: '账户' },
    { key: 'fullName', title: '医生姓名' },
    { key: 'photoUrl', title: '头像', type: 'photo' },
    { key: 'sexTypes', title: '性别', render: (row) => row.sexTypes === 1 ? '男' : row.sexTypes === 2 ? '女' : '—' },
    { key: 'phone', title: '联系方式' },
    { key: 'idNumber', title: '医生身份证号' },
    { key: 'email', title: '邮箱' },
  ],
  formFields: [
    { key: 'username', label: '账户', required: true, defaultValue: '' },
    { key: 'password', label: '密码', required: true, type: 'password', placeholder: '至少6位', defaultValue: '' },
    { key: 'uuidNumber', label: '工号', defaultValue: '' },
    { key: 'fullName', label: '医生姓名', required: true, defaultValue: '' },
    { key: 'photoUrl', label: '头像', type: 'photo', defaultValue: '' },
    { key: 'sexTypes', label: '性别', type: 'select', options: [{ value: '1', label: '男' }, { value: '2', label: '女' }], defaultValue: '' },
    { key: 'phone', label: '联系方式', defaultValue: '' },
    { key: 'idNumber', label: '医生身份证号', defaultValue: '' },
    { key: 'email', label: '邮箱', defaultValue: '' },
  ],
  statusField: 'enabled',
  searchFields: [
    { key: 'uuidNumber', label: '工号' },
    { key: 'fullName', label: '医生姓名' },
    { key: 'sexTypes', label: '性别', type: 'select', options: [{ value: '1', label: '男' }, { value: '2', label: '女' }] },
  ],
  rowActions: [
    {
      key: 'resetPwd',
      label: '重置密码',
      permission: 'doctors:reset-password',
      onClick: (row, helpers) => resetPasswordAction(row, helpers, '/api/v1/doctors'),
    },
  ],
  labelMap: { uuidNumber: '工号', username: '账户', fullName: '医生姓名', photoUrl: '头像', sexTypes: '性别', phone: '联系方式', idNumber: '医生身份证号', email: '邮箱', enabled: '状态', userId: '用户ID', createdAt: '创建时间' },
  createPayload: (form) => ({ username: form.username, password: form.password, uuidNumber: form.uuidNumber || undefined, fullName: form.fullName, photoUrl: form.photoUrl || undefined, sexTypes: form.sexTypes ? Number(form.sexTypes) : undefined, phone: form.phone || undefined, idNumber: form.idNumber || undefined, email: form.email || undefined }),
  updatePayload: (form) => ({ uuidNumber: form.uuidNumber || undefined, fullName: form.fullName, photoUrl: form.photoUrl || undefined, sexTypes: form.sexTypes ? Number(form.sexTypes) : undefined, phone: form.phone || undefined, idNumber: form.idNumber || undefined, email: form.email || undefined }),
};

export const visitPageConfig: EntityPageConfig = {
  title: '就诊记录',
  route: API_ROUTES.visits,
  permissionPrefix: 'visits',
  columns: [
    { key: 'patientFullName', title: '用户姓名' },
    { key: 'patientPhotoUrl', title: '头像', type: 'photo' },
    { key: 'patientPhone', title: '联系方式' },
    { key: 'patientIdNumber', title: '用户身份证号' },
    { key: 'visitNumber', title: '就诊号' },
    { key: 'fee', title: '就诊费用', type: 'currency' },
    { key: 'keshiTypes', title: '科室' },
    { key: 'visitDate', title: '日期', render: (row) => row.visitDate ? String(row.visitDate).substring(0, 10) : '—' },
    { key: 'registrationNotes', title: '挂号备注' },
  ],
  formFields: [
    { key: 'patientId', label: '患者ID', required: true, type: 'number', defaultValue: '' },
    { key: 'fee', label: '就诊费用', type: 'number', defaultValue: '' },
    { key: 'keshiTypes', label: '科室', type: 'number', defaultValue: '' },
    { key: 'visitDate', label: '日期', type: 'datetime', defaultValue: '' },
    { key: 'registrationNotes', label: '挂号备注', type: 'textarea', defaultValue: '' },
    { key: 'visitContent', label: '挂号详情', type: 'textarea', defaultValue: '' },
  ],
  statusField: undefined,
  searchFields: [
    { key: 'visitNumber', label: '就诊号' },
    { key: 'keshiTypes', label: '科室' },
    { key: 'patientName', label: '用户姓名' },
  ],
  labelMap: { patientFullName: '用户姓名', patientPhotoUrl: '头像', patientPhone: '联系方式', patientIdNumber: '用户身份证号', visitNumber: '就诊号', fee: '就诊费用', keshiTypes: '科室', visitDate: '日期', registrationNotes: '挂号备注', visitContent: '挂号详情', createdAt: '创建时间' },
  createPayload: (form) => ({ patientId: Number(form.patientId), fee: form.fee ? Number(form.fee) : undefined, keshiTypes: form.keshiTypes ? Number(form.keshiTypes) : undefined, visitDate: form.visitDate ? new Date(form.visitDate).toISOString() : undefined, registrationNotes: form.registrationNotes || undefined, visitContent: form.visitContent || undefined }),
  updatePayload: (form) => ({ fee: form.fee ? Number(form.fee) : undefined, keshiTypes: form.keshiTypes ? Number(form.keshiTypes) : undefined, visitDate: form.visitDate ? new Date(form.visitDate).toISOString() : undefined, registrationNotes: form.registrationNotes || undefined, visitContent: form.visitContent || undefined }),
};

export const familyDoctorsPageConfig: EntityPageConfig = {
  title: '家庭医生',
  route: API_ROUTES.familyDoctors,
  permissionPrefix: 'family-doctors',
  columns: [
    { key: 'username', title: '账户' },
    { key: 'fullName', title: '家庭医生负责人姓名' },
    { key: 'photoUrl', title: '头像', type: 'photo' },
    { key: 'sexTypes', title: '性别', render: (row) => row.sexTypes === 1 ? '男' : row.sexTypes === 2 ? '女' : '—' },
    { key: 'phone', title: '联系方式' },
    { key: 'email', title: '邮箱' },
  ],
  formFields: [
    { key: 'username', label: '账户', required: true, defaultValue: '' },
    { key: 'password', label: '密码', required: true, type: 'password', placeholder: '至少6位', defaultValue: '' },
    { key: 'fullName', label: '家庭医生负责人姓名', required: true, defaultValue: '' },
    { key: 'photoUrl', label: '头像', type: 'photo', defaultValue: '' },
    { key: 'sexTypes', label: '性别', type: 'select', options: [{ value: '1', label: '男' }, { value: '2', label: '女' }], defaultValue: '' },
    { key: 'phone', label: '联系方式', defaultValue: '' },
    { key: 'email', label: '邮箱', defaultValue: '' },
  ],
  statusField: 'enabled',
  searchFields: [
    { key: 'fullName', label: '家庭医生负责人姓名' },
    { key: 'sexTypes', label: '性别', type: 'select', options: [{ value: '1', label: '男' }, { value: '2', label: '女' }] },
  ],
  rowActions: [
    {
      key: 'resetPwd',
      label: '重置密码',
      permission: 'family-doctors:reset-password',
      onClick: (row, helpers) => resetPasswordAction(row, helpers, '/api/v1/family-doctors'),
    },
  ],
  labelMap: { username: '账户', fullName: '家庭医生负责人姓名', photoUrl: '头像', sexTypes: '性别', phone: '联系方式', email: '邮箱', enabled: '状态', createdAt: '创建时间' },
  createPayload: (form) => ({ username: form.username, password: form.password, fullName: form.fullName, photoUrl: form.photoUrl || undefined, sexTypes: form.sexTypes ? Number(form.sexTypes) : undefined, phone: form.phone || undefined, email: form.email || undefined }),
  updatePayload: (form) => ({ fullName: form.fullName, photoUrl: form.photoUrl || undefined, sexTypes: form.sexTypes ? Number(form.sexTypes) : undefined, phone: form.phone || undefined, email: form.email || undefined }),
};

export const configsPageConfig: EntityPageConfig = {
  title: '系统配置',
  route: API_ROUTES.configs,
  permissionPrefix: 'configs',
  columns: [{ key: 'configKey', title: '配置键' }, { key: 'configValue', title: '配置值' }],
  formFields: [
    { key: 'configKey', label: '配置键', required: true, placeholder: '请输入配置键', defaultValue: '' },
    { key: 'configValue', label: '配置值', required: true, placeholder: '请输入配置值', defaultValue: '' }
  ],
  statusField: 'enabled',
  labelMap: { configKey: '配置键', configValue: '配置值', createdAt: '创建时间', updatedAt: '更新时间' },
  createPayload: (form) => ({ configKey: form.configKey, configValue: form.configValue }),
  updatePayload: (form) => ({ configKey: form.configKey, configValue: form.configValue })
};

export const medicalRecordsPageConfig: EntityPageConfig = {
  title: '病历管理',
  route: API_ROUTES.medicalRecords,
  permissionPrefix: 'medical-records',
  columns: [
    { key: 'caseNumber', title: '病历编号' },
    { key: 'caseName', title: '病历名称' },
    { key: 'doctorName', title: '医生姓名' },
    { key: 'patientName', title: '患者姓名' },
    { key: 'recordDate', title: '日期', render: (row) => row.recordDate ? String(row.recordDate).substring(0, 10) : '—' },
    { key: 'conditionDesc', title: '病情描述' },
  ],
  formFields: [
    { key: 'doctorId', label: '医生', required: true, type: 'select', options: [], placeholder: '请选择医生', defaultValue: '' },
    { key: 'patientId', label: '患者', required: true, type: 'select', options: [], placeholder: '请选择患者', defaultValue: '' },
    { key: 'caseName', label: '病历名称', required: true, placeholder: '请输入病历名称', defaultValue: '' },
    { key: 'recordDate', label: '日期', type: 'datetime', defaultValue: '' },
    { key: 'conditionDesc', label: '病情描述', type: 'textarea', placeholder: '请输入病情描述', defaultValue: '' },
    { key: 'examItems', label: '检查项目', type: 'textarea', placeholder: '请输入检查项目', defaultValue: '' },
    { key: 'examResults', label: '检查结果', type: 'textarea', placeholder: '请输入检查结果', defaultValue: '' },
    { key: 'prescriptionItems', label: '药单', type: 'custom', defaultValue: '[]' },
    { key: 'attachments', label: '病历附件', type: 'custom', defaultValue: '[]' },
  ],
  labelMap: {
    caseNumber: '病历编号', caseName: '病历名称', doctorName: '医生姓名', doctorPhone: '医生联系方式',
    patientName: '患者姓名', patientPhone: '患者联系方式', patientIdNumber: '患者身份证号',
    recordDate: '日期', conditionDesc: '病情描述', examItems: '检查项目', examResults: '检查结果',
    prescriptionItems: '药单', attachments: '病历附件',
    status: '状态', createdAt: '创建时间', updatedAt: '更新时间'
  },
  createPayload: (form) => ({
    doctorId: Number(form.doctorId),
    patientId: Number(form.patientId),
    caseName: form.caseName,
    conditionDesc: form.conditionDesc || undefined,
    examItems: form.examItems || undefined,
    examResults: form.examResults || undefined,
    recordDate: form.recordDate ? new Date(form.recordDate).toISOString() : undefined,
    prescriptionItems: form.prescriptionItems ? JSON.parse(form.prescriptionItems) : [],
    attachments: form.attachments ? JSON.parse(form.attachments) : [],
  }),
  updatePayload: (form) => ({
    doctorId: Number(form.doctorId),
    patientId: Number(form.patientId),
    caseName: form.caseName,
    conditionDesc: form.conditionDesc || undefined,
    examItems: form.examItems || undefined,
    examResults: form.examResults || undefined,
    recordDate: form.recordDate ? new Date(form.recordDate).toISOString() : undefined,
    prescriptionItems: form.prescriptionItems ? JSON.parse(form.prescriptionItems) : [],
    attachments: form.attachments ? JSON.parse(form.attachments) : [],
  })
};

export const healthRecordsPageConfig: EntityPageConfig = {
  title: '健康档案',
  route: API_ROUTES.healthRecords,
  permissionPrefix: 'health-records',
  columns: [
    { key: 'patientFullName', title: '用户姓名' },
    { key: 'patientPhotoUrl', title: '头像', type: 'photo' },
    { key: 'patientPhone', title: '联系方式' },
    { key: 'patientIdNumber', title: '用户身份证号' },
    { key: 'title', title: '档案标题' },
    { key: 'otherMembers', title: '其他成员' },
    { key: 'unitTypes', title: '档案单位' },
    { key: 'recordedAt', title: '记录时间', render: (row) => row.recordedAt ? String(row.recordedAt).substring(0, 10) : '—' },
  ],
  formFields: [
    { key: 'patientId', label: '患者ID', required: true, type: 'number', defaultValue: '' },
    { key: 'title', label: '档案标题', required: true, defaultValue: '' },
    { key: 'otherMembers', label: '其他成员', defaultValue: '' },
    { key: 'unitTypes', label: '档案单位', type: 'number', defaultValue: '' },
    { key: 'recordedAt', label: '记录时间', type: 'datetime', defaultValue: '' },
    { key: 'content', label: '健康状况', type: 'textarea', defaultValue: '' },
  ],
  statusField: 'enabled',
  searchFields: [
    { key: 'unitTypes', label: '档案单位' },
    { key: 'patientName', label: '用户姓名' },
  ],
  labelMap: { patientFullName: '用户姓名', patientPhotoUrl: '头像', patientPhone: '联系方式', patientIdNumber: '用户身份证号', title: '档案标题', otherMembers: '其他成员', unitTypes: '档案单位', recordedAt: '记录时间', content: '健康状况', createdAt: '创建时间' },
  createPayload: (form) => ({ patientId: Number(form.patientId), title: form.title, otherMembers: form.otherMembers || undefined, unitTypes: form.unitTypes ? Number(form.unitTypes) : undefined, recordedAt: form.recordedAt ? new Date(form.recordedAt).toISOString() : undefined, content: form.content || undefined }),
  updatePayload: (form) => ({ title: form.title, otherMembers: form.otherMembers || undefined, unitTypes: form.unitTypes ? Number(form.unitTypes) : undefined, recordedAt: form.recordedAt ? new Date(form.recordedAt).toISOString() : undefined, content: form.content || undefined }),
};

export const departmentsPageConfig: EntityPageConfig = {
  title: '科室管理',
  route: API_ROUTES.departments,
  permissionPrefix: 'departments',
  columns: [
    { key: 'deptName', title: '科室名称' },
    { key: 'description', title: '科室简介' },
    { key: 'headPerson', title: '负责人' },
    { key: 'phone', title: '联系方式' },
    { key: 'deptTypes', title: '科室类型' },
  ],
  formFields: [
    { key: 'deptName', label: '科室名称', required: true, defaultValue: '' },
    { key: 'description', label: '科室简介', type: 'textarea', defaultValue: '' },
    { key: 'headPerson', label: '负责人', defaultValue: '' },
    { key: 'phone', label: '联系方式', defaultValue: '' },
    { key: 'deptTypes', label: '科室类型', type: 'number', defaultValue: '' },
  ],
  statusField: undefined,
  searchFields: [
    { key: 'deptName', label: '科室名称' },
  ],
  labelMap: { deptName: '科室名称', description: '科室简介', headPerson: '负责人', phone: '联系方式', deptTypes: '科室类型', createdAt: '创建时间', updatedAt: '更新时间' },
  createPayload: (form) => ({ deptName: form.deptName, description: form.description || undefined, headPerson: form.headPerson || undefined, phone: form.phone || undefined, deptTypes: form.deptTypes ? Number(form.deptTypes) : undefined }),
  updatePayload: (form) => ({ deptName: form.deptName, description: form.description || undefined, headPerson: form.headPerson || undefined, phone: form.phone || undefined, deptTypes: form.deptTypes ? Number(form.deptTypes) : undefined }),
};
