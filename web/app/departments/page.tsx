'use client';
import { EntityManagementPage } from '@/components/business/entity-management-page';
import { departmentsPageConfig } from '@/components/business/forms/entity-form-configs';

export default function DepartmentsPage() {
  return <EntityManagementPage config={departmentsPageConfig} />;
}
