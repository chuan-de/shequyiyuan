'use client';
import { EntityManagementPage } from '@/components/business/entity-management-page';
import { medicalRecordsPageConfig } from '@/components/business/forms/entity-form-configs';

export default function Page() {
  return <EntityManagementPage config={medicalRecordsPageConfig} />;
}
