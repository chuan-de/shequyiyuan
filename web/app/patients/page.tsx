'use client';
import { EntityManagementPage } from '@/components/business/entity-management-page';
import { patientsPageConfig } from '@/components/business/forms/entity-form-configs';

export default function PatientsPage() {
  return <EntityManagementPage config={patientsPageConfig} />;
}
