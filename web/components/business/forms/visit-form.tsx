'use client';
import { useEffect, useMemo, useState } from 'react';
import { EntityManagementPage, EntityPageConfig } from '@/components/business/entity-management-page';
import { visitPageConfig } from './entity-form-configs';
import { API_ROUTES, EntityRecord } from '@/lib/api-contract';
import { listEntities } from '@/lib/api';
import { readToken } from '@/lib/token-storage';

export { visitPageConfig };

export function VisitManagementPage() {
  const [departments, setDepartments] = useState<EntityRecord[]>([]);
  const [patients, setPatients] = useState<EntityRecord[]>([]);

  useEffect(() => {
    const t = readToken();
    if (!t) return;
    listEntities(t.accessToken, API_ROUTES.departments, { page: 1, size: 200 })
      .then(res => setDepartments(res.records))
      .catch(() => setDepartments([]));
    listEntities(t.accessToken, API_ROUTES.patients, { page: 1, size: 500 })
      .then(res => setPatients(res.records))
      .catch(() => setPatients([]));
  }, []);

  const config: EntityPageConfig = useMemo(() => {
    const deptOptions = departments.map(d => ({
      value: String(d.id),
      label: String(d.deptName ?? d.id),
    }));
    const deptIdToName = new Map(departments.map(d => [Number(d.id), String(d.deptName ?? d.id)]));

    const patientOptions = patients.map(p => ({
      value: String(p.id),
      label: String(p.fullName ?? p.username ?? p.id),
    }));
    const patientIdToName = new Map(patients.map(p => [Number(p.id), String(p.fullName ?? p.username ?? p.id)]));

    return {
      ...visitPageConfig,
      columns: visitPageConfig.columns.map(c => {
        if (c.key === 'keshiTypes') return {
          ...c,
          render: (row) => deptIdToName.get(Number(row.keshiTypes)) ?? (row.keshiTypes != null ? String(row.keshiTypes) : '—'),
        };
        if (c.key === 'patientFullName') return {
          ...c,
          render: (row) => row.patientFullName ? String(row.patientFullName) : (patientIdToName.get(Number(row.patientId)) ?? '—'),
        };
        return c;
      }),
      formFields: visitPageConfig.formFields.map(f => {
        if (f.key === 'keshiTypes') return { ...f, type: 'select' as const, options: deptOptions, placeholder: '请选择科室' };
        if (f.key === 'patientId') return { ...f, label: '患者', type: 'select' as const, options: patientOptions, placeholder: '请选择患者' };
        return f;
      }),
      searchFields: visitPageConfig.searchFields?.map(sf =>
        sf.key === 'keshiTypes' ? { ...sf, type: 'select' as const, options: deptOptions } : sf
      ),
    };
  }, [departments, patients]);

  return <EntityManagementPage config={config} />;
}
