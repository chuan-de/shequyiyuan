import { EntityPageConfig } from '@/components/business/entity-management-page';
import { API_ROUTES } from '@/lib/api-contract';

const baseFields = [
  { key: 'name', label: '名称', required: true, placeholder: '请输入名称', defaultValue: '' }
];

export const doctorPageConfig: EntityPageConfig = {
  title: 'doctors',
  route: API_ROUTES.doctors,
  permissionPrefix: 'doctors',
  columns: [{ key: 'name', title: '医生姓名' }, { key: 'department', title: '科室' }, { key: 'phone', title: '电话' }],
  formFields: [...baseFields, { key: 'department', label: '科室', placeholder: '请输入科室', defaultValue: '' }, { key: 'phone', label: '电话', placeholder: '请输入联系电话', defaultValue: '' }],
  statusField: 'enabled',
  createPayload: (form) => ({ name: form.name, department: form.department, phone: form.phone }),
  updatePayload: (form) => ({ name: form.name, department: form.department, phone: form.phone })
};

export const visitPageConfig: EntityPageConfig = {
  title: 'visits', route: API_ROUTES.visits, permissionPrefix: 'visits',
  columns: [{ key: 'name', title: '就诊人' }, { key: 'visitDate', title: '就诊日期' }, { key: 'hospital', title: '医院' }],
  formFields: [...baseFields, { key: 'visitDate', label: '就诊日期', placeholder: 'YYYY-MM-DD', defaultValue: '' }, { key: 'hospital', label: '医院', placeholder: '请输入医院', defaultValue: '' }],
  statusField: 'status',
  createPayload: (form) => ({ patientName: form.name, visitDate: form.visitDate, hospital: form.hospital }),
  updatePayload: (form) => ({ patientName: form.name, visitDate: form.visitDate, hospital: form.hospital })
};

function simpleConfig(title: string, route: string, permissionPrefix: string): EntityPageConfig {
  return {
    title, route, permissionPrefix,
    columns: [{ key: 'name', title: '名称' }],
    formFields: baseFields,
    statusField: 'enabled',
    createPayload: (form) => ({ name: form.name }),
    updatePayload: (form) => ({ name: form.name })
  };
}

export const medicationsPageConfig = simpleConfig('medications', API_ROUTES.medications, 'medications');
export const familyDoctorsPageConfig = simpleConfig('family-doctors', API_ROUTES.familyDoctors, 'family-doctors');
export const medicalRecordsPageConfig = simpleConfig('medical-records', API_ROUTES.medicalRecords, 'medical-records');
export const healthRecordsPageConfig = simpleConfig('health-records', API_ROUTES.healthRecords, 'health-records');
export const configsPageConfig = simpleConfig('configs', API_ROUTES.configs, 'configs');
