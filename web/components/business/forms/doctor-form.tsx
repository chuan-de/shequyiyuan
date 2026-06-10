'use client';
import { useEffect, useMemo, useState } from 'react';
import { EntityManagementPage, EntityPageConfig } from '@/components/business/entity-management-page';
import { doctorPageConfig } from './entity-form-configs';
import { API_ROUTES, EntityRecord } from '@/lib/api-contract';
import { listEntities } from '@/lib/api';
import { readToken } from '@/lib/token-storage';

export { doctorPageConfig };

export function DoctorManagementPage() {
  const [departments, setDepartments] = useState<EntityRecord[]>([]);

  useEffect(() => {
    const t = readToken();
    if (!t) return;
    listEntities(t.accessToken, API_ROUTES.departments, { page: 1, size: 200 })
      .then(res => setDepartments(res.records))
      .catch(() => setDepartments([]));
  }, []);

  const config: EntityPageConfig = useMemo(() => {
    const deptOptions = departments.map(d => ({
      value: String(d.id),
      label: String(d.deptName ?? d.id),
    }));

    return {
      ...doctorPageConfig,
      formFields: doctorPageConfig.formFields.map(f =>
        f.key === 'departmentId'
          ? { ...f, type: 'select' as const, options: deptOptions, placeholder: '请选择所属科室' }
          : f
      ),
      searchFields: [
        ...(doctorPageConfig.searchFields ?? []),
        { key: 'departmentId', label: '所属科室', type: 'select' as const, options: deptOptions },
      ],
    };
  }, [departments]);

  return <EntityManagementPage config={config} />;
}
