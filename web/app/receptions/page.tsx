'use client';
import { EntityManagementPage } from '@/components/business/entity-management-page';
import { receptionsPageConfig } from '@/components/business/forms/entity-form-configs';

export default function ReceptionsPage() {
  return <EntityManagementPage config={receptionsPageConfig} />;
}
