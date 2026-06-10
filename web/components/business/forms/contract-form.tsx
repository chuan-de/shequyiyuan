'use client';
import { useEffect, useMemo, useState } from 'react';
import { EntityManagementPage, EntityPageConfig } from '@/components/business/entity-management-page';
import { contractsPageConfig } from './entity-form-configs';
import { API_ROUTES, EntityRecord } from '@/lib/api-contract';
import { listEntities } from '@/lib/api';
import { readToken } from '@/lib/token-storage';

export { contractsPageConfig };

export function ContractManagementPage() {
  const [patients, setPatients] = useState<EntityRecord[]>([]);
  const [doctors, setDoctors] = useState<EntityRecord[]>([]);

  useEffect(() => {
    const t = readToken();
    if (!t) return;
    listEntities(t.accessToken, API_ROUTES.patients, { page: 1, size: 500 })
      .then(res => setPatients(res.records))
      .catch(() => setPatients([]));
    listEntities(t.accessToken, API_ROUTES.familyDoctors, { page: 1, size: 200 })
      .then(res => setDoctors(res.records))
      .catch(() => setDoctors([]));
  }, []);

  const config: EntityPageConfig = useMemo(() => {
    const patientOptions = patients.map(p => ({
      value: String(p.id),
      label: String(p.fullName ?? p.username ?? p.id),
    }));
    const doctorOptions = doctors.map(d => ({
      value: String(d.id),
      label: String(d.fullName ?? d.username ?? d.id),
    }));

    return {
      ...contractsPageConfig,
      columns: contractsPageConfig.columns.map(c =>
        c.key === 'patientName'
          ? {
              ...c,
              // 患者姓名可点击 → 患者 360° 视图。
              render: (row) => row.patientId
                ? <a href={`/patients/${row.patientId}`} className="text-blue-600 hover:underline">{String(row.patientName ?? '—')}</a>
                : String(row.patientName ?? '—'),
            }
          : c
      ),
      formFields: contractsPageConfig.formFields.map(f => {
        if (f.key === 'patientId') return { ...f, type: 'select' as const, options: patientOptions, placeholder: '请选择患者' };
        if (f.key === 'familyDoctorId') return { ...f, type: 'select' as const, options: doctorOptions, placeholder: '请选择家庭医生' };
        return f;
      }),
    };
  }, [patients, doctors]);

  return <EntityManagementPage config={config} />;
}
