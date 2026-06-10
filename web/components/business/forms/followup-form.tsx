'use client';
import { useEffect, useMemo, useState } from 'react';
import { EntityManagementPage, EntityPageConfig } from '@/components/business/entity-management-page';
import { followupsPageConfig } from './entity-form-configs';
import { API_ROUTES, EntityRecord } from '@/lib/api-contract';
import { listEntities } from '@/lib/api';
import { readToken } from '@/lib/token-storage';

export { followupsPageConfig };

export function FollowupManagementPage() {
  const [patients, setPatients] = useState<EntityRecord[]>([]);

  useEffect(() => {
    const t = readToken();
    if (!t) return;
    listEntities(t.accessToken, API_ROUTES.patients, { page: 1, size: 500 })
      .then(res => setPatients(res.records))
      .catch(() => setPatients([]));
  }, []);

  const config: EntityPageConfig = useMemo(() => {
    const patientOptions = patients.map(p => ({
      value: String(p.id),
      label: String(p.fullName ?? p.username ?? p.id),
    }));

    return {
      ...followupsPageConfig,
      columns: followupsPageConfig.columns.map(c =>
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
      formFields: followupsPageConfig.formFields.map(f =>
        f.key === 'patientId'
          ? { ...f, type: 'select' as const, options: patientOptions, placeholder: '请选择患者' }
          : f
      ),
    };
  }, [patients]);

  return <EntityManagementPage config={config} />;
}
