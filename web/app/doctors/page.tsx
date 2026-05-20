'use client';
import { EntityManagementPage } from '@/components/business/entity-management-page';
import { doctorPageConfig } from '@/components/business/forms/doctor-form';

export default function Page() {
  return <EntityManagementPage config={doctorPageConfig} />;
}
