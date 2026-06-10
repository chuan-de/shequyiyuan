'use client';
import { useEffect, useMemo, useState } from 'react';
import { EntityManagementPage, EntityPageConfig } from '@/components/business/entity-management-page';
import { healthRecordsPageConfig } from './entity-form-configs';
import { API_ROUTES, EntityRecord } from '@/lib/api-contract';
import { listEntities } from '@/lib/api';
import { readToken } from '@/lib/token-storage';

export { healthRecordsPageConfig };

export function HealthRecordManagementPage() {
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
    const patientById = new Map(patients.map(p => [Number(p.id), p]));

    return {
      ...healthRecordsPageConfig,
      columns: healthRecordsPageConfig.columns.map(c => {
        // 历史数据冗余列可能为空，这里用患者表数据兜底显示。
        if (c.key === 'patientName') return {
          ...c,
          render: (row) => String(row.patientName ?? patientById.get(Number(row.patientId))?.fullName ?? '—'),
        };
        if (c.key === 'patientPhotoUrl') return {
          ...c,
          render: (row) => {
            const url = patientById.get(Number(row.patientId))?.photoUrl;
            return url
              ? <img src={String(url)} alt="" className="h-10 w-10 rounded-full object-cover" onError={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }} />
              : <span className="text-slate-300">—</span>;
          },
        };
        if (c.key === 'patientPhone') return {
          ...c,
          render: (row) => String(row.patientPhone ?? patientById.get(Number(row.patientId))?.phone ?? '—'),
        };
        if (c.key === 'patientIdNumber') return {
          ...c,
          render: (row) => String(row.patientIdNumber ?? patientById.get(Number(row.patientId))?.idNumber ?? '—'),
        };
        return c;
      }),
      formFields: healthRecordsPageConfig.formFields.map(f =>
        f.key === 'patientId'
          ? { ...f, label: '患者', type: 'select' as const, options: patientOptions, placeholder: '请选择患者' }
          : f
      ),
    };
  }, [patients]);

  return <EntityManagementPage config={config} />;
}
